# TrustID — Tài liệu Deploy trên Google Cloud VPS

> **Môi trường mục tiêu:** Google Cloud VPS · Ubuntu 22.04 · e2-standard-4 (4 vCPU, 16 GB RAM) · IP: `34.70.92.195`
> **Phân tích từ:** `docker-compose.yaml`, `crypto-config.yaml`, `configtx.yaml`, `network.sh`, `application.yml`, `application.properties`, `FabricGatewayConfig.kt`

---

## 1. Kiến trúc Hyperledger Fabric

### 1.1 Tổng quan topology

```
┌─────────────────────────────────────────────────────────────────┐
│                     fabric_network (Docker bridge)              │
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐  │
│  │  ca.org1     │    │  ca.org2     │    │ orderer          │  │
│  │  :7054       │    │  :8054       │    │ :7050 (gRPC)     │  │
│  │  fabric-ca   │    │  fabric-ca   │    │ :7053 (admin)    │  │
│  │  1.5.7       │    │  1.5.7       │    │ :9443 (ops)      │  │
│  └──────────────┘    └──────────────┘    └──────────────────┘  │
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐                          │
│  │ peer0.org1   │    │ peer1.org1   │    Org1MSP               │
│  │ :7051        │    │ :8051        │                          │
│  │ :7052 (cc)   │    │ :8052 (cc)   │                          │
│  │ :9444 (ops)  │    │ :9445 (ops)  │                          │
│  └──────────────┘    └──────────────┘                          │
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐                          │
│  │ peer0.org2   │    │ peer1.org2   │    Org2MSP               │
│  │ :9051        │    │ :10051       │                          │
│  │ :9052 (cc)   │    │ :10052 (cc)  │                          │
│  │ :9446 (ops)  │    │ :9447 (ops)  │                          │
│  └──────────────┘    └──────────────┘                          │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  fabric-spring-api  :8080  (Spring Boot + Kotlin)       │   │
│  │  ├── com.mpcorp.identity  (HR/DID/VC service)           │   │
│  │  └── org.fabric.api       (Fabric Gateway bridge)       │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌──────────────────────────────┐                              │
│  │  mysql  :3306                │                              │
│  │  DB: identity_db             │                              │
│  └──────────────────────────────┘                              │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 Thành phần chi tiết

| Thành phần | Số lượng | Image | Ghi chú |
|---|---|---|---|
| **Orderer** | 1 | `hyperledger/fabric-orderer:2.5.4` | etcdraft consensus, TLS bật |
| **CA Org1** | 1 | `hyperledger/fabric-ca:1.5.7` | Cấp cert cho Org1 |
| **CA Org2** | 1 | `hyperledger/fabric-ca:1.5.7` | Cấp cert cho Org2 |
| **Peer Org1** | 2 | `hyperledger/fabric-peer:2.5.4` | peer0 (anchor), peer1 |
| **Peer Org2** | 2 | `hyperledger/fabric-peer:2.5.4` | peer0 (anchor), peer1 |
| **Channel** | 1 | — | `mychannel`, MAJORITY endorsement |
| **Chaincode** | 1 | Java (build runtime) | `identity-ledger` v1.0 |
| **Spring Boot** | 1 | Custom (JDK 17) | Port 8080, kết nối peer0.org1:7051 |
| **MySQL** | 1 | `mysql:8.0` | identity_db |

### 1.3 Luồng kết nối Backend → Fabric

```
Spring Boot App
  └─ FabricGatewayConfig
       ├─ Đọc TLS CA cert: organizations/peerOrganizations/org1.../tls/ca.crt
       ├─ Đọc User cert:   organizations/peerOrganizations/org1.../signcerts/cert.pem
       ├─ Đọc Private key: organizations/peerOrganizations/org1.../keystore/*_sk
       └─ Kết nối gRPC+mTLS → peer0.org1.example.com:7051
            └─ Fabric Gateway SDK 1.7.0
                 ├─ evaluate timeout: 5s
                 ├─ endorse timeout:  15s
                 ├─ submit timeout:   5s
                 └─ commit timeout:   60s
```

**MSP ID:** `Org1MSP` | **Channel:** `mychannel` | **Chaincode:** `identity-ledger`

---

## 2. Danh sách Port

### 2.1 Port cần mở trên GCP Firewall (external)

| Port | Protocol | Service | Cần thiết |
|---|---|---|---|
| **22** | TCP | SSH | Bắt buộc (giới hạn IP của bạn) |
| **8080** | TCP | Spring Boot API | Bắt buộc (Flutter app gọi) |

> **Tất cả port khác đều KHÔNG mở ra internet.** Giao tiếp nội bộ qua Docker bridge `fabric_network`.

### 2.2 Port nội bộ Docker (không mở GCP Firewall)

| Port | Service | Loại |
|---|---|---|
| 7050 | orderer — gRPC | Internal |
| 7051 | peer0.org1 — gRPC | Internal (backend kết nối) |
| 7052 | peer0.org1 — chaincode listener | Internal |
| 7053 | orderer — admin (osnadmin) | Internal |
| 7054 | ca.org1 | Internal |
| 8051 | peer1.org1 — gRPC | Internal |
| 8052 | peer1.org1 — chaincode listener | Internal |
| 8054 | ca.org2 | Internal |
| 9051 | peer0.org2 — gRPC | Internal |
| 9052 | peer0.org2 — chaincode listener | Internal |
| 9443 | orderer — operations/metrics | Internal |
| 9444 | peer0.org1 — operations | Internal |
| 9445 | peer1.org1 — operations | Internal |
| 9446 | peer0.org2 — operations | Internal |
| 9447 | peer1.org2 — operations | Internal |
| 9051 | peer0.org2 — gRPC | Internal |
| 10051 | peer1.org2 — gRPC | Internal |
| 10052 | peer1.org2 — chaincode listener | Internal |
| 3306 | MySQL | Internal |

---

## 3. Docker Compose phù hợp kiến trúc thật

Tạo file `/home/ubuntu/identity-fabric/docker-compose.prod.yaml` trên VPS:

```yaml
version: '3.7'

networks:
  fabric_network:
    name: fabric_network

volumes:
  orderer.example.com:
  peer0.org1.example.com:
  peer1.org1.example.com:
  peer0.org2.example.com:
  peer1.org2.example.com:
  ca.org1.example.com:
  ca.org2.example.com:
  mysql_data:

services:

  # ── Certificate Authorities ────────────────────────────────────────────────

  ca.org1.example.com:
    image: hyperledger/fabric-ca:1.5.7
    container_name: ca.org1.example.com
    environment:
      - FABRIC_CA_HOME=/etc/hyperledger/fabric-ca-server
      - FABRIC_CA_SERVER_CA_NAME=ca-org1
      - FABRIC_CA_SERVER_TLS_ENABLED=true
      - FABRIC_CA_SERVER_PORT=7054
    ports:
      - "127.0.0.1:7054:7054"
    command: sh -c 'fabric-ca-server start -b admin:adminpw -d'
    volumes:
      - ca.org1.example.com:/etc/hyperledger/fabric-ca-server
    networks:
      - fabric_network
    restart: unless-stopped

  ca.org2.example.com:
    image: hyperledger/fabric-ca:1.5.7
    container_name: ca.org2.example.com
    environment:
      - FABRIC_CA_HOME=/etc/hyperledger/fabric-ca-server
      - FABRIC_CA_SERVER_CA_NAME=ca-org2
      - FABRIC_CA_SERVER_TLS_ENABLED=true
      - FABRIC_CA_SERVER_PORT=8054
    ports:
      - "127.0.0.1:8054:8054"
    command: sh -c 'fabric-ca-server start -b admin:adminpw -d'
    volumes:
      - ca.org2.example.com:/etc/hyperledger/fabric-ca-server
    networks:
      - fabric_network
    restart: unless-stopped

  # ── Orderer ────────────────────────────────────────────────────────────────

  orderer.example.com:
    image: hyperledger/fabric-orderer:2.5.4
    container_name: orderer.example.com
    environment:
      - FABRIC_LOGGING_SPEC=INFO
      - ORDERER_GENERAL_LISTENADDRESS=0.0.0.0
      - ORDERER_GENERAL_LISTENPORT=7050
      - ORDERER_GENERAL_LOCALMSPID=OrdererMSP
      - ORDERER_GENERAL_LOCALMSPDIR=/var/hyperledger/orderer/msp
      - ORDERER_GENERAL_TLS_ENABLED=true
      - ORDERER_GENERAL_TLS_PRIVATEKEY=/var/hyperledger/orderer/tls/server.key
      - ORDERER_GENERAL_TLS_CERTIFICATE=/var/hyperledger/orderer/tls/server.crt
      - ORDERER_GENERAL_TLS_ROOTCAS=[/var/hyperledger/orderer/tls/ca.crt]
      - ORDERER_GENERAL_CLUSTER_CLIENTCERTIFICATE=/var/hyperledger/orderer/tls/server.crt
      - ORDERER_GENERAL_CLUSTER_CLIENTPRIVATEKEY=/var/hyperledger/orderer/tls/server.key
      - ORDERER_GENERAL_CLUSTER_ROOTCAS=[/var/hyperledger/orderer/tls/ca.crt]
      - ORDERER_GENERAL_BOOTSTRAPMETHOD=none
      - ORDERER_CHANNELPARTICIPATION_ENABLED=true
      - ORDERER_ADMIN_TLS_ENABLED=true
      - ORDERER_ADMIN_TLS_CERTIFICATE=/var/hyperledger/orderer/tls/server.crt
      - ORDERER_ADMIN_TLS_PRIVATEKEY=/var/hyperledger/orderer/tls/server.key
      - ORDERER_ADMIN_TLS_ROOTCAS=[/var/hyperledger/orderer/tls/ca.crt]
      - ORDERER_ADMIN_TLS_CLIENTROOTCAS=[/var/hyperledger/orderer/tls/ca.crt]
      - ORDERER_ADMIN_LISTENADDRESS=0.0.0.0:7053
      - ORDERER_OPERATIONS_LISTENADDRESS=0.0.0.0:9443
    working_dir: /root
    command: orderer
    volumes:
      - ./fabric-network/network/channel-artifacts/genesis.block:/var/hyperledger/orderer/orderer.genesis.block
      - ./fabric-network/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp:/var/hyperledger/orderer/msp
      - ./fabric-network/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls:/var/hyperledger/orderer/tls
      - orderer.example.com:/var/hyperledger/production/orderer
    ports:
      - "127.0.0.1:7050:7050"
      - "127.0.0.1:7053:7053"
    networks:
      - fabric_network
    restart: unless-stopped

  # ── Org1 Peers ─────────────────────────────────────────────────────────────

  peer0.org1.example.com:
    image: hyperledger/fabric-peer:2.5.4
    container_name: peer0.org1.example.com
    environment:
      - CORE_VM_ENDPOINT=unix:///host/var/run/docker.sock
      - CORE_VM_DOCKER_HOSTCONFIG_NETWORKMODE=fabric_network
      - FABRIC_LOGGING_SPEC=INFO
      - CORE_PEER_TLS_ENABLED=true
      - CORE_PEER_PROFILE_ENABLED=false
      - CORE_PEER_TLS_CERT_FILE=/etc/hyperledger/fabric/tls/server.crt
      - CORE_PEER_TLS_KEY_FILE=/etc/hyperledger/fabric/tls/server.key
      - CORE_PEER_TLS_ROOTCERT_FILE=/etc/hyperledger/fabric/tls/ca.crt
      - CORE_PEER_ID=peer0.org1.example.com
      - CORE_PEER_ADDRESS=peer0.org1.example.com:7051
      - CORE_PEER_LISTENADDRESS=0.0.0.0:7051
      - CORE_PEER_CHAINCODEADDRESS=peer0.org1.example.com:7052
      - CORE_PEER_CHAINCODELISTENADDRESS=0.0.0.0:7052
      - CORE_PEER_GOSSIP_BOOTSTRAP=peer1.org1.example.com:8051
      - CORE_PEER_GOSSIP_EXTERNALENDPOINT=peer0.org1.example.com:7051
      - CORE_PEER_LOCALMSPID=Org1MSP
      - CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/msp
      - CORE_OPERATIONS_LISTENADDRESS=0.0.0.0:9444
      - CORE_METRICS_PROVIDER=prometheus
    volumes:
      - /var/run/docker.sock:/host/var/run/docker.sock
      - ./fabric-network/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/msp:/etc/hyperledger/fabric/msp
      - ./fabric-network/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls:/etc/hyperledger/fabric/tls
      - peer0.org1.example.com:/var/hyperledger/production
    working_dir: /root
    command: peer node start
    ports:
      - "127.0.0.1:7051:7051"
    networks:
      - fabric_network
    restart: unless-stopped
    depends_on:
      - orderer.example.com

  peer1.org1.example.com:
    image: hyperledger/fabric-peer:2.5.4
    container_name: peer1.org1.example.com
    environment:
      - CORE_VM_ENDPOINT=unix:///host/var/run/docker.sock
      - CORE_VM_DOCKER_HOSTCONFIG_NETWORKMODE=fabric_network
      - FABRIC_LOGGING_SPEC=INFO
      - CORE_PEER_TLS_ENABLED=true
      - CORE_PEER_PROFILE_ENABLED=false
      - CORE_PEER_TLS_CERT_FILE=/etc/hyperledger/fabric/tls/server.crt
      - CORE_PEER_TLS_KEY_FILE=/etc/hyperledger/fabric/tls/server.key
      - CORE_PEER_TLS_ROOTCERT_FILE=/etc/hyperledger/fabric/tls/ca.crt
      - CORE_PEER_ID=peer1.org1.example.com
      - CORE_PEER_ADDRESS=peer1.org1.example.com:8051
      - CORE_PEER_LISTENADDRESS=0.0.0.0:8051
      - CORE_PEER_CHAINCODEADDRESS=peer1.org1.example.com:8052
      - CORE_PEER_CHAINCODELISTENADDRESS=0.0.0.0:8052
      - CORE_PEER_GOSSIP_BOOTSTRAP=peer0.org1.example.com:7051
      - CORE_PEER_GOSSIP_EXTERNALENDPOINT=peer1.org1.example.com:8051
      - CORE_PEER_LOCALMSPID=Org1MSP
      - CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/msp
      - CORE_OPERATIONS_LISTENADDRESS=0.0.0.0:9445
    volumes:
      - /var/run/docker.sock:/host/var/run/docker.sock
      - ./fabric-network/organizations/peerOrganizations/org1.example.com/peers/peer1.org1.example.com/msp:/etc/hyperledger/fabric/msp
      - ./fabric-network/organizations/peerOrganizations/org1.example.com/peers/peer1.org1.example.com/tls:/etc/hyperledger/fabric/tls
      - peer1.org1.example.com:/var/hyperledger/production
    working_dir: /root
    command: peer node start
    ports:
      - "127.0.0.1:8051:8051"
    networks:
      - fabric_network
    restart: unless-stopped
    depends_on:
      - orderer.example.com

  # ── Org2 Peers ─────────────────────────────────────────────────────────────

  peer0.org2.example.com:
    image: hyperledger/fabric-peer:2.5.4
    container_name: peer0.org2.example.com
    environment:
      - CORE_VM_ENDPOINT=unix:///host/var/run/docker.sock
      - CORE_VM_DOCKER_HOSTCONFIG_NETWORKMODE=fabric_network
      - FABRIC_LOGGING_SPEC=INFO
      - CORE_PEER_TLS_ENABLED=true
      - CORE_PEER_PROFILE_ENABLED=false
      - CORE_PEER_TLS_CERT_FILE=/etc/hyperledger/fabric/tls/server.crt
      - CORE_PEER_TLS_KEY_FILE=/etc/hyperledger/fabric/tls/server.key
      - CORE_PEER_TLS_ROOTCERT_FILE=/etc/hyperledger/fabric/tls/ca.crt
      - CORE_PEER_ID=peer0.org2.example.com
      - CORE_PEER_ADDRESS=peer0.org2.example.com:9051
      - CORE_PEER_LISTENADDRESS=0.0.0.0:9051
      - CORE_PEER_CHAINCODEADDRESS=peer0.org2.example.com:9052
      - CORE_PEER_CHAINCODELISTENADDRESS=0.0.0.0:9052
      - CORE_PEER_GOSSIP_BOOTSTRAP=peer1.org2.example.com:10051
      - CORE_PEER_GOSSIP_EXTERNALENDPOINT=peer0.org2.example.com:9051
      - CORE_PEER_LOCALMSPID=Org2MSP
      - CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/msp
      - CORE_OPERATIONS_LISTENADDRESS=0.0.0.0:9446
    volumes:
      - /var/run/docker.sock:/host/var/run/docker.sock
      - ./fabric-network/organizations/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/msp:/etc/hyperledger/fabric/msp
      - ./fabric-network/organizations/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls:/etc/hyperledger/fabric/tls
      - peer0.org2.example.com:/var/hyperledger/production
    working_dir: /root
    command: peer node start
    ports:
      - "127.0.0.1:9051:9051"
    networks:
      - fabric_network
    restart: unless-stopped
    depends_on:
      - orderer.example.com

  peer1.org2.example.com:
    image: hyperledger/fabric-peer:2.5.4
    container_name: peer1.org2.example.com
    environment:
      - CORE_VM_ENDPOINT=unix:///host/var/run/docker.sock
      - CORE_VM_DOCKER_HOSTCONFIG_NETWORKMODE=fabric_network
      - FABRIC_LOGGING_SPEC=INFO
      - CORE_PEER_TLS_ENABLED=true
      - CORE_PEER_PROFILE_ENABLED=false
      - CORE_PEER_TLS_CERT_FILE=/etc/hyperledger/fabric/tls/server.crt
      - CORE_PEER_TLS_KEY_FILE=/etc/hyperledger/fabric/tls/server.key
      - CORE_PEER_TLS_ROOTCERT_FILE=/etc/hyperledger/fabric/tls/ca.crt
      - CORE_PEER_ID=peer1.org2.example.com
      - CORE_PEER_ADDRESS=peer1.org2.example.com:10051
      - CORE_PEER_LISTENADDRESS=0.0.0.0:10051
      - CORE_PEER_CHAINCODEADDRESS=peer1.org2.example.com:10052
      - CORE_PEER_CHAINCODELISTENADDRESS=0.0.0.0:10052
      - CORE_PEER_GOSSIP_BOOTSTRAP=peer0.org2.example.com:9051
      - CORE_PEER_GOSSIP_EXTERNALENDPOINT=peer1.org2.example.com:10051
      - CORE_PEER_LOCALMSPID=Org2MSP
      - CORE_PEER_MSPCONFIGPATH=/etc/hyperledger/fabric/msp
      - CORE_OPERATIONS_LISTENADDRESS=0.0.0.0:9447
    volumes:
      - /var/run/docker.sock:/host/var/run/docker.sock
      - ./fabric-network/organizations/peerOrganizations/org2.example.com/peers/peer1.org2.example.com/msp:/etc/hyperledger/fabric/msp
      - ./fabric-network/organizations/peerOrganizations/org2.example.com/peers/peer1.org2.example.com/tls:/etc/hyperledger/fabric/tls
      - peer1.org2.example.com:/var/hyperledger/production
    working_dir: /root
    command: peer node start
    ports:
      - "127.0.0.1:10051:10051"
    networks:
      - fabric_network
    restart: unless-stopped
    depends_on:
      - orderer.example.com

  # ── MySQL ───────────────────────────────────────────────────────────────────

  mysql:
    image: mysql:8.0
    container_name: mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-StrongPass@2026}
      MYSQL_DATABASE: identity_db
    ports:
      - "127.0.0.1:3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - fabric_network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD:-StrongPass@2026}"]
      interval: 10s
      timeout: 5s
      retries: 10

  # ── Spring Boot API ─────────────────────────────────────────────────────────

  fabric-api:
    build:
      context: ./fabric-spring-backend
      dockerfile: Dockerfile
    container_name: fabric-spring-api
    ports:
      - "0.0.0.0:8080:8080"
    environment:
      # MySQL
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/identity_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD:-StrongPass@2026}
      # Fabric connection — paths trỏ vào /crypto mount
      FABRIC_MSP_ID: Org1MSP
      FABRIC_CHANNEL_NAME: mychannel
      FABRIC_CHAINCODE_NAME: identity-ledger
      FABRIC_PEER_ENDPOINT: peer0.org1.example.com:7051
      FABRIC_PEER_TLS_CERT_PATH: /crypto/org1/peers/peer0.org1.example.com/tls/ca.crt
      FABRIC_GATEWAY_CERT_PATH: /crypto/org1/users/User1@org1.example.com/msp/signcerts/User1@org1.example.com-cert.pem
      FABRIC_GATEWAY_KEY_PATH: /crypto/org1/users/User1@org1.example.com/msp/keystore/
      # JWT & VC secrets
      JWT_SECRET: ${JWT_SECRET:-a1f4b9c2d75e3f8a6e1b9cd0f3471e89a023c17de8fa2bd6c57c1a3e90d4f2b1=}
      VC_SECRET: ${VC_SECRET:-vc-secret-trustid-org1-2026}
      # Admin seed
      ADMIN_EMAIL: ${ADMIN_EMAIL:-admin@mpcorp.com}
      ADMIN_PHONE: ${ADMIN_PHONE:-0900000000}
      ADMIN_PASSWORD: ${ADMIN_PASSWORD:-Admin@123}
      # Spring profile
      SPRING_PROFILES_ACTIVE: prod
    volumes:
      - ./fabric-network/organizations/peerOrganizations/org1.example.com:/crypto/org1:ro
    networks:
      - fabric_network
    depends_on:
      mysql:
        condition: service_healthy
      peer0.org1.example.com:
        condition: service_started
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5
```

---

## 4. GCP Firewall Rules

Chạy các lệnh này từ **Google Cloud Console → VPC Network → Firewall** hoặc dùng `gcloud`:

```bash
# SSH — giới hạn chỉ IP của bạn (thay YOUR_IP)
gcloud compute firewall-rules create allow-ssh \
  --direction=INGRESS \
  --priority=1000 \
  --network=default \
  --action=ALLOW \
  --rules=tcp:22 \
  --source-ranges=YOUR_IP/32 \
  --target-tags=trustid-vps

# API cho Flutter app (public)
gcloud compute firewall-rules create allow-trustid-api \
  --direction=INGRESS \
  --priority=1000 \
  --network=default \
  --action=ALLOW \
  --rules=tcp:8080 \
  --source-ranges=0.0.0.0/0 \
  --target-tags=trustid-vps
```

**Tất cả port Fabric (7050-10052) và MySQL (3306) KHÔNG tạo firewall rule** — chỉ bind `127.0.0.1` trong docker-compose.

---

## 5. Các bước Deploy theo thứ tự

### Bước 0 — Chuẩn bị VPS (chạy 1 lần)

```bash
# SSH vào VPS
ssh ubuntu@34.70.92.195

# Cài Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker ubuntu
newgrp docker

# Cài Docker Compose v2
sudo apt-get install -y docker-compose-plugin
docker compose version

# Cài Java 17 (dùng để build chaincode)
sudo apt-get install -y openjdk-17-jdk maven git
java -version

# Tải Fabric binaries (cryptogen, configtxgen, peer, osnadmin)
curl -sSL https://bit.ly/2ysbOFE | bash -s -- 2.5.4 1.5.7 --docker-images --no-samples
# Hoặc download thủ công:
# https://github.com/hyperledger/fabric/releases/tag/v2.5.4
export PATH=$PATH:$HOME/fabric-samples/bin
# Thêm vào ~/.bashrc để persist:
echo 'export PATH=$PATH:$HOME/fabric-samples/bin' >> ~/.bashrc
```

### Bước 1 — Upload mã nguồn lên VPS

```bash
# Từ máy local — rsync toàn bộ project (bỏ build artifacts)
rsync -avz --exclude='*/build/' --exclude='*/.gradle/' --exclude='*/node_modules/' \
  d:/Academy/identity-fabric/ \
  ubuntu@34.70.92.195:/home/ubuntu/identity-fabric/

