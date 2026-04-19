package com.mpcorp.identity.infrastructures.fabric

import com.fasterxml.jackson.databind.ObjectMapper
import com.mpcorp.identity.domain.entity.ContractEntity
import com.mpcorp.identity.domain.entity.PayrollEntity
import com.mpcorp.identity.domain.entity.ProfileEntity
import org.fabric.api.model.RegisterDIDRequest
import org.fabric.api.model.RevokeDIDRequest
import org.fabric.api.model.UpsertIdentityRecordRequest
import org.fabric.api.service.IdentityLedgerService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.security.MessageDigest

/**
 * FabricLedgerBridge — cầu nối giữa com.mpcorp.identity và org.fabric.api.
 *
 * ## Chiến lược: Fire-and-Forget + Outbox Retry
 *
 * MySQL là source of truth — luôn commit trước, không bao giờ rollback vì Fabric.
 *
 * Luồng ghi blockchain:
 *   1. UseCase (CreateProfileUseCase, ...) lưu vào MySQL → commit
 *   2. UseCase gọi FabricLedgerBridge async (@Async)
 *   3. Bridge tính SHA-256 hash + tạo partial snapshot (keyFields)
 *   4. Gọi [IdentityLedgerService.upsertRecord] → submit transaction lên Fabric
 *   5a. Thành công → log info, kết thúc
 *   5b. Thất bại  → [FabricOutboxService.enqueue] lưu vào bảng fabric_outbox_events
 *   6. [FabricRetryScheduler] định kỳ retry với exponential backoff
 *   7. Sau MAX_RETRIES lần thất bại → chuyển DEAD_LETTER, cần xử lý thủ công
 *
 * ## Hash Strategy (SHA-256)
 *
 * fullJson = serialize toàn bộ entity → sha256(fullJson) = dataHash
 * dataHash được lưu on-chain, fullJson KHÔNG được gửi lên chain.
 * Để verify: tính lại sha256 của MySQL record, so sánh với on-chain dataHash.
 *
 * ## keyFields (Partial Snapshot)
 *
 * Chỉ các trường không nhạy cảm, không có PII (số CMND, số tài khoản, lương...).
 * Dùng cho audit trail và truy vết nhanh trên chain mà không cần đọc MySQL.
 */
