package com.mpcorp.identity.infrastructures.fabric

import com.fasterxml.jackson.databind.ObjectMapper
import com.mpcorp.identity.domain.entity.ContractEntity
import com.mpcorp.identity.domain.entity.PayrollEntity
import com.mpcorp.identity.domain.entity.ProfileEntity
import org.fabric.api.model.DIDDocument
import org.fabric.api.model.RegisterDIDRequest
import org.fabric.api.model.RevokeDIDRequest
import org.fabric.api.model.StatusListRecord
import org.fabric.api.model.UpdateStatusListEntryRequest
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

    // ── Status List 2021 ──────────────────────────────────────────────────────

    /**
     * Push an updated Status List 2021 snapshot to Fabric (synchronous —
     * the caller usually needs to know whether the on-chain write succeeded
     * before responding to the Verifier).
     *
     * Unlike most bridge methods we keep this synchronous on purpose: the
     * Verifier-facing controller fetches the encoded list from chaincode,
     * so a delayed/queued write would race with verification reads.
     *
     * Failures still go to the outbox so retries can heal eventual consistency.
     */
    fun updateStatusListEntry(
        listId: String,
        encodedList: String,
        size: Long,
        updatedIndex: Long,
        revoked: Boolean,
        updatedBy: String,
    ): StatusListRecord? {
        return runCatching {
            val record = ledgerService.updateStatusListEntry(
                UpdateStatusListEntryRequest(
                    listId       = listId,
                    encodedList  = encodedList,
                    size         = size,
                    updatedIndex = updatedIndex,
                    revoked      = revoked,
                    updatedBy    = updatedBy,
                )
            )
            log.info("[FabricBridge] StatusList updated — listId=$listId index=$updatedIndex revoked=$revoked")
            record
        }.getOrElse { ex ->
            log.warn("[FabricBridge] StatusList update failed — listId=$listId index=$updatedIndex error=${ex.message}")
            outboxService.enqueue(
                employeeId   = "statuslist:$listId",
                recordType   = "STATUS_LIST",
                recordStatus = if (revoked) "REVOKED" else "ACTIVE",
                keyFields    = objectMapper.writeValueAsString(
                    mapOf(
                        "listId"       to listId,
                        "updatedIndex" to updatedIndex,
                        "revoked"      to revoked,
                        "size"         to size,
                    )
                ),
                dataHash     = sha256("$listId:$updatedIndex:$revoked"),
                action       = "UPDATE",
                updatedBy    = updatedBy,
                error        = ex.message ?: "unknown",
            )
            null
        }
    }

    /**
     * Resolves the latest StatusListRecord from the ledger.
     * Returns null if the list has never been written (first issuance case).
     */
    fun getStatusList(listId: String): StatusListRecord? {
        return runCatching {
            ledgerService.getStatusList(listId)
        }.getOrElse { ex ->
            log.warn("[FabricBridge] StatusList resolve failed — listId=$listId error=${ex.message}")
            null
        }
    }

    // ── DID Resolver ──────────────────────────────────────────────────────────

    /**
     * Resolve a DID Document from the ledger. Propagates exceptions so callers
     * (e.g. UniversalResolverController) can return the appropriate HTTP status.
     */
    fun resolveDid(did: String): DIDDocument = ledgerService.resolveDID(did)

    // ── Trust Registry ────────────────────────────────────────────────────────

    fun registerIssuer(did: String, name: String, role: String, scope: String, registeredBy: String) {
        runCatching {
            ledgerService.registerIssuer(did, name, role, scope, registeredBy)
            log.info("[FabricBridge] Issuer registered — did=$did name=$name")
        }.onFailure { ex ->
            log.warn("[FabricBridge] RegisterIssuer failed — did=$did error=${ex.message}")
        }
    }

    fun revokeIssuer(did: String, revokedBy: String) {
        runCatching {
            ledgerService.revokeIssuer(did, revokedBy)
            log.info("[FabricBridge] Issuer revoked — did=$did revokedBy=$revokedBy")
        }.onFailure { ex ->
            log.warn("[FabricBridge] RevokeIssuer failed — did=$did error=${ex.message}")
        }
    }

    fun listTrustedIssuers(): List<Map<String, Any?>> = runCatching {
        ledgerService.listTrustedIssuers()
    }.getOrElse { ex ->
        log.warn("[FabricBridge] ListTrustedIssuers failed — error=${ex.message}")
        emptyList()
    }

    fun isTrustedIssuer(did: String): Boolean = runCatching {
        ledgerService.isTrustedIssuer(did)
    }.getOrElse { false }

    // ── Contract Signatures ───────────────────────────────────────────────────

    fun recordSignature(
        contractId: String,
        signerDid: String,
        signatureBase64: String,
        docHash: String,
        updatedBy: String,
    ) {
        runCatching {
            ledgerService.recordSignature(contractId, signerDid, signatureBase64, docHash, updatedBy)
            log.info("[FabricBridge] Signature recorded — contractId=$contractId signerDid=$signerDid")
        }.onFailure { ex ->
            log.warn("[FabricBridge] RecordSignature failed — contractId=$contractId error=${ex.message}")
            outboxService.enqueue(
                employeeId   = "contract:$contractId",
                recordType   = "CONTRACT_SIGNATURE",
                recordStatus = "SIGNED",
                keyFields    = objectMapper.writeValueAsString(mapOf("contractId" to contractId, "signerDid" to signerDid)),
                dataHash     = sha256("$contractId:$signerDid:$docHash"),
                action       = "CREATE",
                updatedBy    = updatedBy,
                error        = ex.message ?: "unknown",
            )
        }
    }

    fun getSignatures(contractId: String): List<Map<String, Any?>> = runCatching {
        ledgerService.getSignatures(contractId)
    }.getOrElse { ex ->
        log.warn("[FabricBridge] GetSignatures failed — contractId=$contractId error=${ex.message}")
        emptyList()
    }

    // ── Notarization ─────────────────────────────────────────────────────────

    /**
     * Anchor a document fingerprint on Fabric.
     * Uses UpsertRecord with recordType=DOCUMENT so it integrates with the existing
     * audit trail and GetRecordHistory chaincode functions without new chaincode code.
     *
     * keyFields stores non-PII metadata (filename, mimeType, label, signerDid).
     * dataHash is SHA-256 of the original file bytes — computed by the controller.
     */
    @Async
    fun upsertNotarizationRecord(
        docId: String,
        docHash: String,
        filename: String,
        mimeType: String,
        label: String,
        signerDid: String,
        updatedBy: String,
    ) {
        val keyFields = objectMapper.writeValueAsString(
            mapOf(
                "docId"     to docId,
                "filename"  to filename,
                "mimeType"  to mimeType,
                "label"     to label,
                "signerDid" to signerDid,
            )
        )
        runCatching {
            ledgerService.upsertRecord(
                UpsertIdentityRecordRequest(
                    employeeId   = "document:$docId",
                    recordType   = "DOCUMENT",
                    status       = "ACTIVE",
                    keyFields    = keyFields,
                    dataHash     = docHash,
                    action       = "NOTARIZE",
                    updatedBy    = updatedBy,
                )
            )
            log.info("[FabricBridge] Document notarized — docId=$docId signerDid=$signerDid hash=$docHash")
        }.onFailure { ex ->
            log.warn("[FabricBridge] Notarization failed — docId=$docId error=${ex.message}")
            outboxService.enqueue(
                employeeId   = "document:$docId",
                recordType   = "DOCUMENT",
                recordStatus = "ACTIVE",
                keyFields    = keyFields,
                dataHash     = docHash,
                action       = "NOTARIZE",
                updatedBy    = updatedBy,
                error        = ex.message ?: "unknown",
            )
        }
    }

    /**
     * Retrieve notarization record from on-chain by docId.
     * Returns null if the record has never been written.
     */
    fun getNotarizationRecord(docId: String): Map<String, Any?>? = runCatching {
        val record = ledgerService.getRecord("DOCUMENT", "document:$docId")
        mapOf(
            "recordId"   to record.recordId,
            "recordType" to record.recordType,
            "status"     to record.status,
            "dataHash"   to record.dataHash,
            "keyFields"  to record.keyFields,
            "action"     to record.action,
            "timestamp"  to record.timestamp,
            "updatedBy"  to record.updatedBy,
        )
    }.getOrElse { ex ->
        log.warn("[FabricBridge] GetNotarizationRecord failed — docId=$docId error=${ex.message}")
        null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun sha256(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}