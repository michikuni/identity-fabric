# CHƯƠNG 1: TỔNG QUAN ĐỀ TÀI


## 1.1. Tính cấp thiết của đề tài

//Tại sao phải nghiên cứu đề tài này?

## 1.2. Mục tiêu nghiên cứu của đề tài

//Nghiên cứu đề tài này giải quyết vấn đề gì?

## 1.3. Đối tượng và phạm vi nghiên cứu

### 1.3.1. Đối tượng nghiên cứu

//Nghiên cứu công nghệ gì? Thực hiện nghiệp vụ nào?

### 1.3.2. Phạm vi nghiên cứu

#### 1.3.2.1. Phạm vi về không gian

//Đề tài này ứng dụng trong không gian nào? Quốc gia hay địa phương? Doanh nghiệp tư nhân hay doanh nghiệp nhà nước?

#### 1.3.2.2. Phạm vi về thời gian

//Đề tài này nghiên cứu công nghệ, dữ liệu trong khoảng thời gian nào?

## 1.4. Khảo sát các nền tảng blockchain hiện có

//Có các loại blockchain nào? Những ứng dụng nào xây dựng với nền tảng blockchain đó nổi bật và có giá trị thực tiễn?

## 1.5. Tổng quan về blockchain

### 1.5.1. Khái niệm và đặc tính của blockchain

//Blockchain là gì? Đặc tính cốt lõi của blockchain là gì?Lợi ích của các đặc tính đó đến thực tiễn?

### 1.5.2. Phân loại blockchain

//Có các loại blockchain nào và chúng khác nhau thế nào?

## 1.6. Tổng quan về Hyperledger Fabric

### 1.6.1. Bối cảnh ra đời

//Hyperledger được tạo ra bởi công ty nào? Với mục đích gì? Sự khác biệt cốt lõi của Hyperledger Fabric

### 1.6.2. Đặc điểm chính của Hyperledger Fabric

//Permissioned Blockchain, Modular Architecture, Privacy and Confidentiality, High Performance and Scalability, Pluggable Consensus Mechanism

### 1.6.3. Kiến trúc tổng thể của Hyperledger Fabric

//Organization, Peer Node, Ordering Service, Channel, Ledger, Chaincode

### 1.6.4. Cơ chế giao dịch

//Mô hình Execute-Order-Validate

### 1.6.5. Cơ chế đồng thuận và sự tiến hóa

//Kafka, Raft, SmartBFT. Hiện tại bản mới nhất của Hyperledger Fabric đang dùng cơ chế đồng thuận nào? Có thể dùng tùy chọn không hay bắt buộc?

### 1.6.6. Ưu điểm, hạn chế và ứng dụng thực tế của HyperLedger Fabric

//Hyperledger Fabric có ưu điểm gì? Hạn chế gì? Ứng dụng thực tế trong đời sống vào việc gì và như thế nào?


# CHƯƠNG 2: PHÂN TÍCH VÀ THIẾT KẾ KIẾN TRÚC



## 2.1. Phân tích làm rõ yêu cầu đối với hệ thống định danh số thế hệ mới trong doanh nghiệp



### 2.1.1. Yêu cầu chức năng


//Hệ thống này có chức năng gì? Tác nhân? Mô tả?


### 2.1.2. Yêu cầu phi chức năng


//Các yêu cầu phi chức năng định nghĩa các thuộc tính chất lượng mà hệ thống phải đáp ứng, được phân loại theo các nhóm tiêu chuẩn ISO 25010


## 2.2. Đề xuất kiến trúc hệ thống



### 2.2.1. Lớp hạ tầng blockchain


//Trình bày cách xây dựng Fabric gateway, peer nodes, orderer nodes, Certificate Authorities. Sử dụng cơ chế đồng thuận gì? Chaincode viết bằng ngôn ngữ gì? Cách triển khai

### 2.2.2. Lớp ứng dụng


#### 2.2.2.1. Giao tiếp giữa ứng dụng với blockchain

//Trình bày cách tương tác giữa backend với fabric gateway

