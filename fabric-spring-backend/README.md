# Fabric Spring Backend

Module này là một project Spring Boot/Kotlin đang ở trạng thái hybrid: nó vừa chứa ứng dụng identity/HR sử dụng MySQL + JWT, vừa còn giữ lại một cụm code prototype kết nối Hyperledger Fabric.

README này mô tả theo code hiện có trong module, không theo README cũ.

## Tổng quan

Trong `src/main/kotlin` hiện có 2 nhóm package lớn:

- `com.mpcorp.identity`
  - Ứng dụng chính cho authentication, employee, profile, contract, payroll
  - Dùng Spring Security, JPA, MySQL, JWT
- `org.fabric.api`
  - prototype API kết nối Fabric Gateway
  - expose CRUD cho chaincode `asset-transfer`

Nói ngắn gọn: Đây không phải chỉ là Fabric wrapper, mà là một backend gồm 2 hướng code đang cùng tồn tại.

## Công nghệ

- Kotlin `2.2.21`
- Spring Boot `4.0.5`
- Spring Web
- Spring Security
- Spring Data JPA
- MySQL Connector/J
- JWT (`jjwt`)
- Gradle Kotlin DSL

## Cấu trúc thư mục chính

```text
fabric-spring-backend/
|-- build.gradle.kts
|-- settings.gradle.kts
|-- Dockerfile
|-- docker-compose.yml
`-- src/
    |-- main/kotlin/
    |   |-- com/mpcorp/identity/
    |   `-- org/fabric/api/
    `-- main/resources/
        |-- application.properties
        `-- application.yml
```

## Phần `com.mpcorp.identity`

Đây là phần backend nghiệp vụ chính khi đọc code hiện tại.

### Kiến trúc mã nguồn

- `presentation`
  - API contract
  - controller
  - request/response
  - mapper
- `application`
  - use case
  - dto
  - mapper
  - support/reference model
- `domain`
  - entity
  - repository abstraction
- `infrastructures`
  - JPA entity
  - JPA repository
  - repository implementation
  - security
  - config
- `common`
  - exception
  - validation
  - constant
  - utility

### Bảo mật

`SecurityConfig` cho phép anonymous với:

- `/api/v1/auth/**`

Tất cả API còn lại yêu cầu JWT Bearer token.

### API chính

#### Auth

- `POST /api/v1/auth/sign-up`
- `POST /api/v1/auth/sign-in`

Ví dụ `sign-up`:

```json
{
  "email": "user@example.com",
  "phone": "0123456789",
  "password": "secret"
}
```

Ví dụ `sign-in`:

```json
{
  "username": "0123456789",
  "password": "secret"
}
```

#### Employee

- `POST /api/v1/employee`
- `GET /api/v1/employee`
- `PUT /api/v1/employee`
- `DELETE /api/v1/employee`

Resource employee được thao tác theo user hiện tại, không dùng ID trên URL.

#### Profile

- `POST /api/v1/profile`
- `GET /api/v1/profile`
- `PUT /api/v1/profile`
- `DELETE /api/v1/profile`

#### Contract

- `POST /api/v1/contracts`
- `GET /api/v1/contracts`
- `PUT /api/v1/contracts`
- `DELETE /api/v1/contracts`

#### Payroll

- `POST /api/v1/payroll`
- `GET /api/v1/payroll`
- `PUT /api/v1/payroll`
- `DELETE /api/v1/payroll`

## Phần `org.fabric.api`

Đây là cụm code prototype để kết nối mạng Fabric ở module `../fabric-network`.

### Thành phần chính

- `FabricApplication.kt`
- `config/FabricGatewayConfig.kt`
- `config/FabricProperties.kt`
- `controller/AssetController.kt`
- `service/AssetService.kt`
- `websocket/*`

### Chức năng

Prototype này gọi chaincode `asset-transfer` trên channel `mychannel` thông qua Fabric Gateway và expose các API:

