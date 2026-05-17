package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.common.response.ApiResponse
import org.fabric.api.service.IdentityLedgerService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

/**
 * AuditLogController — on-chain audit trail viewer for Admin/Chief.
 *
 * Tận dụng GetRecordHistory (chaincode) đã có — không cần chaincode mới.
 *
 * Endpoints:
 *   GET /api/v1/audit/employees/{employeeId}/history          — all record types
 *   GET /api/v1/audit/employees/{employeeId}/history/{type}   — PROFILE | CONTRACT | PAYROLL | ATTENDANCE | DID
 *   GET /api/v1/audit/records                                  — GetAllRecords (full ledger snapshot)
 *   GET /api/v1/audit/records/{type}/{employeeId}              — GetRecord for a specific employee+type
 */
@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
class AuditLogController(
    private val ledgerService: IdentityLedgerService,
) {
    companion object {
        private val VALID_TYPES = setOf("PROFILE", "CONTRACT", "PAYROLL", "ATTENDANCE", "REQUEST", "DID", "STATUS_LIST", "CONTRACT_SIGNATURE", "COMPANY")
    }

    /**
     * Full history of a single (employeeId, recordType) combination.
     * Returns list ordered oldest → newest (Fabric history API order).
     * Demo storyline: "Admin xem ai làm gì lúc nào với record của nhân viên."
     */
    @GetMapping("/employees/{employeeId}/history/{type}")
    fun getRecordHistory(
        @PathVariable employeeId: String,
        @PathVariable type: String,
    ): ApiResponse<Any> {
        val recordType = type.uppercase()
        if (recordType !in VALID_TYPES) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid record type: $type. Valid: $VALID_TYPES")
        }
        val history = runCatching {
            ledgerService.getRecordHistory(recordType, employeeId)
        }.getOrElse { ex ->
            if (ex.message?.contains("does not exist") == true || ex.message?.contains("not found") == true) {
                return ApiResponse(status = "200", message = "No history found", data = emptyList<Any>())
            }
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fabric error: ${ex.message}")
        }
        return ApiResponse(
            status  = "200",
            message = "Record history (${history.size} entries)",
            data    = history,
        )
    }

    /**
     * All record types for a given employee — aggregates across PROFILE, CONTRACT, PAYROLL.
     * Calls GetAllRecordsByEmployee chaincode function.
     */
    @GetMapping("/employees/{employeeId}/history")
    fun getAllRecordsByEmployee(@PathVariable employeeId: String): ApiResponse<Any> {
        val records = runCatching {
            ledgerService.getAllRecordsByEmployee(employeeId)
        }.getOrElse { ex ->
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fabric error: ${ex.message}")
        }
        return ApiResponse(
            status  = "200",
            message = "All records for employee $employeeId (${records.size} found)",
            data    = records,
        )
    }

    /**
     * Full ledger snapshot — all records across all employees.
     * Returns raw list from GetAllRecords chaincode function.
     * Warning: this is O(all records); suitable for demo / small datasets.
     */
    @GetMapping("/records")
    fun getAllRecords(): ApiResponse<Any> {
        val records = runCatching {
            ledgerService.getAllRecords()
        }.getOrElse { ex ->
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fabric error: ${ex.message}")
        }
        return ApiResponse(
            status  = "200",
            message = "Full ledger snapshot (${records.size} records)",
            data    = records,
        )
    }

    /**
     * Read a single current record by type + employeeId.
     * Does NOT return history — use /history/{type} for that.
     */
    @GetMapping("/records/{type}/{employeeId}")
    fun getRecord(
        @PathVariable type: String,
        @PathVariable employeeId: String,
    ): ApiResponse<Any> {
        val recordType = type.uppercase()
        if (recordType !in VALID_TYPES) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid record type")
        }
        val record = runCatching {
            ledgerService.getRecord(recordType, employeeId)
        }.getOrElse { ex ->
            if (ex.message?.contains("does not exist") == true || ex.message?.contains("not found") == true) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found: $recordType/$employeeId")
            }
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fabric error: ${ex.message}")
        }
        return ApiResponse(status = "200", message = "OK", data = record)
    }
}
