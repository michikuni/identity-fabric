#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# network.sh  –  Bring up / tear down the 2-org Hyperledger Fabric network
#
# Usage:
#   ./network.sh up                 # generate crypto, start containers
#   ./network.sh createChannel      # create mychannel and join all peers
#   ./network.sh deployCC           # package, install, approve, commit chaincode
#   ./network.sh down               # stop containers and clean artefacts
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

CHANNEL_NAME="mychannel"
CC_NAME="asset-transfer"
CC_SRC_PATH="./chaincode/asset-transfer"
CC_RUNTIME_LANGUAGE="java"
CC_VERSION="1.0"
CC_SEQUENCE="1"
CC_INIT_FCN="InitLedger"
DELAY=3
MAX_RETRY=5
VERBOSE=false

# configtxgen needs configtx.yaml
export CONFIGTX_CFG_PATH="${PWD}/network/configtx"
# peer binary needs core.yaml (ships in <fabric-binaries>/config/)
export FABRIC_PEER_CFG_PATH="${PWD}/config"
export FABRIC_CFG_PATH="${CONFIGTX_CFG_PATH}"
export PATH="${PWD}/bin:${PATH}"

# ── Colour helpers ────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }
# ── peer wrapper: use config/ for core.yaml, not configtx/ ───────────────────
peer() {
  FABRIC_CFG_PATH="${FABRIC_PEER_CFG_PATH}" command peer "$@"
}


# ── Prerequisite check ────────────────────────────────────────────────────────
checkPrereqs() {
  for tool in docker docker-compose peer cryptogen configtxgen; do
    command -v "$tool" &>/dev/null || error "Missing prerequisite: $tool"
  done
  info "All prerequisites found."
}

# ── Crypto material ───────────────────────────────────────────────────────────
generateCrypto() {
  info "Generating crypto material with cryptogen…"
  cryptogen generate \
    --config=./network/crypto-config/crypto-config.yaml \
    --output=./organizations
  info "Crypto material written to ./organizations"
}

# ── Genesis block + channel tx ────────────────────────────────────────────────
generateChannelArtifacts() {
  info "Generating genesis block…"
  mkdir -p ./network/channel-artifacts
  configtxgen \
    -profile TwoOrgsOrdererGenesis \
    -channelID system-channel \
    -outputBlock ./network/channel-artifacts/genesis.block

  info "Generating channel creation transaction…"
  configtxgen \
    -profile TwoOrgsChannel \
    -outputCreateChannelTx ./network/channel-artifacts/${CHANNEL_NAME}.tx \
    -channelID "${CHANNEL_NAME}"

  info "Generating anchor peer updates…"
  for org in Org1MSP Org2MSP; do
    configtxgen \
      -profile TwoOrgsChannel \
      -outputAnchorPeersUpdate "./network/channel-artifacts/${org}anchors.tx" \
      -channelID "${CHANNEL_NAME}" \
      -asOrg "${org}"
  done
}

# ── Docker compose ────────────────────────────────────────────────────────────
networkUp() {
  checkPrereqs
  generateCrypto
  generateChannelArtifacts
  info "Starting network containers…"
  docker-compose up -d
  info "Network is up. Waiting ${DELAY}s for peers to stabilize…"
  sleep "$DELAY"
}

networkDown() {
  info "Tearing down the network…"
  docker-compose down --volumes --remove-orphans 2>/dev/null || true
  docker rm -f $(docker ps -aq --filter "name=dev-peer") 2>/dev/null || true
  docker rmi -f $(docker images -q "dev-peer*") 2>/dev/null || true
  rm -rf ./organizations ./network/channel-artifacts
  info "Network removed."
}

# ── Channel operations ────────────────────────────────────────────────────────
setGlobalsPeer0Org1() {
  export CORE_PEER_LOCALMSPID="Org1MSP"
  export CORE_PEER_TLS_ROOTCERT_FILE="./organizations/org1/peers/peer0/tls/ca.crt"
  export CORE_PEER_MSPCONFIGPATH="./organizations/org1/users/Admin@org1.example.com/msp"
  export CORE_PEER_ADDRESS="localhost:7051"
}

setGlobalsPeer0Org2() {
  export CORE_PEER_LOCALMSPID="Org2MSP"
  export CORE_PEER_TLS_ROOTCERT_FILE="./organizations/org2/peers/peer0/tls/ca.crt"
  export CORE_PEER_MSPCONFIGPATH="./organizations/org2/users/Admin@org2.example.com/msp"
  export CORE_PEER_ADDRESS="localhost:9051"
}