- `POST /api/v1/assets/init`
- `GET /api/v1/assets`
- `GET /api/v1/assets/{id}`
- `GET /api/v1/assets/{id}/exists`
- `POST /api/v1/assets`
- `PUT /api/v1/assets/{id}`
- `DELETE /api/v1/assets/{id}`
- `PATCH /api/v1/assets/{id}/transfer`

Ngoài REST API, nó còn publish WebSocket event khi có transaction commit.

## Cấu hình

Module này đang có 2 bộ cấu hình song song:

### `application.properties`

Phục vụ identity app:

- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `jwt.secret`
- `jwt.expiration`

Database mặc định:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/identity_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=password
```

### `application.yml`

Phục vụ prototype Fabric:

- `fabric.msp-id`
- `fabric.channel-name`
- `fabric.chaincode-name`
- `fabric.peer.endpoint`
- `fabric.peer.tls-cert-path`
- `fabric.gateway.cert-path`
- `fabric.gateway.key-path`

Mặc định nó đang trỏ tới crypto material trong `../fabric-network/organizations/...`

## Cách chạy local

### Chạy identity app

Cần có MySQL local theo thông số trong `application.properties`, sau đó:

```bash
./gradlew bootRun
```

Nếu dùng Windows:

```bash
gradlew.bat bootRun
```

### Chạy phần Fabric prototype

Trước tiên cần dùng Fabric network:

```bash
cd ../fabric-network
./scripts/network.sh up
./scripts/network.sh createChannel
./scripts/network.sh deployCC
```

Sau đó quay lại backend và đảm bảo các đường dẫn crypto hợp lệ qua biến môi trường hoặc `application.yml`.

## Docker

`docker-compose.yml` của module này đang thiết kế theo hướng:

- attach vào network Docker `fabric_network`
- mount crypto material từ `../fabric-network/organizations/org1`
- set env cho Fabric Gateway

Nó phù hợp hơn với phần `org.fabric.api`.

Lưu ý: File này hiện không định nghĩa MySQL container cho `com.mpcorp.identity`, nên nếu muốn chạy identity app bằng Docker, cần bổ sung database service hoặc dùng MySQL bên ngoài.

## Điểm cần lưu ý khi làm việc với module này

### 1. Có 2 class `@SpringBootApplication`

Trong code hiện có:

- `com.mpcorp.identity.IdentityApplication`
- `org.fabric.api.FabricApplication`

Đây là dấu hiệu cho thấy project đang chứa 2 luồng app trong cùng một module. Trước khi đóng gọi hoặc tách deployment, nên quyết định rõ entry point nào là chính.

### 2. README cũ không còn đúng với hiện trạng code

README cũ mô tả backend như một Fabric wrapper thuần, nhưng code thực tế đã mở rộng thành identity backend có JPA, security và use-case architecture.

### 3. Docker và config hiện đang nghiêng về phần Fabric

Trong khi phần business chính `com.mpcorp.identity` lại cần MySQL. Vì vậy nếu onboard người mới, nên nói rõ backend đang ở trạng thái chuyển tiếp.

## Lệnh hữu ích

Chạy test:

```bash
./gradlew test
```

Build jar:

```bash
./gradlew bootJar
```

## Gợi ý đọc code

Nếu muốn hiểu nhanh phần identity:

- `src/main/kotlin/com/mpcorp/identity/IdentityApplication.kt`
- `src/main/kotlin/com/mpcorp/identity/infrastructures/config/SecurityConfig.kt`
- `src/main/kotlin/com/mpcorp/identity/presentation/controller/AuthController.kt`
- `src/main/kotlin/com/mpcorp/identity/presentation/controller/EmployeeController.kt`
- `src/main/kotlin/com/mpcorp/identity/application/usecase/`

Nếu muốn hiểu nhanh phần Fabric prototype:

- `src/main/kotlin/org/fabric/api/FabricApplication.kt`
- `src/main/kotlin/org/fabric/api/config/FabricGatewayConfig.kt`
- `src/main/kotlin/org/fabric/api/controller/AssetController.kt`
- `src/main/kotlin/org/fabric/api/service/AssetService.kt`
