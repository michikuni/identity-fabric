#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# network.sh  –  Bring up / tear down the 2-org Hyperledger Fabric network
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

CHANNEL_NAME="mychannel"
CC_NAME="asset-transfer"
CC_SRC_PATH="${ROOT_DIR}/chaincode/asset-transfer"
CC_RUNTIME_LANGUAGE="java"
CC_VERSION="1.0"
CC_SEQUENCE="1"
CC_INIT_FCN="InitLedger"
DELAY=3
MAX_RETRY=5
VERBOSE=false

# configtxgen reads configtx.yaml from here
export FABRIC_CFG_PATH="${ROOT_DIR}/network/configtx"
export PATH="${ROOT_DIR}/bin:${PATH}"

# ── Colour helpers ────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ── peer wrapper ──────────────────────────────────────────────────────────────
# The peer binary needs core.yaml; it ships in <fabric-download>/config/.
# We copy it into network/configtx/ once so a single FABRIC_CFG_PATH works
# for both configtxgen (configtx.yaml) and peer (core.yaml + orderer.yaml).
ensureCoreYaml() {
  local cfg_dir="${ROOT_DIR}/network/configtx"
  if [[ ! -f "${cfg_dir}/core.yaml" ]]; then
    local src="${ROOT_DIR}/config/core.yaml"
    if [[ ! -f "${src}" ]]; then
      error "core.yaml not found at ${src}.\nRun: curl -sSL https://bit.ly/2ysbOFE | bash -s -- 2.5.4 1.5.7\nthen: cp fabric-samples/config/core.yaml ${cfg_dir}/"
    fi
    cp "${src}" "${cfg_dir}/core.yaml"
    info "Copied core.yaml into ${cfg_dir}/"
  fi
}

# ── Prerequisite check ────────────────────────────────────────────────────────
checkPrereqs() {
  for tool in docker docker-compose cryptogen configtxgen peer; do
    command -v "$tool" &>/dev/null || error "Missing prerequisite: $tool"
  done
  info "All prerequisites found."
}

# ── Crypto material ───────────────────────────────────────────────────────────
generateCrypto() {
  info "Generating crypto material with cryptogen…"
  cryptogen generate \
    --config="${ROOT_DIR}/network/crypto-config/crypto-config.yaml" \
    --output="${ROOT_DIR}/organizations"
  info "Crypto material written to ${ROOT_DIR}/organizations"
}

# ── Genesis block + channel tx ────────────────────────────────────────────────
generateChannelArtifacts() {
  info "Generating genesis block…"
  mkdir -p "${ROOT_DIR}/network/channel-artifacts"

  configtxgen \
    -profile TwoOrgsOrdererGenesis \
    -channelID system-channel \
    -outputBlock "${ROOT_DIR}/network/channel-artifacts/genesis.block"

  info "Generating channel creation transaction…"
  configtxgen \
    -profile TwoOrgsChannel \
    -outputCreateChannelTx "${ROOT_DIR}/network/channel-artifacts/${CHANNEL_NAME}.tx" \
    -channelID "${CHANNEL_NAME}"

  info "Generating anchor peer updates…"
  for org in Org1MSP Org2MSP; do
    configtxgen \
      -profile TwoOrgsChannel \
      -outputAnchorPeersUpdate "${ROOT_DIR}/network/channel-artifacts/${org}anchors.tx" \
      -channelID "${CHANNEL_NAME}" \
      -asOrg "${org}"
  done
}

# ── Docker compose ────────────────────────────────────────────────────────────
networkUp() {
  checkPrereqs
  ensureCoreYaml
  generateCrypto
  generateChannelArtifacts
  info "Starting network containers…"
  docker-compose -f "${ROOT_DIR}/docker-compose.yaml" up -d
  info "Network is up. Waiting ${DELAY}s for peers to stabilize…"
  sleep "${DELAY}"
}

