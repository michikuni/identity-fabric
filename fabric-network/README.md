# Fabric Network

Module này dùng để dựng mạng Hyperledger Fabric local với 2 organization, 1 orderer, 4 peer và chaincode Java `asset-transfer`.

## Mục tiêu

`fabric-network` gồm 3 phần:

- Hạ tầng Fabric chạt bằng Docker Compose
- Chaincode Java `asset-transfer`
- Một Java client demo sử dụng Fabric Gateway SDK

Toàn bộ luồng khởi tạo mạng được gọi qua `scripts/network.sh`.

## Cấu trúc thư mục

```text
fabric-network/
|-- docker-compose.yaml
|-- scripts/network.sh
|-- network/
|   |-- configtx/configtx.yaml
|   `-- crypto-config/crypto-config.yaml
|-- chaincode/
|   `-- asset-transfer/
`-- application/
```

## Kiến trúc mạng

### Organization và node

- Org1
  - `peer0.org1.example.com`
  - `peer1.org1.example.com`
  - `ca.org1.example.com`
- Org2
  - `peer0.org2.example.com`
  - `peer1.org2.example.com`
  - `ca.org2.example.com`
- Orderer org
  - `orderer.example.com`

### Port map

| Container | Port | Vai trò |
|---|---:|---|
| `ca.org1.example.com` | `7054` | CA cho Org1 |
| `ca.org2.example.com` | `8054` | CA cho Org2 |
| `orderer.example.com` | `7050` | Orderer |
| `orderer.example.com` | `7053` | Orderer admin |
| `peer0.org1.example.com` | `7051` | Peer 0 Org1 |
| `peer1.org1.example.com` | `8051` | Peer 1 Org1 |
| `peer0.org2.example.com` | `9051` | Peer 0 Org2 |
| `peer1.org2.example.com` | `10051` | Peer 1 Org2 |

### Channel và chaincode

- Channel mặc định: `mychannel`
- Chaincode mặc định: `asset-transfer`
- Runtime chaincode: Java

## Yêu cầu môi trường

Cần cài sẵn:

- Docker
- Docker Compose
- Java 11+
- Maven
- Hyperledger Fabric binaries:
  - `cryptogen`
  - `configtxgen`
  - `peer`
  - `osnadmin`

Nếu bạn chưa có Fabric binaries, có thể tải theo hướng quen thuộc:

```bash
curl -sSL https://bit.ly/2ysbOFE | bash -s -- 2.5.4 1.5.7
```

Sau đó cần đảm bảo các binary nằm trong `PATH` hoặc trong thư mục `bin/` của module này.

Script `network.sh` cũng cần `core.yaml`. Nếu file này chưa tồn tại trong `network/configtx`, script sẽ tìm ở `config/core.yaml`.

## Cách chạy nhanh

### 1. Dựng mạng

```bash
./scripts/network.sh up
```

Lệnh này sẽ:

- Kiểm tra prerequisite
- Tạo crypto material bằng `cryptogen`
- Tạo genesis block và channel artifacts bằng `configtxgen`
- Dựng container bằng `docker-compose`

### 2. Tạo channel

```bash
./scripts/network.sh createChannel
```

Lệnh này sẽ:

- Tạo channel `mychannel`
- Cho orderer join channel
- Cho `peer0.org1` và `peer0.org2` join channel

### 3. Deploy chaincode

```bash
./scripts/network.sh deployCC
```

Lệnh này sẽ:

- build chaincode Java bằng Maven
- package chaincode thành `asset-transfer.tar.gz`
- install lên peer của Org1 và Org2
- approve chaincode cho từng org
- commit chaincode lên `mychannel`
- invoke `InitLedger`

### 4. Hạ mạng

```bash
./scripts/network.sh down
```

Lệnh này sẽ:

- stop và remove container
- xoá volume liên quan
- xoá `organizations/`
- xoá `network/channel-artifacts/`

## Chaincode `asset-transfer`

Chaincode nằm ở `chaincode/asset-transfer` và được viết bằng Java với `fabric-chaincode-shim`.

### Model dữ liệu

Asset gồm các thuộc tính:

- `assetId`
- `color`
- `size`
- `owner`
- `appraisedValue`

### Contract API

| Function | Loại | Mô tả |
|---|---|---|
| `InitLedger` | Submit | Seed 6 asset mẫu vao ledger |
| `CreateAsset` | Submit | Tạo asset mới |
| `ReadAsset` | Evaluate | Đọc 1 asset theo ID |
| `UpdateAsset` | Submit | Ghi đè toàn bộ thông tin asset |
| `DeleteAsset` | Submit | Xóa asset khỏi world state |
| `TransferAsset` | Submit | Chuyển owner và tra owner cũ |
| `AssetExists` | Evaluate | Kiểm tra asset có tồn tại |
| `GetAllAssets` | Evaluate | Lấy toàn bộ asset |

### Event chaincode

Chaincode phát event khi:

- Tạo asset: `CreateAsset`
- Cập nhật asset: `UpdateAsset`
- Xóa asset: `DeleteAsset`
- Chuyển owner: `TransferAsset`

## Java client demo

Thư mục `application/` là một client Java nhỏ sử dụng Fabric Gateway SDK.

Luồng demo trong `App.java`:

- `InitLedger`
- `GetAllAssets`
- `CreateAsset`
- `TransferAsset`
- `GetAllAssets`

Chạy demo:

```bash
cd application
mvn clean package
java -jar target/app.jar
```

Client đang kết nối:

- `peer0.org1.example.com:7051`
- MSP: `Org1MSP`
- user: `User1@org1.example.com`

## Lệnh kiểm thử

### Test chaincode

```bash
cd chaincode/asset-transfer
mvn test
```

### Query bằng CLI container

```bash
docker exec -it cli bash
```

Ví dụ query tất cả asset:

```bash
peer chaincode query -C mychannel -n asset-transfer \
  -c '{"function":"GetAllAssets","Args":[]}'
```

## Ghi chú thực tế

- Network này phù hợp cho local development/demo, chưa phải production topology.
- Docker Compose đang dùng single orderer.
- Script `deployCC` đang install và commit chaincode trên `peer0` của mỗi org.
- Policy thực tế phụ thuộc vào `configtx.yaml`, README này chỉ mô tả luồng chạy theo code hiện có.