#### 2.2.2.2. Backend

//Trình bày công nghệ sử dụng để xây dựng backend và các thành phần của backend

#### 2.2.2.3. Database

//Trình bày công nghệ database sử dụng và các trường trong database

#### 2.2.2.4. Ứng dụng di động

//Trình bày công nghệ xây dựng ứng dụng di động và các chức năng, thành phần của ứng dụng

#### 2.2.2.5. Website định danh

//Trình bày lý do làm website định danh và công nghệ sử dụng để xây dựng.

## 2.3. Xây dựng các quy trình nghiệp vụ


### 2.3.1. Quy trình cấp phát chứng chỉ số


//Cấp phát VC như thế nào? Các thực thể tham gia có nhiệm vụ gì trong việc cấp phát và sử dụng VC,…


### 2.3.2. Quy trình xác thực và phân quyền truy cập


//Trình bày quy trình xác thực và phân quyền truy cập


### 2.3.3. Quy trình thu hồi danh tính


//Trình bày quy trình thu hồ danh tính


### 2.3.4. Mô hình đồng bộ blockchain bất đồng bộ


//Trình bày cách hệ thống áp dụng mô hình fire-and-forget kết hợp Transactional Outbox


## 2.4. Thiết kế giải pháp bảo mật và quyền riêng tư



### 2.4.1. Quản lý khóa mật mã


//Trình bày cách quản lý khóa mật mã


### 2.4.2. Mã hóa dữ liệu


//Trình bày các phương pháp mã hóa dữ liệu


### 2.4.3. Bảo vệ quyền riêng tư


//Trình bày các phương pháp bảo vệ quyền riêng tư


### 2.4.4. Phòng chống các loại tấn công


//Trình bày cách phòng chống các loại tấn công


### 2.4.5. Tuân thủ pháp lý và tiêu chuẩn


//Trình bày đã tuân thủ pháp lý và các tiêu chuẩn như thế nào


## 2.5. Các cơ chế đặc trưng của ứng dụng



### 2.5.1. Oubox Pattern và Dual-write – Đảm bảo eventual consistency


//Trình bày cơ chế Oubox Pattern và Dual-write trong ứng dụng


### 2.5.2. Selective Disclosure thông qua SD-JWT


//Trình bày cơ chế Selective Disclosure trong ứng dụng


### 2.5.3. Status List 2021 – Cơ chế thu hồi bảo toàn quyền riêng tư


//Trình bày cơ chế thu hồi VC


### 2.5.4. Trust Registry on-chain — Sổ đăng ký nhà phát hành tin cậy


//Trình bày cơ chế Trust Registry on-chain


### 2.5.5. E-sign hợp đồng và non-repudiation thông qua chữ ký Holder


//Trình bày cơ chế E-sign


### 2.5.6. Hỗ trợ GDPR Điều 17 và Điều 20 — Quyền lãng quên và Quyền chuyển dữ liệu


//Trình bày nguyên tắc xây dựng trên GDPR điều 17 và điều 20 – Quyền lãng quên và Quyền chuyển dữ liệu


# CHƯƠNG 3: PHÁT TRIỂN HỆ THỐNG



## 3.1. Biểu đồ Usecase



### 3.1.1. Biểu đồ Usecase tổng quát


//Vẽ biểu đồ usecase tổng quát bằng https://app.diagrams.net/ lưu file .drawio vào google driver


### 3.1.2. Biểu đồ Usecase luồng Admin


//Vẽ biểu đồ usecase luồng Admin bằng https://app.diagrams.net/ lưu file .drawio vào google driver


### 3.1.3. Biểu đồ Usecase luồng Issuer


//Vẽ biểu đồ usecase luồng Issuer bằng https://app.diagrams.net/ lưu file .drawio vào google driver


### 3.1.4. Biểu đồ Usecase luồng Holder


//Vẽ biểu đồ usecase luồng Holder bằng https://app.diagrams.net/ lưu file .drawio vào google driver


