# Identity Fabric

Repo nay gom 2 phan chinh:

- `fabric-network`: mang Hyperledger Fabric 2 org dung de chay chaincode `asset-transfer`.
- `fabric-spring-backend`: backend Spring Boot/Kotlin cho bai toan identity nhan su, dung JWT + MySQL. Module nay dong thoi van con giu lai mot cum code Fabric API cu trong package `org.fabric.api`.

README nay duoc viet lai dua tren code hien co trong repo, khong dua theo mo ta cu.

## Tong quan kien truc

### 1. `fabric-network`

Module nay dung de dung mot mang Fabric local phuc vu development:

- 2 Certificate Authority: `ca.org1.example.com`, `ca.org2.example.com`
- 1 orderer: `orderer.example.com`
- 4 peer:
  - `peer0.org1.example.com`
  - `peer1.org1.example.com`
  - `peer0.org2.example.com`
  - `peer1.org2.example.com`
- 1 channel: `mychannel`
- 1 chaincode Java: `asset-transfer`

Chaincode `asset-transfer` ho tro:

- `InitLedger`
- `CreateAsset`
- `ReadAsset`
- `UpdateAsset`
- `DeleteAsset`
- `TransferAsset`
- `AssetExists`
- `GetAllAssets`

Chaincode co phat event khi tao, cap nhat, xoa va chuyen chu so huu asset.

### 2. `fabric-spring-backend`

Module backend hien tai khong phai chi la wrapper cho Fabric. Khi doc code, co 2 nhom chuc nang song song:

- `com.mpcorp.identity`: ung dung chinh dang duoc cau truc theo domain/use case/repository, quan ly:
  - authentication
  - employee
  - profile
  - contract
  - payroll
- `org.fabric.api`: mot cum code prototype de ket noi Fabric Gateway va expose API CRUD cho `asset-transfer`.

Noi cach khac: backend nay dang chua ca **identity service** va **Fabric API prototype** trong cung mot project.

## Cau truc thu muc

```text
identity-fabric/
|-- fabric-network/
|   |-- docker-compose.yaml
|   |-- scripts/network.sh
|   |-- network/configtx/
|   |-- network/crypto-config/
|   |-- chaincode/asset-transfer/
|   `-- application/
`-- fabric-spring-backend/
    |-- build.gradle.kts
    |-- Dockerfile
    |-- docker-compose.yml
    `-- src/main/kotlin/
        |-- com/mpcorp/identity/
        `-- org/fabric/api/
```

## Phan tich theo module

### `fabric-network`

#### Thanh phan chinh

- `scripts/network.sh`: script quan ly toan bo vong doi mang.
- `docker-compose.yaml`: dinh nghia CA, orderer, peer va CLI.
- `network/configtx/configtx.yaml`: channel profile, MSP, policy.
- `network/crypto-config/crypto-config.yaml`: topo sinh crypto bang `cryptogen`.
- `chaincode/asset-transfer`: chaincode Java dong goi bang Maven Shade.
- `application`: Java client su dung Fabric Gateway SDK.

#### Luong chay

Script `network.sh` ho tro 4 lenh:

- `up`: kiem tra prerequisite, tao crypto material, tao genesis/channel artifacts, sau do `docker-compose up -d`
- `createChannel`: tao `mychannel`, cho orderer join, roi join peer cua 2 org vao channel
- `deployCC`: build chaincode Java, package, install, approve cho tung org, commit len channel va goi `InitLedger`
- `down`: ha mang, xoa volume/container chaincode va xoa `organizations/`, `network/channel-artifacts/`

#### Yeu cau moi truong

Can co:

- Docker
- Docker Compose
- `cryptogen`
- `configtxgen`
- `peer`
- `osnadmin`
- Java 11+
- Maven

Script cung yeu cau `core.yaml` ton tai de peer CLI chay duoc. Neu chua co, script se tim o `fabric-network/config/core.yaml`.

#### Chaincode Java

Chaincode o `fabric-network/chaincode/asset-transfer`:

- model `Asset` gom: `assetId`, `color`, `size`, `owner`, `appraisedValue`
- contract `AssetTransfer` dung `Genson` de serialize JSON
- build ra fat jar `chaincode.jar` thong qua Maven Shade

#### Java client demo

Thu muc `fabric-network/application` la mot client Java nho:

- ket noi `peer0.org1` qua Fabric Gateway
- dung cert/key cua `User1@org1.example.com`
- chay demo workflow:
  - `InitLedger`
  - `GetAllAssets`
  - `CreateAsset`
  - `TransferAsset`
  - `GetAllAssets`

### `fabric-spring-backend`

#### Cong nghe

- Kotlin 2.2.21
- Spring Boot 4.0.5
- Spring Web
- Spring Security
- Spring Data JPA
- MySQL
- JWT (`jjwt`)
- Gradle Kotlin DSL

#### Khoi chuc nang chinh dang su dung

Package `com.mpcorp.identity` duoc to chuc thanh cac lop:

