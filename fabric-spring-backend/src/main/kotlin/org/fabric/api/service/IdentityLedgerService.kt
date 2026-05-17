package org.fabric.api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.fabric.api.config.FabricProperties
import org.fabric.api.exception.IdentityRecordNotFoundException
import org.fabric.api.exception.FabricTransactionException
import org.fabric.api.model.DIDDocument
import org.fabric.api.model.IdentityRecord
import org.fabric.api.model.RegisterDIDRequest
import org.fabric.api.model.RevokeDIDRequest
import org.fabric.api.model.StatusListRecord
import org.fabric.api.model.UpdateStatusListEntryRequest
import org.fabric.api.model.UpsertIdentityRecordRequest
import org.fabric.api.model.VerifyRecordResponse
import org.fabric.api.websocket.FabricEvent
import org.fabric.api.websocket.FabricEventPublisher
import org.fabric.api.websocket.EventType
import org.hyperledger.fabric.client.EndorseException
import org.hyperledger.fabric.client.Gateway
import org.hyperledger.fabric.client.SubmitException
import org.springframework.stereotype.Service
import java.time.Instant

private val log = KotlinLogging.logger {}

/**
 * IdentityLedgerService — phía org.fabric.api
 *
 * Giao tiếp trực tiếp với chaincode `identity-ledger` (IdentityLedger.java) trên Hyperledger Fabric.
 *
 * Chaincode functions được gọi:
 *   - UpsertRecord(employeeId, recordType, status, keyFields, dataHash, action, timestamp, updatedBy)
 *   - GetRecord(recordType, employeeId)
 *   - RecordExists(recordType, employeeId)
 *   - DeleteRecord(employeeId, recordType, updatedBy)
 *   - GetAllRecordsByEmployee(employeeId)
 *   - GetAllRecords()
 *   - GetRecordHistory(recordType, employeeId)
 *
 * Tất cả dữ liệu nhận từ com.mpcorp.identity sẽ đi qua đây để ghi vào ledger.
 */