networkDown() {
  info "Tearing down the network…"
  docker-compose -f "${ROOT_DIR}/docker-compose.yaml" down --volumes --remove-orphans 2>/dev/null || true
  docker rm -f $(docker ps -aq --filter "name=dev-peer") 2>/dev/null || true
  docker rmi -f $(docker images -q "dev-peer*") 2>/dev/null || true
  rm -rf "${ROOT_DIR}/organizations" "${ROOT_DIR}/network/channel-artifacts"
  info "Network removed."
}

# ── Peer env helpers (absolute paths — no more relative-path confusion) ────────
setGlobalsPeer0Org1() {
  export CORE_PEER_LOCALMSPID="Org1MSP"
  export CORE_PEER_TLS_ROOTCERT_FILE="${ROOT_DIR}/organizations/org1/peers/peer0/tls/ca.crt"
  export CORE_PEER_MSPCONFIGPATH="${ROOT_DIR}/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp"
  export CORE_PEER_ADDRESS="localhost:7051"
  export CORE_PEER_TLS_ENABLED="true"
}

setGlobalsPeer1Org1() {
  export CORE_PEER_LOCALMSPID="Org1MSP"
  export CORE_PEER_TLS_ROOTCERT_FILE="${ROOT_DIR}/organizations/org1/peers/peer1/tls/ca.crt"
  export CORE_PEER_MSPCONFIGPATH="${ROOT_DIR}/organizations/peerOrganizations/org1.example.com/users/User1@org1.example.com/msp"
  export CORE_PEER_ADDRESS="localhost:8051"
  export CORE_PEER_TLS_ENABLED="true"
}

setGlobalsPeer0Org2() {
  export CORE_PEER_LOCALMSPID="Org2MSP"
  export CORE_PEER_TLS_ROOTCERT_FILE="${ROOT_DIR}/organizations/org2/peers/peer0/tls/ca.crt"
  export CORE_PEER_MSPCONFIGPATH="${ROOT_DIR}/organizations/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp"
  export CORE_PEER_ADDRESS="localhost:9051"
  export CORE_PEER_TLS_ENABLED="true"
}

setGlobalsPeer1Org2() {
  export CORE_PEER_LOCALMSPID="Org2MSP"
  export CORE_PEER_TLS_ROOTCERT_FILE="${ROOT_DIR}/organizations/org2/peers/peer1/tls/ca.crt"
  export CORE_PEER_MSPCONFIGPATH="${ROOT_DIR}/organizations/peerOrganizations/org2.example.com/users/User1@org2.example.com/msp"
  export CORE_PEER_ADDRESS="localhost:10051"
  export CORE_PEER_TLS_ENABLED="true"
}

# ── Channel operations ────────────────────────────────────────────────────────
createChannel() {
  ensureCoreYaml
  info "Creating channel ${CHANNEL_NAME}…"

  setGlobalsPeer0Org1
  peer channel create \
    -o localhost:7050 \
    -c "${CHANNEL_NAME}" \
    -f "${ROOT_DIR}/network/channel-artifacts/${CHANNEL_NAME}.tx" \
    --outputBlock "${ROOT_DIR}/network/channel-artifacts/${CHANNEL_NAME}.block" \
    --tls \
    --cafile "${ROOT_DIR}/organizations/orderer/tls/ca.crt"

  info "Joining peers to ${CHANNEL_NAME}…"
  for setGlobals in setGlobalsPeer0Org1 setGlobalsPeer0Org2; do
    ${setGlobals}
    peer channel join -b "${ROOT_DIR}/network/channel-artifacts/${CHANNEL_NAME}.block"
    info "  → ${CORE_PEER_ADDRESS} joined"
  done

  info "Setting anchor peers…"
  setGlobalsPeer0Org1
  peer channel update \
    -o localhost:7050 \
    -c "${CHANNEL_NAME}" \
    -f "${ROOT_DIR}/network/channel-artifacts/Org1MSPanchors.tx" \
    --tls --cafile "${ROOT_DIR}/organizations/orderer/tls/ca.crt"

  setGlobalsPeer0Org2
  peer channel update \
    -o localhost:7050 \
    -c "${CHANNEL_NAME}" \
    -f "${ROOT_DIR}/network/channel-artifacts/Org2MSPanchors.tx" \
    --tls --cafile "${ROOT_DIR}/organizations/orderer/tls/ca.crt"

  info "Channel ${CHANNEL_NAME} created and all peers joined."
}