# Copy docker-compose.prod.yaml (đã tạo ở Mục 3)
scp document_deploy_compose.yaml ubuntu@34.70.92.195:/home/ubuntu/identity-fabric/docker-compose.prod.yaml
```

### Bước 2 — Tạo file .env cho secrets

```bash
# Trên VPS
cd /home/ubuntu/identity-fabric
cat > .env << 'EOF'
MYSQL_ROOT_PASSWORD=StrongPass@2026!
JWT_SECRET=your-256-bit-jwt-secret-change-this-in-production
VC_SECRET=vc-secret-trustid-org1-2026-change-this
ADMIN_EMAIL=admin@mpcorp.com
ADMIN_PHONE=0900000000
ADMIN_PASSWORD=Admin@SecurePass2026!
EOF
chmod 600 .env
```

### Bước 3 — Sinh Crypto Material

```bash
cd /home/ubuntu/identity-fabric

# Đảm bảo Fabric binaries có trong PATH
export PATH=$PATH:$HOME/fabric-samples/bin
export FABRIC_CFG_PATH=$(pwd)/fabric-network/network/configtx

# Sinh cert/key cho tất cả orgs, peers, users
cryptogen generate \
  --config=./fabric-network/network/crypto-config/crypto-config.yaml \
  --output=./fabric-network/organizations