### 3.1.5. Biều đồ Usecase luồng Verifier


//Vẽ biểu đồ usecase luồng Verifier bằng https://app.diagrams.net/ lưu file .drawio vào google driver


## 3.2. Biều đồ tuần tự



### 3.2.1. Biểu đồ tuần tự luồng Admin


//Vẽ biểu đồ tuần tự luồng Admin bằng https://app.diagrams.net/ lưu file .drawio vào google driver


### 3.2.2. Biểu đồ tuần tự luồng Issuer


//Vẽ biểu đồ tuần tự luồng Issuer bằng https://app.diagrams.net/ lưu file .drawio vào google driver


### 3.2.3. Biểu đồ tuần tự luồng Holder


//Vẽ biểu đồ tuần tự luồng Holder bằng https://app.diagrams.net/ lưu file .drawio vào google driver


### 3.2.4. Biều đồ tuần tự luồng Verifier


//Vẽ biểu đồ tuần tự luồng Verifier bằng https://app.diagrams.net/ lưu file .drawio vào google driver


## 3.3. Biểu đồ hoạt động



### 3.3.1. Biểu đồ hoạt động quy trình phát hành chứng chỉ có thể xác minh


//Vẽ biểu đồ hoạt động quy trình phát hành chứng chỉ có thể xác minh bằng https://app.diagrams.net/ lưu file .drawio vào google driver


### 3.3.2. Biều đồ hoạt động quy trình xác minh chứng chỉ


//Vẽ biểu đồ hoạt động quy trình xác minh chứng chỉ bằng https://app.diagrams.net/ lưu file .drawio vào google driver


## 3.4. Thiết kế hệ thống



### 3.4.1. Biểu đồ thiết kế tầng 1 – User Layer


//Vẽ biểu đồ thiết kế tầng 1 – User Layer bằng https://app.diagrams.net/ lưu file .drawio vào google driver


### 3.4.2. Biều đồ thiết kế tầng 2 – Gateway Layer


//Vẽ biểu đồ thiết kế tầng 2 – Gateway Layer bằng https://app.diagrams.net/ lưu file .drawio vào google driver


### 3.4.3. Biều đồ thiết kế tầng 3 – Application Layer


//Vẽ biểu đồ thiết kế tầng 3 – Application Layer bằng https://app.diagrams.net/ lưu file .drawio vào google driver


### 3.4.4. Biều đồ thiết kế tầng 4 – Blockchain Layer


//Vẽ biểu đồ thiết kế tầng 4 – Blockchain Layer bằng https://app.diagrams.net/ lưu file .drawio vào google driver


## 3.5. Thiết kế cơ sở dữ liệu


//Vẽ biểu đồ thiết kế cơ sở dữ liệu bằng https://app.diagrams.net/ lưu file .drawio vào google driver


## 3.6. Lập trình module hệ thống


//Trình bày các đoạn code module chính của hệ thống và mô tả


## 3.7. Tương tác với blockchain


//Trình bày cách hệ thống tương tác với blockchain. User tạo dữ liệu như nào? Ghi vào database và ghi vào blockchain như nào? Mô tả cách hoạt động của từng thành phần như peer, CAs, orderer, hoạt động trong context này


# CHƯƠNG 4: KẾT QUẢ HIỆN THỰC HÓA VÀ ĐÁNH GIÁ THỰC NGHIỆM



## 4.1. Kết quả hiện thực hóa


//Hình ảnh thực nghiệm


## 4.2. Đánh giá


//Đo tốc độ blockchain và đánh giá


# KẾT LUẬN


## 1. Các kết quả đạt được

//Trình bày kết  quả đạt được

## 2. Hạn chế của đề tài

//Trình bày hạn chế của đề tài


# TÀI LIỆU THAM KHẢO


//Trình bày các tài liệu tham khảo chính theo chuẩn IEEE như bên dưới

[1]	W3C, “Verifiable Credentials Data Model v2.0,” W3C Recommendation, 2025. [Online]. Available: https://www.w3.org/TR/vc-data-model-2.0/