@Service
class FabricLedgerBridge(
    private val ledgerService: IdentityLedgerService,
    private val outboxService: FabricOutboxService,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(FabricLedgerBridge::class.java)

    // ── Profile ───────────────────────────────────────────────────────────────

    @Async
    fun upsertProfileRecord(profile: ProfileEntity, action: String = "CREATE") {
        val employeeId = profile.employee.id?.toString() ?: run {
            log.warn("[FabricBridge] ProfileEntity missing employeeId, skip")
            return
        }
        val keyFields = objectMapper.writeValueAsString(
            mapOf(
                "name"           to profile.name,
                "gender"         to profile.gender,
                "educationLevel" to profile.educationLevel,
                "major"          to profile.major,
                "expYears"       to profile.expYears,
                "email"          to profile.email,
            )
        )
        val fullJson = objectMapper.writeValueAsString(profile)
        val dataHash = sha256(fullJson)
        val status   = if (action == "DELETE") "DELETED" else "ACTIVE"

        runCatching {
            ledgerService.upsertRecord(
                UpsertIdentityRecordRequest(
                    employeeId = employeeId,
                    recordType = "PROFILE",
                    status     = status,
                    keyFields  = keyFields,
                    dataHash   = dataHash,
                    action     = action,
                    updatedBy  = "system",
                )
            )
            log.info("[FabricBridge] PROFILE written — employeeId=$employeeId action=$action")
        }.onFailure { ex ->
            log.warn("[FabricBridge] PROFILE write failed, enqueuing for retry — employeeId=$employeeId error=${ex.message}")
            outboxService.enqueue(
                employeeId   = employeeId,
                recordType   = "PROFILE",
                recordStatus = status,
                keyFields    = keyFields,
                dataHash     = dataHash,
                action       = action,
                updatedBy    = "system",
                error        = ex.message ?: "unknown",
            )
        }
    }

    @Async
    fun deleteProfileRecord(employeeId: String) {
        runCatching {
            ledgerService.deleteRecord(employeeId, "PROFILE", updatedBy = "system")
            log.info("[FabricBridge] PROFILE DELETE written — employeeId=$employeeId")
        }.onFailure { ex ->
            log.warn("[FabricBridge] PROFILE DELETE failed — employeeId=$employeeId error=${ex.message}")
            // DeleteRecord dùng chaincode DeleteRecord (soft delete) — cần enqueue với action DELETE
            outboxService.enqueue(
                employeeId   = employeeId,
                recordType   = "PROFILE",
                recordStatus = "DELETED",
                keyFields    = "{}",
                dataHash     = "",
                action       = "DELETE",
                updatedBy    = "system",
                error        = ex.message ?: "unknown",
            )
        }
    }

    // ── Contract ──────────────────────────────────────────────────────────────

    @Async
    fun upsertContractRecord(contract: ContractEntity, action: String = "CREATE") {
        val employeeId = contract.employee.id?.toString() ?: run {
            log.warn("[FabricBridge] ContractEntity missing employeeId, skip")
            return
        }
        val keyFields = objectMapper.writeValueAsString(
            mapOf(
                "typeContract"   to contract.typeContract,
                "startDate"      to contract.startDate?.toString(),
                "endDate"        to contract.endDate?.toString(),
                "contractExpire" to contract.contractExpire?.toString(),
            )
        )
        val fullJson = objectMapper.writeValueAsString(contract)
        val dataHash = sha256(fullJson)
        val status   = if (action == "DELETE") "DELETED" else "ACTIVE"

        runCatching {
            ledgerService.upsertRecord(
                UpsertIdentityRecordRequest(
                    employeeId = employeeId,
                    recordType = "CONTRACT",
                    status     = status,
                    keyFields  = keyFields,
                    dataHash   = dataHash,
                    action     = action,
                    updatedBy  = "system",
                )
            )
            log.info("[FabricBridge] CONTRACT written — employeeId=$employeeId action=$action")
        }.onFailure { ex ->
            log.warn("[FabricBridge] CONTRACT write failed, enqueuing for retry — employeeId=$employeeId error=${ex.message}")
            outboxService.enqueue(
                employeeId   = employeeId,
                recordType   = "CONTRACT",
                recordStatus = status,
                keyFields    = keyFields,
                dataHash     = dataHash,
                action       = action,
                updatedBy    = "system",
                error        = ex.message ?: "unknown",
            )
        }
    }

    @Async
    fun deleteContractRecord(employeeId: String) {
        runCatching {
            ledgerService.deleteRecord(employeeId, "CONTRACT", updatedBy = "system")
            log.info("[FabricBridge] CONTRACT DELETE written — employeeId=$employeeId")
        }.onFailure { ex ->
            log.warn("[FabricBridge] CONTRACT DELETE failed — employeeId=$employeeId error=${ex.message}")
            outboxService.enqueue(
                employeeId   = employeeId,
                recordType   = "CONTRACT",
                recordStatus = "DELETED",
                keyFields    = "{}",
                dataHash     = "",
                action       = "DELETE",
                updatedBy    = "system",
                error        = ex.message ?: "unknown",
            )
        }
    }

    // ── Payroll ───────────────────────────────────────────────────────────────

    @Async
    fun upsertPayrollRecord(payroll: PayrollEntity, action: String = "CREATE") {
        val employeeId = payroll.employee.id?.toString() ?: run {
            log.warn("[FabricBridge] PayrollEntity missing employeeId, skip")
            return
        }
        val keyFields = objectMapper.writeValueAsString(
            mapOf(
                "salaryType"  to payroll.salaryType,
                "currency"    to payroll.currency,
                "totalIncome" to payroll.totalIncome,
                "payDay"      to payroll.payDay?.toString(),
                "bankName"    to payroll.bankName,
                // Không đưa số tài khoản, số lương chi tiết vào keyFields
            )
        )
        val fullJson = objectMapper.writeValueAsString(payroll)
        val dataHash = sha256(fullJson)
        val status   = if (action == "DELETE") "DELETED" else "ACTIVE"

        runCatching {
            ledgerService.upsertRecord(
                UpsertIdentityRecordRequest(
                    employeeId = employeeId,
                    recordType = "PAYROLL",
                    status     = status,
                    keyFields  = keyFields,
                    dataHash   = dataHash,
                    action     = action,
                    updatedBy  = "system",
                )
            )
            log.info("[FabricBridge] PAYROLL written — employeeId=$employeeId action=$action")
        }.onFailure { ex ->
            log.warn("[FabricBridge] PAYROLL write failed, enqueuing for retry — employeeId=$employeeId error=${ex.message}")
            outboxService.enqueue(
                employeeId   = employeeId,
                recordType   = "PAYROLL",
                recordStatus = status,
                keyFields    = keyFields,
                dataHash     = dataHash,
                action       = action,
                updatedBy    = "system",
                error        = ex.message ?: "unknown",
            )
        }
    }

    @Async
    fun deletePayrollRecord(employeeId: String) {
        runCatching {
            ledgerService.deleteRecord(employeeId, "PAYROLL", updatedBy = "system")
            log.info("[FabricBridge] PAYROLL DELETE written — employeeId=$employeeId")
        }.onFailure { ex ->
            log.warn("[FabricBridge] PAYROLL DELETE failed — employeeId=$employeeId error=${ex.message}")
            outboxService.enqueue(
                employeeId   = employeeId,
                recordType   = "PAYROLL",
                recordStatus = "DELETED",
                keyFields    = "{}",
                dataHash     = "",
                action       = "DELETE",
                updatedBy    = "system",
                error        = ex.message ?: "unknown",
            )
        }
    }

    // ── Attendance ────────────────────────────────────────────────────────────

    @Async
    fun logAttendance(employeeId: String, action: String, updatedBy: String) {
        val keyFields = objectMapper.writeValueAsString(mapOf("action" to action))
        val dataHash  = sha256("$employeeId:$action")
        runCatching {
            ledgerService.upsertRecord(
                UpsertIdentityRecordRequest(
                    employeeId = employeeId, recordType = "ATTENDANCE",
                    status = "ACTIVE", keyFields = keyFields,
                    dataHash = dataHash, action = action, updatedBy = updatedBy,
                )
            )
        }.onFailure { ex ->
            outboxService.enqueue(employeeId, "ATTENDANCE", "ACTIVE", keyFields, dataHash, action, updatedBy, ex.message ?: "unknown")
        }
    }

    // ── Request ───────────────────────────────────────────────────────────────

    @Async
    fun logRequest(employeeId: String, requestType: String, action: String, updatedBy: String) {
        val keyFields = objectMapper.writeValueAsString(mapOf("requestType" to requestType, "action" to action))
        val dataHash  = sha256("$employeeId:$requestType:$action")
        runCatching {
            ledgerService.upsertRecord(
                UpsertIdentityRecordRequest(
                    employeeId = employeeId, recordType = "REQUEST",
                    status = "ACTIVE", keyFields = keyFields,
                    dataHash = dataHash, action = action, updatedBy = updatedBy,
                )
            )
        }.onFailure { ex ->
            outboxService.enqueue(employeeId, "REQUEST", "ACTIVE", keyFields, dataHash, action, updatedBy, ex.message ?: "unknown")
        }
    }

    // ── Company ───────────────────────────────────────────────────────────────

    @Async
    fun logCompany(companyId: String, action: String, updatedBy: String) {
        val keyFields = objectMapper.writeValueAsString(mapOf("companyId" to companyId))
        val dataHash  = sha256("$companyId:$action")
        runCatching {
            ledgerService.upsertRecord(
                UpsertIdentityRecordRequest(
                    employeeId = companyId, recordType = "COMPANY",
                    status = "ACTIVE", keyFields = keyFields,
                    dataHash = dataHash, action = action, updatedBy = updatedBy,
                )
            )
        }.onFailure { ex ->
            outboxService.enqueue(companyId, "COMPANY", "ACTIVE", keyFields, dataHash, action, updatedBy, ex.message ?: "unknown")
        }
    }

    // ── DID ───────────────────────────────────────────────────────────────────

    /**
     * Đăng ký DID Document lên Fabric. Gọi sau khi Admin approve account.
     *
     * @param employeeId  ID của employee (Long, dùng làm phần cuối DID)
     * @param publicKeyJwk  ECDSA P-256 public key dạng JWK JSON, do Flutter sinh và gửi lên
     * @param approvedBy  email/employeeId của Admin thực hiện approve
     */
    @Async
    fun registerDID(employeeId: String, publicKeyJwk: String, approvedBy: String) {
        val did = "did:fabric:trustid:$employeeId"
        runCatching {
            ledgerService.registerDID(
                RegisterDIDRequest(
                    did          = did,
                    employeeId   = employeeId,
                    publicKeyJwk = publicKeyJwk,
                    controller   = "did:fabric:trustid:org1",
                )
            )
            log.info("[FabricBridge] DID registered — did=$did approvedBy=$approvedBy")
        }.onFailure { ex ->
            log.warn("[FabricBridge] RegisterDID failed — did=$did error=${ex.message}")
            // DID registration failure: enqueue as PROFILE type audit event for retry
            outboxService.enqueue(
                employeeId   = employeeId,
                recordType   = "DID",
                recordStatus = "ACTIVE",
                keyFields    = objectMapper.writeValueAsString(mapOf("did" to did, "approvedBy" to approvedBy)),
                dataHash     = sha256("$did:$publicKeyJwk"),
                action       = "CREATE",
                updatedBy    = approvedBy,
                error        = ex.message ?: "unknown",
            )
        }
    }

    /**
     * Thu hồi DID Document khi chấm dứt hợp đồng.
     *
     * @param employeeId  ID của employee
     * @param revokedBy   email/employeeId của Admin/Chief thực hiện terminate
     * @param reason      lý do thu hồi, mặc định "CONTRACT_TERMINATED"
     */
    @Async
    fun revokeDID(employeeId: String, revokedBy: String, reason: String = "CONTRACT_TERMINATED") {
        val did = "did:fabric:trustid:$employeeId"
        runCatching {
            ledgerService.revokeDID(
                RevokeDIDRequest(
                    did          = did,
                    revokedBy    = revokedBy,
                    revokeReason = reason,
                )
            )
            log.info("[FabricBridge] DID revoked — did=$did revokedBy=$revokedBy reason=$reason")
        }.onFailure { ex ->
            log.warn("[FabricBridge] RevokeDID failed — did=$did error=${ex.message}")
            outboxService.enqueue(
                employeeId   = employeeId,
                recordType   = "DID",
                recordStatus = "REVOKED",
                keyFields    = objectMapper.writeValueAsString(mapOf("did" to did, "reason" to reason)),
                dataHash     = sha256("$did:REVOKED"),
                action       = "UPDATE",
                updatedBy    = revokedBy,
                error        = ex.message ?: "unknown",
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun sha256(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}