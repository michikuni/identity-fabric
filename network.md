Admin cấp VC — Có liên quan đến CA/MSP, nhưng ở hai tầng khác nhau
Tầng 1: CA/MSP — Xác thực Spring Backend với Fabric
CA và MSP không trực tiếp cấp VC, nhưng chúng là điều kiện tiên quyết:


Admin gọi API → Spring Backend (dùng cert User1@org1 do CA ký)
                → Fabric Gateway xác thực qua MSP (Org1MSP)
                → Ghi DID lên blockchain (RegisterDID chaincode)
Backend dùng X.509 cert của User1@org1.example.com (do CA của Org1 cấp) để được Fabric nhận diện là thành viên hợp lệ của Org1MSP — nếu không có cert này, backend không thể ghi bất kỳ transaction nào lên ledger.

Tầng 2: VC — Admin cấp bằng HMAC, không phải CA cert
Khi Admin approve một employee (AdminController.kt):


Admin approve account
  → registerDID(employeeId, publicKeyJwk)  ← ghi lên Fabric (cần CA/MSP)
  → issueEmploymentVC(employee)             ← tạo VC bằng HMAC-SHA256 (độc lập với CA)
  → lưu VC dưới dạng JSON vào MySQL
VC được ký bằng HMAC-SHA256 (secret key riêng, không phải private key từ CA). CA/MSP không tham gia vào bước ký VC.

Sơ đồ tổng quan

CA (cryptogen)
  └─ cấp cert cho User1@org1
       └─ MSP (Org1MSP) nhận cert → backend được phép ghi lên Fabric
            └─ RegisterDID → lưu publicKeyJwk của employee lên ledger

Admin role (@PreAuthorize)
  └─ issueEmploymentVC() → ký bằng HMAC-SHA256
       └─ lưu VC (JSON) vào MySQL (off-chain)
       └─ employee download VC về Flutter wallet
            └─ khi verify: kiểm tra HMAC signature + expiry
Tóm gọn
CA/MSP	Admin cấp VC
Vai trò	Xác thực backend với Fabric network	Cấp danh tính nghề nghiệp cho employee
Cơ chế ký	X.509 certificate (ECDSA)	HMAC-SHA256
Lưu ở đâu	Fabric ledger (DID document)	MySQL (VC JSON)
Ai kiểm soát	Fabric CA	Spring backend (Admin role)
Liên quan nhau?	CA/MSP là điều kiện để backend ghi DID lên ledger — DID chứa public key dùng để verify VC sau này	
Nói cách khác: CA/MSP bảo vệ tầng Fabric, còn Admin cấp VC ở tầng ứng dụng — hai tầng độc lập nhưng cùng phục vụ một mục tiêu là xác minh danh tính.