# ── Chaincode lifecycle (Fabric 2.x) ─────────────────────────────────────────
deployCC() {
  ensureCoreYaml
  info "Building Java chaincode…"
  pushd "${CC_SRC_PATH}" >/dev/null
  mvn clean package -DskipTests -q
  popd >/dev/null

  info "Packaging chaincode…"
  peer lifecycle chaincode package "${ROOT_DIR}/${CC_NAME}.tar.gz" \
    --path "${CC_SRC_PATH}" \
    --lang "${CC_RUNTIME_LANGUAGE}" \
    --label "${CC_NAME}_${CC_VERSION}"

  for setGlobals in setGlobalsPeer0Org1 setGlobalsPeer0Org2; do
    ${setGlobals}
    info "Installing chaincode on ${CORE_PEER_ADDRESS}…"
    peer lifecycle chaincode install "${ROOT_DIR}/${CC_NAME}.tar.gz"
  done

  setGlobalsPeer0Org1
  CC_PACKAGE_ID=$(peer lifecycle chaincode queryinstalled \
    | grep "${CC_NAME}_${CC_VERSION}" \
    | awk -F 'Package ID: ' '{print $2}' \
    | awk -F ',' '{print $1}')
  info "Package ID: ${CC_PACKAGE_ID}"
  export CC_PACKAGE_ID

  for setGlobals in setGlobalsPeer0Org1 setGlobalsPeer0Org2; do
    ${setGlobals}
    info "Approving chaincode for ${CORE_PEER_LOCALMSPID}…"
    peer lifecycle chaincode approveformyorg \
      -o localhost:7050 \
      --channelID "${CHANNEL_NAME}" \
      --name "${CC_NAME}" \
      --version "${CC_VERSION}" \
      --package-id "${CC_PACKAGE_ID}" \
      --sequence "${CC_SEQUENCE}" \
      --tls --cafile "${ROOT_DIR}/organizations/orderer/tls/ca.crt"
  done

  setGlobalsPeer0Org1
  peer lifecycle chaincode checkcommitreadiness \
    --channelID "${CHANNEL_NAME}" \
    --name "${CC_NAME}" \
    --version "${CC_VERSION}" \
    --sequence "${CC_SEQUENCE}" \
    --output json

  info "Committing chaincode to channel…"
  peer lifecycle chaincode commit \
    -o localhost:7050 \
    --channelID "${CHANNEL_NAME}" \
    --name "${CC_NAME}" \
    --version "${CC_VERSION}" \
    --sequence "${CC_SEQUENCE}" \
    --tls --cafile "${ROOT_DIR}/organizations/orderer/tls/ca.crt" \
    --peerAddresses localhost:7051 \
    --tlsRootCertFiles "${ROOT_DIR}/organizations/org1/peers/peer0/tls/ca.crt" \
    --peerAddresses localhost:9051 \
    --tlsRootCertFiles "${ROOT_DIR}/organizations/org2/peers/peer0/tls/ca.crt"

  sleep 3

  info "Invoking ${CC_INIT_FCN}…"
  setGlobalsPeer0Org1
  peer chaincode invoke \
    -o localhost:7050 \
    -C "${CHANNEL_NAME}" \
    -n "${CC_NAME}" \
    --tls --cafile "${ROOT_DIR}/organizations/orderer/tls/ca.crt" \
    --peerAddresses localhost:7051 \
    --tlsRootCertFiles "${ROOT_DIR}/organizations/org1/peers/peer0/tls/ca.crt" \
    --peerAddresses localhost:9051 \
    --tlsRootCertFiles "${ROOT_DIR}/organizations/org2/peers/peer0/tls/ca.crt" \
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