# Kiểm tra
ls ./fabric-network/organizations/
# Phải thấy: ordererOrganizations/  peerOrganizations/
```

### Bước 4 — Sinh Channel Artifacts (Genesis Block)

```bash
cd /home/ubuntu/identity-fabric
export FABRIC_CFG_PATH=$(pwd)/fabric-network/network/configtx

# Copy core.yaml vào configtx dir (cần cho peer binary)
cp $HOME/fabric-samples/config/core.yaml ./fabric-network/network/configtx/

mkdir -p ./fabric-network/network/channel-artifacts

# Genesis block (dùng profile TwoOrgsOrdererGenesis cho orderer bootstrap)
configtxgen \
  -profile TwoOrgsOrdererGenesis \
  -channelID system-channel \
  -outputBlock ./fabric-network/network/channel-artifacts/genesis.block

# Application channel genesis block
configtxgen \
  -profile TwoOrgsApplicationGenesis \
  -outputBlock ./fabric-network/network/channel-artifacts/mychannel.block \
  -channelID mychannel

# Anchor peer updates
configtxgen -profile TwoOrgsChannel \
  -outputAnchorPeersUpdate ./fabric-network/network/channel-artifacts/Org1MSPanchors.tx \
  -channelID mychannel -asOrg Org1MSP