@Service
class IdentityLedgerService(
    private val gateway: Gateway,
    private val props: FabricProperties,
    private val objectMapper: ObjectMapper,
    private val eventPublisher: FabricEventPublisher,
) {

    private val network by lazy { gateway.getNetwork(props.channelName) }
    private val contract by lazy { network.getContract(props.chaincodeName) }

    // ── Init ──────────────────────────────────────────────────────────────────

    /**
     * Gọi InitLedger trên chaincode.
     * Thường chỉ cần gọi một lần khi bootstrap.
     */
    fun initLedger() {
        log.info { "Submitting InitLedger transaction" }
        runCatching {
            contract.submitTransaction("InitLedger")
        }.onFailure { handleFabricError("InitLedger", it) }
        log.info { "InitLedger committed successfully" }
        eventPublisher.publish(
            FabricEvent(EventType.INIT_LEDGER, recordId = null, payload = null)
        )
    }

    // ── Write (SUBMIT) ────────────────────────────────────────────────────────

    /**
     * Tạo hoặc cập nhật một IdentityRecord trên ledger.
     *
     * Đây là phương thức chính nhận dữ liệu từ com.mpcorp.identity
     * (qua HTTP hoặc service call) và ghi vào blockchain.
     *
     * @param request dữ liệu đã được chuẩn hóa từ identity backend
     * @return IdentityRecord đã được ghi lên ledger
     */
    fun upsertRecord(request: UpsertIdentityRecordRequest): IdentityRecord {
        val timestamp = Instant.now().toString()
        log.info { "SUBMIT UpsertRecord employeeId=${request.employeeId} type=${request.recordType} action=${request.action}" }

        val resultBytes = runCatching {
            contract.submitTransaction(
                "UpsertRecord",
                request.employeeId,
                request.recordType,
                request.status,
                request.keyFields,
                request.dataHash,
                request.action,
                timestamp,
                request.updatedBy,
            )
        }.getOrElse { handleFabricError("UpsertRecord", it) }

        val record = objectMapper.readValue<IdentityRecord>(resultBytes)

        // Publish WebSocket event
        val eventType = when (request.action.uppercase()) {
            "CREATE" -> EventType.RECORD_CREATED
            "UPDATE" -> EventType.RECORD_UPDATED
            "DELETE" -> EventType.RECORD_DELETED
            else     -> EventType.RECORD_UPDATED
        }
        eventPublisher.publish(FabricEvent(eventType, recordId = record.recordId, payload = record))

        return record
    }

    /**
     * Xóa logic một record (đặt status = DELETED, action = DELETE).
     * Chaincode không xóa vật lý — mọi thứ được ghi lại trong audit trail.
     */
    fun deleteRecord(employeeId: String, recordType: String, updatedBy: String = "system"): IdentityRecord {
        log.info { "SUBMIT DeleteRecord employeeId=$employeeId type=$recordType" }

        val resultBytes = runCatching {
            contract.submitTransaction("DeleteRecord", employeeId, recordType, updatedBy)
        }.getOrElse { handleFabricError("DeleteRecord", it) }

        val record = objectMapper.readValue<IdentityRecord>(resultBytes)
        eventPublisher.publish(FabricEvent(EventType.RECORD_DELETED, recordId = record.recordId, payload = record))
        return record
    }

    // ── Read (EVALUATE) ───────────────────────────────────────────────────────

    /**
     * Đọc một IdentityRecord theo employeeId + recordType.
     * Không tạo transaction trên ledger.
     */
    fun getRecord(recordType: String, employeeId: String): IdentityRecord {
        log.debug { "EVALUATE GetRecord employeeId=$employeeId type=$recordType" }
        val result = runCatching {
            contract.evaluateTransaction("GetRecord", recordType, employeeId)
        }.getOrElse { e ->
            if (e.message?.contains("does not exist") == true)
                throw IdentityRecordNotFoundException(employeeId, recordType)
            handleFabricError("ReadRecord", e)
        }
        return objectMapper.readValue(result)
    }

    /**
     * Kiểm tra record có tồn tại không mà không throw exception.
     */
    fun recordExists(recordType: String, employeeId: String): Boolean {
        log.debug { "EVALUATE RecordExists employeeId=$employeeId type=$recordType" }
        val result = runCatching {
            contract.evaluateTransaction("RecordExists", recordType, employeeId)
        }.getOrElse { handleFabricError("RecordExists", it) }
        return String(result).trim().equals("true", ignoreCase = true)
    }

    /**
     * Lấy tất cả record của một employee (PROFILE + CONTRACT + PAYROLL).
     */
    fun getAllRecordsByEmployee(employeeId: String): List<IdentityRecord> {
        log.debug { "EVALUATE GetAllRecordsByEmployee employeeId=$employeeId" }
        val result = runCatching {
            contract.evaluateTransaction("GetAllRecordsByEmployee", employeeId)
        }.getOrElse { handleFabricError("GetAllRecordsByEmployee", it) }
        return objectMapper.readValue(result)
    }

    /**
     * Lấy toàn bộ record trên ledger (dùng cho audit/admin).
     */
    fun getAllRecords(): List<IdentityRecord> {
        log.debug { "EVALUATE GetAllRecords" }
        val result = runCatching {
            contract.evaluateTransaction("GetAllRecords")
        }.getOrElse { handleFabricError("GetAllRecords", it) }
        return objectMapper.readValue(result)
    }

    /**
     * Lấy lịch sử thay đổi của một record theo employeeId + recordType.
     * Trả về danh sách các IdentityRecord theo thứ tự thời gian.
     */
    fun getRecordHistory(recordType: String, employeeId: String): List<IdentityRecord> {
        log.debug { "EVALUATE GetRecordHistory employeeId=$employeeId type=$recordType" }
        val result = runCatching {
            contract.evaluateTransaction("GetRecordHistory", recordType, employeeId)
        }.getOrElse { handleFabricError("GetRecordHistory", it) }
        return objectMapper.readValue(result)
    }

    // ── DID Layer ─────────────────────────────────────────────────────────────

    /**
     * Đăng ký DID Document lên ledger khi Admin approve account.
     * Gọi chaincode RegisterDID.
     */
    fun registerDID(request: RegisterDIDRequest): DIDDocument {
        val timestamp = Instant.now().toString()
        log.info { "SUBMIT RegisterDID did=${request.did} employeeId=${request.employeeId}" }

        val resultBytes = runCatching {
            contract.submitTransaction(
                "RegisterDID",
                request.did,
                request.employeeId,
                request.publicKeyJwk,
                request.controller,
                timestamp,
            )
        }.getOrElse { handleFabricError("RegisterDID", it) }

        val doc = objectMapper.readValue<DIDDocument>(resultBytes)
        eventPublisher.publish(FabricEvent(EventType.RECORD_CREATED, recordId = doc.did, payload = doc))
        return doc
    }

    /**
     * Thu hồi DID Document khi nhân viên chấm dứt hợp đồng.
     * Gọi chaincode RevokeDID.
     */
    fun revokeDID(request: RevokeDIDRequest): DIDDocument {
        val timestamp = Instant.now().toString()
        log.info { "SUBMIT RevokeDID did=${request.did} revokedBy=${request.revokedBy}" }

        val resultBytes = runCatching {
            contract.submitTransaction(
                "RevokeDID",
                request.did,
                request.revokedBy,
                request.revokeReason,
                timestamp,
            )
        }.getOrElse { handleFabricError("RevokeDID", it) }

        val doc = objectMapper.readValue<DIDDocument>(resultBytes)
        eventPublisher.publish(FabricEvent(EventType.RECORD_UPDATED, recordId = doc.did, payload = doc))
        return doc
    }

    /**
     * Resolve DID Document từ ledger.
     * Gọi chaincode ResolveDID (EVALUATE — không tốn phí).
     */
    fun resolveDID(did: String): DIDDocument {
        log.debug { "EVALUATE ResolveDID did=$did" }
        val result = runCatching {
            contract.evaluateTransaction("ResolveDID", did)
        }.getOrElse { e ->
            if (e.message?.contains("not found") == true)
                throw IdentityRecordNotFoundException(did, "DID")
            handleFabricError("ResolveDID", e)
        }
        return objectMapper.readValue(result)
    }

    // ── Status List 2021 — VC Revocation ──────────────────────────────────────

    /**
     * Updates the on-chain snapshot of a W3C Status List 2021 bitstring.
     * The backend computes the new encoded list (gzip + base64url of the bitmap)
     * and submits the whole payload — the chaincode just stores it.
     */
    fun updateStatusListEntry(request: UpdateStatusListEntryRequest): StatusListRecord {
        val timestamp = Instant.now().toString()
        log.info { "SUBMIT UpdateStatusListEntry listId=${request.listId} index=${request.updatedIndex} revoked=${request.revoked}" }

        val resultBytes = runCatching {
            contract.submitTransaction(
                "UpdateStatusListEntry",
                request.listId,
                request.encodedList,
                request.size.toString(),
                request.updatedIndex.toString(),
                request.revoked.toString(),
                timestamp,
                request.updatedBy,
            )
        }.getOrElse { handleFabricError("UpdateStatusListEntry", it) }

        val record = objectMapper.readValue<StatusListRecord>(resultBytes)
        eventPublisher.publish(FabricEvent(EventType.RECORD_UPDATED, recordId = "statuslist:${record.listId}", payload = record))
        return record
    }

    /**
     * Reads the latest StatusListRecord from the ledger.
     * Returns null if no record exists yet (first issuance scenario).
     */
    fun getStatusList(listId: String): StatusListRecord? {
        log.debug { "EVALUATE GetStatusList listId=$listId" }
        val result = runCatching {
            contract.evaluateTransaction("GetStatusList", listId)
        }.getOrElse { e ->
            if (e.message?.contains("not found") == true) return null
            handleFabricError("GetStatusList", e)
        }
        return objectMapper.readValue(result)
    }

    // ── Trust Registry ───────────────────────────────────────────────────────

    fun registerIssuer(did: String, name: String, role: String, scope: String, registeredBy: String) {
        val timestamp = Instant.now().toString()
        log.info { "SUBMIT RegisterIssuer did=$did name=$name" }
        runCatching {
            contract.submitTransaction("RegisterIssuer", did, name, role, scope, registeredBy, timestamp)
        }.getOrElse { handleFabricError("RegisterIssuer", it) }
    }

    fun revokeIssuer(did: String, revokedBy: String) {
        val timestamp = Instant.now().toString()
        log.info { "SUBMIT RevokeIssuer did=$did" }
        runCatching {
            contract.submitTransaction("RevokeIssuer", did, revokedBy, timestamp)
        }.getOrElse { handleFabricError("RevokeIssuer", it) }
    }

    @Suppress("UNCHECKED_CAST")
    fun listTrustedIssuers(): List<Map<String, Any?>> {
        log.debug { "EVALUATE ListTrustedIssuers" }
        val result = runCatching {
            contract.evaluateTransaction("ListTrustedIssuers")
        }.getOrElse { handleFabricError("ListTrustedIssuers", it) }
        return objectMapper.readValue(result, List::class.java) as List<Map<String, Any?>>
    }

    fun isTrustedIssuer(did: String): Boolean {
        log.debug { "EVALUATE IsTrustedIssuer did=$did" }
        val result = runCatching {
            contract.evaluateTransaction("IsTrustedIssuer", did)
        }.getOrElse { handleFabricError("IsTrustedIssuer", it) }
        return String(result).trim().equals("true", ignoreCase = true)
    }

    // ── Contract Signatures ───────────────────────────────────────────────────

    fun recordSignature(
        contractId: String,
        signerDid: String,
        signatureBase64: String,
        docHash: String,
        updatedBy: String,
    ) {
        val timestamp = Instant.now().toString()
        log.info { "SUBMIT RecordSignature contractId=$contractId signerDid=$signerDid" }
        runCatching {
            contract.submitTransaction("RecordSignature", contractId, signerDid, signatureBase64, docHash, timestamp, updatedBy)
        }.getOrElse { handleFabricError("RecordSignature", it) }
    }

    @Suppress("UNCHECKED_CAST")
    fun getSignatures(contractId: String): List<Map<String, Any?>> {
        log.debug { "EVALUATE GetSignatures contractId=$contractId" }
        val result = runCatching {
            contract.evaluateTransaction("GetSignatures", contractId)
        }.getOrElse { handleFabricError("GetSignatures", it) }
        return objectMapper.readValue(result, List::class.java) as List<Map<String, Any?>>
    }

    // ── Verify hash integrity ─────────────────────────────────────────────────

    /**
     * Verifies that the off-chain data (MySQL) has not been tampered with by comparing
     * the provided SHA-256 hash against the immutable dataHash stored on-chain.
     *
     * Usage:
     *   1. Caller re-serializes the MySQL record to JSON
     *   2. Computes sha256(json)
     *   3. Passes the hex string here
     *   4. Service calls VerifyRecord on chaincode and returns the result
     *
     * @param recordType    PROFILE | CONTRACT | PAYROLL
     * @param employeeId    employee identifier
     * @param hashToVerify  SHA-256 hex of current off-chain data
     * @return VerifyRecordResponse with valid=true if hashes match
     */
    fun verifyRecord(recordType: String, employeeId: String, hashToVerify: String): VerifyRecordResponse {
        log.debug { "EVALUATE VerifyRecord employeeId=$employeeId type=$recordType" }
        val result = runCatching {
            contract.evaluateTransaction("VerifyRecord", recordType, employeeId, hashToVerify)
        }.getOrElse { handleFabricError("VerifyRecord", it) }
        return objectMapper.readValue(result)
    }

    // ── Error handling ────────────────────────────────────────────────────────

    private fun handleFabricError(fn: String, t: Throwable): Nothing {
        val msg = when (t) {
            is EndorseException -> "Endorse failed for $fn: ${t.message}"
            is SubmitException  -> "Submit failed for $fn: ${t.message}"
            else                -> "Fabric error in $fn: ${t.message}"
        }
        log.error(t) { msg }
        throw FabricTransactionException(msg, t)
    }
}