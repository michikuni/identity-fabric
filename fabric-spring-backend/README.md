# Fabric Spring Boot Backend

A **Spring Boot 3 + Kotlin + Gradle** REST API that wraps the Hyperledger Fabric
`asset-transfer` chaincode, using the official **Fabric Gateway SDK 1.4**.

---

## Prerequisites

| Tool | Version |
|---|---|
| JDK | 17+ |
| Gradle | 8.x (wrapper included) |
| Docker | 24+ |
| Fabric network | Running (`fabric-network/scripts/network.sh up && deployCC`) |

---

## Project Structure

```
fabric-spring-backend/
├── build.gradle.kts                  # Gradle Kotlin DSL build file
├── settings.gradle.kts
├── Dockerfile
├── docker-compose.yml                # Attach to existing Fabric network
└── src/
    ├── main/
    │   ├── kotlin/org/fabric/api/
    │   │   ├── FabricApplication.kt          # Entry point
    │   │   ├── config/
    │   │   │   ├── FabricProperties.kt        # Typed config binding
    │   │   │   ├── FabricGatewayConfig.kt     # Gateway bean setup (gRPC + TLS)
    │   │   │   └── OpenApiConfig.kt           # Swagger metadata
    │   │   ├── controller/
    │   │   │   └── AssetController.kt         # REST endpoints
    │   │   ├── service/
    │   │   │   └── AssetService.kt            # Fabric Gateway calls
    │   │   ├── model/
    │   │   │   └── AssetModels.kt             # DTOs, request/response models
    │   │   └── exception/
    │   │       └── Exceptions.kt              # Domain exceptions + global handler
    │   └── resources/
    │       └── application.yml
    └── test/
        └── kotlin/org/fabric/api/
            └── service/
                └── AssetServiceTest.kt        # Unit tests (MockK)
```

---

## Quick Start

### 1. Start the Fabric network first

```bash
cd ../fabric-network
./scripts/network.sh up
./scripts/network.sh createChannel
./scripts/network.sh deployCC
```

### 2. Configure paths

Edit `src/main/resources/application.yml` or set environment variables:

```bash
export FABRIC_TLS_CERT_PATH=../fabric-network/organizations/org1/peers/peer0/tls/ca.crt
export FABRIC_CERT_PATH=../fabric-network/organizations/org1/users/User1@org1.example.com/msp/signcerts/cert.pem
export FABRIC_KEY_PATH=../fabric-network/organizations/org1/users/User1@org1.example.com/msp/keystore/
```

### 3. Run locally

```bash
./gradlew bootRun
```

### 4. Run with Docker Compose (alongside Fabric network)

```bash
docker-compose -f ../fabric-network/docker-compose.yaml -f docker-compose.yml up -d
```

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/assets/init` | Seed ledger with 6 sample assets |
| `GET` | `/api/v1/assets` | Get all assets |
| `GET` | `/api/v1/assets/{id}` | Get asset by ID |
| `GET` | `/api/v1/assets/{id}/exists` | Check if asset exists |
| `POST` | `/api/v1/assets` | Create new asset |
| `PUT` | `/api/v1/assets/{id}` | Update asset |
| `DELETE` | `/api/v1/assets/{id}` | Delete asset |
| `PATCH` | `/api/v1/assets/{id}/transfer` | Transfer ownership |

### Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---

## Example Requests

```bash
# Initialize ledger
curl -X POST http://localhost:8080/api/v1/assets/init

# Get all assets
curl http://localhost:8080/api/v1/assets

# Create an asset
curl -X POST http://localhost:8080/api/v1/assets \
  -H "Content-Type: application/json" \
  -d '{"id":"asset7","color":"purple","size":10,"owner":"Alice","appraisedValue":900}'

# Transfer an asset
curl -X PATCH http://localhost:8080/api/v1/assets/asset1/transfer \
  -H "Content-Type: application/json" \
  -d '{"newOwner":"Bob"}'

# Delete an asset
curl -X DELETE http://localhost:8080/api/v1/assets/asset7
```

---

## Running Tests

```bash
./gradlew test
```

---

## WebSocket (STOMP)

After every Fabric transaction commits, the backend pushes a real-time event over WebSocket.

### Connect

```javascript
// Using @stomp/stompjs
const client = new Client({ brokerURL: 'ws://localhost:8080/ws/fabric' });
client.activate();
```

### Subscribe to topics

| Topic | Description |
|---|---|
| `/topic/assets` | All asset events (global feed) |
| `/topic/assets/{id}` | Events for a specific asset ID |

### Event payload shape

```json
{
  "type": "ASSET_CREATED",
  "assetId": "asset7",
  "payload": { "ID": "asset7", "Color": "purple", "Size": 10, "Owner": "Alice", "AppraisedValue": 900 },
  "timestamp": "2026-03-29T10:00:00Z"
}
```

### Event types

| Type | Trigger |
|---|---|
| `INIT_LEDGER` | `POST /api/v1/assets/init` |
| `ASSET_CREATED` | `POST /api/v1/assets` |
| `ASSET_UPDATED` | `PUT /api/v1/assets/{id}` |
| `ASSET_DELETED` | `DELETE /api/v1/assets/{id}` |
| `ASSET_TRANSFERRED` | `PATCH /api/v1/assets/{id}/transfer` |

### Quick browser test

```javascript
client.onConnect = () => {
  client.subscribe('/topic/assets', (msg) => {
    console.log(JSON.parse(msg.body));
  });
};
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9 |
| Framework | Spring Boot 3.2 |
| Build | Gradle 8 (Kotlin DSL) |
| Blockchain SDK | Fabric Gateway 1.4 + gRPC/Netty |
| Real-time | Spring WebSocket + STOMP |
| API docs | SpringDoc OpenAPI (Swagger UI) |
| Validation | Jakarta Validation |
| Testing | JUnit 5 + MockK |