configtxgen -profile TwoOrgsChannel \
  -outputAnchorPeersUpdate ./fabric-network/network/channel-artifacts/Org2MSPanchors.tx \
  -channelID mychannel -asOrg Org2MSP

ls ./fabric-network/network/channel-artifacts/
# genesis.block  mychannel.block  Org1MSPanchors.tx  Org2MSPanchors.tx
```

### Bước 5 — Khởi động Fabric Network + MySQL

```bash
cd /home/ubuntu/identity-fabric

# Khởi động chỉ Fabric nodes và MySQL trước (chưa có Spring Boot)
docker compose -f docker-compose.prod.yaml up -d \
  ca.org1.example.com \
  ca.org2.example.com \
  orderer.example.com \
  peer0.org1.example.com \
  peer1.org1.example.com \
  peer0.org2.example.com \
  peer1.org2.example.com \
  mysql

# Chờ các container ổn định
sleep 10
docker compose -f docker-compose.prod.yaml ps
```

### Bước 6 — Tạo Channel và Join Peers

```bash
cd /home/ubuntu/identity-fabric
export PATH=$PATH:$HOME/fabric-samples/bin
export FABRIC_CFG_PATH=$(pwd)/fabric-network/network/configtx

ROOT_DIR=$(pwd)/fabric-network
ORDERER_CA="${ROOT_DIR}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls/ca.crt"
ORDERER_ADMIN_TLS_CERT="${ROOT_DIR}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls/server.crt"
ORDERER_ADMIN_TLS_KEY="${ROOT_DIR}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls/server.key"