createChannel() {
  info "Creating channel ${CHANNEL_NAME}…"
  setGlobalsPeer0Org1
  peer channel create \
    -o localhost:7050 \
    -c "${CHANNEL_NAME}" \
    -f "./network/channel-artifacts/${CHANNEL_NAME}.tx" \
    --outputBlock "./network/channel-artifacts/${CHANNEL_NAME}.block" \
    --tls \
    --cafile "./organizations/orderer/tls/ca.crt"

  info "Joining peers to ${CHANNEL_NAME}…"
  for peer_env in setGlobalsPeer0Org1 setGlobalsPeer0Org2; do
    $peer_env
    peer channel join -b "./network/channel-artifacts/${CHANNEL_NAME}.block"
  done

  info "Setting anchor peers…"
  setGlobalsPeer0Org1
  peer channel update \
    -o localhost:7050 \
    -c "${CHANNEL_NAME}" \
    -f "./network/channel-artifacts/Org1MSPanchors.tx" \
    --tls --cafile "./organizations/orderer/tls/ca.crt"

  setGlobalsPeer0Org2
  peer channel update \
    -o localhost:7050 \
    -c "${CHANNEL_NAME}" \
    -f "./network/channel-artifacts/Org2MSPanchors.tx" \
    --tls --cafile "./organizations/orderer/tls/ca.crt"

  info "Channel ${CHANNEL_NAME} created and peers joined."
}

# ── Chaincode lifecycle (Fabric 2.x) ─────────────────────────────────────────
deployCC() {
  info "Building Java chaincode…"
  pushd "${CC_SRC_PATH}" >/dev/null
  mvn clean package -DskipTests -q
  popd >/dev/null

  info "Packaging chaincode…"
  peer lifecycle chaincode package "${CC_NAME}.tar.gz" \
    --path "${CC_SRC_PATH}" \
    --lang "${CC_RUNTIME_LANGUAGE}" \
    --label "${CC_NAME}_${CC_VERSION}"

  # Install on all anchor peers
  for peer_env in setGlobalsPeer0Org1 setGlobalsPeer0Org2; do
    $peer_env
    info "Installing on ${CORE_PEER_ADDRESS}…"
    peer lifecycle chaincode install "${CC_NAME}.tar.gz"
  done

  # Query installed package ID
  setGlobalsPeer0Org1
  CC_PACKAGE_ID=$(peer lifecycle chaincode queryinstalled \
    | grep "${CC_NAME}_${CC_VERSION}" \
    | awk -F 'Package ID: ' '{print $2}' \
    | awk -F ',' '{print $1}')
  info "Package ID: ${CC_PACKAGE_ID}"
  export CC_PACKAGE_ID

  # Approve for both orgs
  for peer_env in setGlobalsPeer0Org1 setGlobalsPeer0Org2; do
    $peer_env
    info "Approving chaincode for ${CORE_PEER_LOCALMSPID}…"
    peer lifecycle chaincode approveformyorg \
      -o localhost:7050 \
      --channelID "${CHANNEL_NAME}" \
      --name "${CC_NAME}" \
      --version "${CC_VERSION}" \
      --package-id "${CC_PACKAGE_ID}" \
      --sequence "${CC_SEQUENCE}" \
      --tls --cafile "./organizations/orderer/tls/ca.crt"
  done

  # Check commit readiness
  setGlobalsPeer0Org1
  peer lifecycle chaincode checkcommitreadiness \
    --channelID "${CHANNEL_NAME}" \
    --name "${CC_NAME}" \
    --version "${CC_VERSION}" \
    --sequence "${CC_SEQUENCE}" \
    --output json

  # Commit chaincode definition
  info "Committing chaincode to channel…"
  peer lifecycle chaincode commit \
    -o localhost:7050 \
    --channelID "${CHANNEL_NAME}" \
    --name "${CC_NAME}" \
    --version "${CC_VERSION}" \
    --sequence "${CC_SEQUENCE}" \
    --tls --cafile "./organizations/orderer/tls/ca.crt" \
    --peerAddresses localhost:7051 \
    --tlsRootCertFiles "./organizations/org1/peers/peer0/tls/ca.crt" \
    --peerAddresses localhost:9051 \
    --tlsRootCertFiles "./organizations/org2/peers/peer0/tls/ca.crt"

  sleep 3

  # Initialize ledger
  info "Invoking ${CC_INIT_FCN}…"
  peer chaincode invoke \
    -o localhost:7050 \
    -C "${CHANNEL_NAME}" \
    -n "${CC_NAME}" \
    --tls --cafile "./organizations/orderer/tls/ca.crt" \
    --peerAddresses localhost:7051 \
    --tlsRootCertFiles "./organizations/org1/peers/peer0/tls/ca.crt" \
    --peerAddresses localhost:9051 \
    --tlsRootCertFiles "./organizations/org2/peers/peer0/tls/ca.crt" \
    -c "{\"function\":\"${CC_INIT_FCN}\",\"Args\":[]}"

  info "Chaincode ${CC_NAME} deployed and initialized successfully."
}

# ── Entry point ───────────────────────────────────────────────────────────────
case "${1:-}" in
  up)             networkUp ;;
  createChannel)  createChannel ;;
  deployCC)       deployCC ;;
  down)           networkDown ;;
  *)
    echo "Usage: $0 {up|createChannel|deployCC|down}"
    exit 1
    ;;
esac