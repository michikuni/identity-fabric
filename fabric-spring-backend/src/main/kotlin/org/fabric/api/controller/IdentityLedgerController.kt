package org.fabric.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.fabric.api.model.*
import org.fabric.api.service.IdentityLedgerService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST API để tương tác với chaincode `identity-ledger` trên Hyperledger Fabric.
 *
 * Base path: /api/v1/ledger
 *
 * Tất cả endpoints đều PUBLIC (FabricSecurityConfig mở toàn bộ).
 * Trong production: thêm API-key filter hoặc mTLS.
 *
 * Endpoints:
 *   POST   /api/v1/ledger/init                          → InitLedger
 *   POST   /api/v1/ledger/records                       → UpsertRecord (CREATE/UPDATE)
 *   DELETE /api/v1/ledger/records/{employeeId}/{type}   → DeleteRecord
 *   GET    /api/v1/ledger/records/{employeeId}/{type}   → GetRecord
 *   GET    /api/v1/ledger/records/{employeeId}/{type}/exists → RecordExists
 *   GET    /api/v1/ledger/records/{employeeId}          → GetAllRecordsByEmployee
 *   GET    /api/v1/ledger/records                       → GetAllRecords
 *   GET    /api/v1/ledger/records/{employeeId}/{type}/history → GetRecordHistory
 */