# Orderer join channel (channel participation API — no system channel)
osnadmin channel join \
  --channelID mychannel \
  --config-block "${ROOT_DIR}/network/channel-artifacts/mychannel.block" \
  -o localhost:7053 \
  --ca-file "${ORDERER_CA}" \
  --client-cert "${ORDERER_ADMIN_TLS_CERT}" \
  --client-key "${ORDERER_ADMIN_TLS_KEY}"

sleep 3

# peer0.org1 join
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_TLS_ROOTCERT_FILE="${ROOT_DIR}/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt"
export CORE_PEER_MSPCONFIGPATH="${ROOT_DIR}/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp"
export CORE_PEER_ADDRESS="localhost:7051"
export CORE_PEER_TLS_ENABLED="true"
peer channel join -b "${ROOT_DIR}/network/channel-artifacts/mychannel.block"

# peer0.org2 join
export CORE_PEER_LOCALMSPID="Org2MSP"
export CORE_PEER_TLS_ROOTCERT_FILE="${ROOT_DIR}/organizations/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt"
export CORE_PEER_MSPCONFIGPATH="${ROOT_DIR}/organizations/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp"
export CORE_PEER_ADDRESS="localhost:9051"
peer channel join -b "${ROOT_DIR}/network/channel-artifacts/mychannel.block"