- `presentation`: controller, request/response, api contract
- `application`: use case, dto, mapper, support
- `domain`: entity, repository abstraction
- `infrastructures`: JPA repository, mapper, security, config
- `common`: exception, constant, validation, util

#### Bao mat

`SecurityConfig` cho phep anonymous voi:

- `/api/v1/auth/**`

Tat ca endpoint con lai yeu cau JWT Bearer token.

#### Cau hinh backend

Backend dang dung:

- `application.properties` cho JWT + MySQL
- `application.yml` cho config Fabric prototype (`fabric.msp-id`, `channel-name`, `chaincode-name`, `peer.endpoint`, ...)

Dieu nay cho thay project hien dang co 2 huong cau hinh cung ton tai.

## API chinh cua `com.mpcorp.identity`

### Auth

- `POST /api/v1/auth/sign-up`
- `POST /api/v1/auth/sign-in`

Payload `sign-up`:

```json
{
  "email": "user@example.com",
  "phone": "0123456789",
  "password": "secret"
}
```

Payload `sign-in`:

```json
{
  "username": "0123456789",
  "password": "secret"
}
```

### Employee

- `POST /api/v1/employee`
- `GET /api/v1/employee`
- `PUT /api/v1/employee`
- `DELETE /api/v1/employee`

Employee duoc thao tac theo user hien tai lay tu JWT, khong truyen `id` tren URL.

### Profile

- `POST /api/v1/profile`
- `GET /api/v1/profile`
- `PUT /api/v1/profile`
- `DELETE /api/v1/profile`

Profile gan voi employee hien tai.

### Contract

- `POST /api/v1/contracts`
- `GET /api/v1/contracts`
- `PUT /api/v1/contracts`
- `DELETE /api/v1/contracts`

### Payroll

- `POST /api/v1/payroll`
- `GET /api/v1/payroll`
- `PUT /api/v1/payroll`
- `DELETE /api/v1/payroll`

## Fabric API prototype trong backend

Ngoai identity API, project con chua package `org.fabric.api`:

- `FabricApplication.kt`: them mot `@SpringBootApplication` rieng
- `AssetController.kt`: expose API `/api/v1/assets`
- `AssetService.kt`: goi Fabric Gateway den chaincode `asset-transfer`
- `FabricGatewayConfig.kt`, `FabricProperties.kt`: bean/cau hinh cho Fabric

API prototype nay ho tro:

- `POST /api/v1/assets/init`
- `GET /api/v1/assets`
- `GET /api/v1/assets/{id}`
- `GET /api/v1/assets/{id}/exists`
- `POST /api/v1/assets`
- `PUT /api/v1/assets/{id}`
- `DELETE /api/v1/assets/{id}`
- `PATCH /api/v1/assets/{id}/transfer`

Luu y: do project dang co **2 class `@SpringBootApplication`** (`IdentityApplication` va `FabricApplication`), can kiem tra lai y do thiet ke truoc khi dong goi/chay production.

## Cach chay de tham khao

### 1. Chay Fabric network

Tu thu muc `fabric-network`:

```bash
./scripts/network.sh up
./scripts/network.sh createChannel
./scripts/network.sh deployCC
```

Neu muon dung Java client demo:

```bash
cd application
mvn clean package
java -jar target/app.jar
```

Tat mang:

```bash
./scripts/network.sh down
```

### 2. Chay backend identity

Backend can MySQL local theo `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/identity_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=password
```

Chay local:

```bash
cd fabric-spring-backend
./gradlew bootRun
```

Hoac tren Windows:

```bash
gradlew.bat bootRun
```

### 3. Docker cho backend

`fabric-spring-backend/docker-compose.yml` dang duoc viet theo huong attach vao `fabric_network` va mount crypto material tu `../fabric-network/organizations/org1`.

No phu hop hon cho phan `org.fabric.api` ket noi Fabric, nhung chua dinh nghia MySQL service cho `com.mpcorp.identity`.

## Nhan xet quan trong sau khi doc code

Repo hien tai co tinh chat "hybrid":

- `fabric-network` da ro rang, tuong doi tu hoan chinh cho bai toan demo asset-transfer tren Hyperledger Fabric.
- `fabric-spring-backend` dang chua hai huong phat trien:
  - ung dung identity/HR su dung JPA + MySQL
  - prototype ket noi Hyperledger Fabric

Neu team muon repo de onboarding de hon, nen tach ro:

- backend identity thanh 1 service rieng
- Fabric API prototype thanh 1 service rieng

Hoac toi thieu can chuan hoa lai:

- class entry point
- file config
- Docker Compose
- README rieng cho tung module

## File nen doc dau tien

- `fabric-network/scripts/network.sh`
- `fabric-network/docker-compose.yaml`
- `fabric-network/chaincode/asset-transfer/src/main/java/org/hyperledger/fabric/samples/AssetTransfer.java`
- `fabric-spring-backend/build.gradle.kts`
- `fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/AuthController.kt`
- `fabric-spring-backend/src/main/kotlin/com/mpcorp/identity/presentation/controller/EmployeeController.kt`
- `fabric-spring-backend/src/main/kotlin/org/fabric/api/controller/AssetController.kt`