@RestController
@RequestMapping("/api/v1/ledger")
@Tag(name = "Identity Ledger", description = "Ghi và truy vấn identity audit records trên Hyperledger Fabric")
class IdentityLedgerController(
    private val ledgerService: IdentityLedgerService,
) {

    // ── Init ──────────────────────────────────────────────────────────────────

    @PostMapping("/init")
    @Operation(
        summary = "Khởi tạo ledger",
        description = "Gọi InitLedger trên chaincode. Chỉ cần gọi một lần khi bootstrap.",
    )
    fun initLedger(): ResponseEntity<ApiResponse<Nothing>> {
        ledgerService.initLedger()
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Ledger initialized")
        )
    }

    // ── Upsert (Create / Update) ──────────────────────────────────────────────

    @PostMapping("/records")
    @Operation(
        summary = "Ghi IdentityRecord lên ledger",
        description = """
            Nhận dữ liệu từ com.mpcorp.identity sau khi đã lưu vào MySQL, 
            rồi ghi một audit record lên blockchain.
            
            Dữ liệu nhạy cảm (PII) NẾU KHÔNG được ghi lên chain — 
            chỉ ghi keyFields (summary) + dataHash (SHA-256 proof).
            
            action = CREATE → lần đầu tạo record
            action = UPDATE → cập nhật
            action = DELETE → đánh dấu xóa (soft delete trên chain)
        """,
    )
    fun upsertRecord(
        @Valid @RequestBody request: UpsertIdentityRecordRequest,
    ): ResponseEntity<ApiResponse<IdentityRecord>> {
        val record = ledgerService.upsertRecord(request)
        val status = if (request.action.uppercase() == "CREATE") HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(
            ApiResponse(success = true, message = "Record written to ledger", data = record)
        )
    }

    // ── Delete (soft) ─────────────────────────────────────────────────────────

    @DeleteMapping("/records/{employeeId}/{recordType}")
    @Operation(
        summary = "Xóa logic một record",
        description = "Đặt status = DELETED, không xóa vật lý. Audit trail vẫn giữ nguyên.",
    )
    fun deleteRecord(
        @Parameter(description = "Employee ID", example = "123") @PathVariable employeeId: String,
        @Parameter(description = "PROFILE | CONTRACT | PAYROLL") @PathVariable recordType: String,
        @RequestParam(defaultValue = "system") updatedBy: String,
    ): ResponseEntity<ApiResponse<IdentityRecord>> {
        val record = ledgerService.deleteRecord(employeeId, recordType, updatedBy)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Record marked as DELETED", data = record)
        )
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @GetMapping("/records/{employeeId}/{recordType}")
    @Operation(
        summary = "Đọc một IdentityRecord",
        description = "Query world state theo employeeId + recordType. Không tạo transaction.",
    )
    fun getRecord(
        @PathVariable employeeId: String,
        @PathVariable recordType: String,
    ): ResponseEntity<ApiResponse<IdentityRecord>> {
        val record = ledgerService.getRecord(recordType, employeeId)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Record found", data = record)
        )
    }

    @GetMapping("/records/{employeeId}/{recordType}/exists")
    @Operation(summary = "Kiểm tra record có tồn tại không")
    fun recordExists(
        @PathVariable employeeId: String,
        @PathVariable recordType: String,
    ): ResponseEntity<ApiResponse<Boolean>> {
        val exists = ledgerService.recordExists(recordType, employeeId)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = if (exists) "Record exists" else "Record not found", data = exists)
        )
    }

    @GetMapping("/records/{employeeId}")
    @Operation(
        summary = "Lấy tất cả records của một employee",
        description = "Trả về PROFILE + CONTRACT + PAYROLL records (nếu có) của employee.",
    )
    fun getRecordsByEmployee(
        @PathVariable employeeId: String,
    ): ResponseEntity<ApiResponse<List<IdentityRecord>>> {
        val records = ledgerService.getAllRecordsByEmployee(employeeId)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Found ${records.size} record(s) for employee $employeeId", data = records)
        )
    }

    @GetMapping("/records")
    @Operation(
        summary = "Lấy toàn bộ records (admin/audit)",
        description = "Trả về tất cả IdentityRecord hiện có trên ledger. Dùng cho audit.",
    )
    fun getAllRecords(): ResponseEntity<ApiResponse<List<IdentityRecord>>> {
        val records = ledgerService.getAllRecords()
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Found ${records.size} record(s)", data = records)
        )
    }

    @GetMapping("/records/{employeeId}/{recordType}/history")
    @Operation(
        summary = "Lấy lịch sử thay đổi của một record",
        description = "Trả về toàn bộ audit trail theo thứ tự thời gian.",
    )
    fun getRecordHistory(
        @PathVariable employeeId: String,
        @PathVariable recordType: String,
    ): ResponseEntity<ApiResponse<List<IdentityRecord>>> {
        val history = ledgerService.getRecordHistory(recordType, employeeId)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Found ${history.size} history entry(ies)", data = history)
        )
    }

    // ── Verify hash integrity ──────────────────────────────────────────────────

    @GetMapping("/records/{employeeId}/{recordType}/verify")
    @Operation(
        summary = "Xác thực tính toàn vẹn dữ liệu (Hybrid integrity check)",
        description = """
            So sánh hash của dữ liệu off-chain (MySQL) với dataHash bất biến đã lưu on-chain.

            Cách dùng:
              1. Lấy bản ghi hiện tại từ MySQL
              2. Serialize thành JSON (cùng định dạng khi ghi lần đầu)
              3. Tính SHA-256 của JSON đó
              4. Gọi endpoint này với hash vừa tính

            Kết quả valid=true → dữ liệu không bị thay đổi kể từ lần ghi lên chain cuối cùng.
            Kết quả valid=false → phát hiện sai lệch, cần điều tra.
        """,
    )
    fun verifyRecord(
        @Parameter(description = "Employee ID", example = "123") @PathVariable employeeId: String,
        @Parameter(description = "PROFILE | CONTRACT | PAYROLL") @PathVariable recordType: String,
        @Parameter(
            description = "SHA-256 hex của full JSON record hiện tại trong MySQL",
            example = "a3f1b9c2...",
            required = true,
        ) @RequestParam hash: String,
    ): ResponseEntity<ApiResponse<VerifyRecordResponse>> {
        val result = ledgerService.verifyRecord(recordType.uppercase(), employeeId, hash)
        val message = if (result.valid)
            "Integrity verified — off-chain data matches on-chain hash"
        else
            "Integrity FAILED — hash mismatch detected, investigate tampering"
        return ResponseEntity.ok(ApiResponse(success = result.valid, message = message, data = result))
    }
}