echo "Channel mychannel created and peers joined."
```

### Bước 7 — Build và Deploy Chaincode `identity-ledger`

```bash
cd /home/ubuntu/identity-fabric
export PATH=$PATH:$HOME/fabric-samples/bin
export FABRIC_CFG_PATH=$(pwd)/fabric-network/network/configtx

ROOT_DIR=$(pwd)/fabric-network
CC_SRC_PATH="${ROOT_DIR}/chaincode/asset-transfer"
ORDERER_CA="${ROOT_DIR}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/tls/ca.crt"
ORG1_PEER0_CA="${ROOT_DIR}/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt"
ORG2_PEER0_CA="${ROOT_DIR}/organizations/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt"

# Build Java chaincode
cd "${CC_SRC_PATH}"
mvn clean package -Dmaven.test.skip=true -q
cd /home/ubuntu/identity-fabric

# Package chaincode
peer lifecycle chaincode package "${ROOT_DIR}/identity-ledger.tar.gz" \
  --path "${CC_SRC_PATH}" \
  --lang java \
  --label "identity-ledger_1.0"

# Install trên peer0.org1
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_TLS_ROOTCERT_FILE="${ROOT_DIR}/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt"
export CORE_PEER_MSPCONFIGPATH="${ROOT_DIR}/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp"
export CORE_PEER_ADDRESS="localhost:7051"
export CORE_PEER_TLS_ENABLED="true"
peer lifecycle chaincode install "${ROOT_DIR}/identity-ledger.tar.gz"

# Install trên peer0.org2
export CORE_PEER_LOCALMSPID="Org2MSP"
export CORE_PEER_TLS_ROOTCERT_FILE="${ROOT_DIR}/organizations/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt"
export CORE_PEER_MSPCONFIGPATH="${ROOT_DIR}/organizations/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp"
export CORE_PEER_ADDRESS="localhost:9051"
peer lifecycle chaincode install "${ROOT_DIR}/identity-ledger.tar.gz"

# Lấy Package ID
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_ADDRESS="localhost:7051"
export CORE_PEER_TLS_ROOTCERT_FILE="${ROOT_DIR}/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt"
export CORE_PEER_MSPCONFIGPATH="${ROOT_DIR}/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp"

CC_PACKAGE_ID=$(peer lifecycle chaincode queryinstalled \
  | grep "identity-ledger_1.0" \
  | awk -F 'Package ID: ' '{print $2}' \
  | awk -F ',' '{print $1}')
echo "Package ID: ${CC_PACKAGE_ID}"
export CC_PACKAGE_ID

# Approve cho Org1
peer lifecycle chaincode approveformyorg \
  -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com \
  --channelID mychannel --name identity-ledger --version 1.0 \
  --package-id "${CC_PACKAGE_ID}" --sequence 1 \
  --tls --cafile "${ORDERER_CA}"

# Approve cho Org2
export CORE_PEER_LOCALMSPID="Org2MSP"
export CORE_PEER_ADDRESS="localhost:9051"
export CORE_PEER_TLS_ROOTCERT_FILE="${ROOT_DIR}/organizations/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt"
export CORE_PEER_MSPCONFIGPATH="${ROOT_DIR}/organizations/peerOrganizations/org2.example.com/users/Admin@org2.example.com/msp"

peer lifecycle chaincode approveformyorg \
  -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com \
  --channelID mychannel --name identity-ledger --version 1.0 \
  --package-id "${CC_PACKAGE_ID}" --sequence 1 \
  --tls --cafile "${ORDERER_CA}"

# Commit chaincode (cần cả 2 org endorsement)
peer lifecycle chaincode commit \
  -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com \
  --channelID mychannel --name identity-ledger --version 1.0 --sequence 1 \
  --tls --cafile "${ORDERER_CA}" \
  --peerAddresses localhost:7051 --tlsRootCertFiles "${ORG1_PEER0_CA}" \
  --peerAddresses localhost:9051 --tlsRootCertFiles "${ORG2_PEER0_CA}"

# Verify
peer lifecycle chaincode querycommitted --channelID mychannel --name identity-ledger
```

### Bước 8 — Build và Khởi động Spring Boot

```bash
cd /home/ubuntu/identity-fabric

# Build và start Spring Boot container
docker compose -f docker-compose.prod.yaml up -d --build fabric-api

# Theo dõi log khởi động
docker logs -f fabric-spring-api
# Chờ: "Started ... in X.XXX seconds"
```

### Bước 9 — Kiểm tra hệ thống

```bash
# Health check Spring Boot
curl http://34.70.92.195:8080/actuator/health
# Expect: {"status":"UP"}

# Test API đăng ký
curl -X POST http://34.70.92.195:8080/api/v1/auth/signin \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@mpcorp.com","password":"Admin@SecurePass2026!"}'

# Kiểm tra Swagger UI (từ browser)
# http://34.70.92.195:8080/swagger-ui.html

# Kiểm tra Fabric containers
docker compose -f docker-compose.prod.yaml ps

# Kiểm tra chaincode đã commit
export PATH=$PATH:$HOME/fabric-samples/bin
export FABRIC_CFG_PATH=/home/ubuntu/identity-fabric/fabric-network/network/configtx
export CORE_PEER_LOCALMSPID="Org1MSP"
export CORE_PEER_ADDRESS="localhost:7051"
export CORE_PEER_TLS_ENABLED="true"
export CORE_PEER_TLS_ROOTCERT_FILE="/home/ubuntu/identity-fabric/fabric-network/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt"
export CORE_PEER_MSPCONFIGPATH="/home/ubuntu/identity-fabric/fabric-network/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp"
peer lifecycle chaincode querycommitted --channelID mychannel
```

---

## 6. Cấu hình Firebase RemoteConfig cho Flutter

Sau khi backend chạy ổn định, cập nhật Firebase RemoteConfig để app trỏ đúng IP:

```
Key:   base_url
Value: http://34.70.92.195:8080/api/v1
```

File fallback trong app: `identity_frontend/lib/core/network/api_constants.dart`

---

## 7. Lệnh vận hành thường dùng

```bash
# Xem log tất cả containers
docker compose -f docker-compose.prod.yaml logs -f

# Restart chỉ Spring Boot (sau khi update code)
docker compose -f docker-compose.prod.yaml up -d --build fabric-api

# Dừng toàn bộ (giữ data)
docker compose -f docker-compose.prod.yaml stop

# Xóa toàn bộ + volumes (NGUY HIỂM — mất hết data blockchain)
docker compose -f docker-compose.prod.yaml down --volumes

# Xem disk usage
docker system df

# Xem resource containers
docker stats --no-stream
```

---

## 8. Lưu ý quan trọng

| Vấn đề | Mô tả | Giải pháp |
|---|---|---|
| **Cert paths hard-code** | `application.yml` có paths `/home/phuongdang/...` | Đã override bằng env vars `FABRIC_PEER_TLS_CERT_PATH`, `FABRIC_GATEWAY_CERT_PATH`, `FABRIC_GATEWAY_KEY_PATH` trong docker-compose.prod.yaml |
| **VpSessionStore in-memory** | Không persist qua restart | Chấp nhận cho dev/demo; production cần Redis |
| **ddl-auto=update** | Hibernate tự sửa schema | An toàn cho dev; production dùng Flyway |
| **VC proof HMAC-SHA256** | Không phải chuẩn Ed25519 | Chấp nhận cho POC/research |
| **Fabric peer endpoint** | Backend connect đến `peer0.org1.example.com:7051` qua Docker DNS | Hoạt động vì cùng `fabric_network` |
| **Java chaincode build time** | Maven build chaincode mất 5-10 phút lần đầu | Bình thường — Docker cache sẽ giúp lần sau |
| **e2-standard-4 RAM** | 16GB cần thiết — 4 peers + orderer + Spring Boot + MySQL tốn ~10-12GB | Đủ cho dev/demo topology |
