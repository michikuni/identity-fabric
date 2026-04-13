package org.fabric.api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.fabric.api.config.FabricProperties
import org.fabric.api.exception.IdentityRecordNotFoundException
import org.fabric.api.exception.FabricTransactionException
import org.fabric.api.model.IdentityRecord
import org.fabric.api.model.UpsertIdentityRecordRequest
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
 *   - ReadRecord(employeeId, recordType)
 *   - RecordExists(employeeId, recordType)
 *   - DeleteRecord(employeeId, recordType, updatedBy)
 *   - GetRecordsByEmployee(employeeId)
 *   - GetAllRecords()
 *   - GetRecordHistory(employeeId, recordType)
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
    fun readRecord(employeeId: String, recordType: String): IdentityRecord {
        log.debug { "EVALUATE ReadRecord employeeId=$employeeId type=$recordType" }
        val result = runCatching {
            contract.evaluateTransaction("ReadRecord", employeeId, recordType)
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
    fun recordExists(employeeId: String, recordType: String): Boolean {
        log.debug { "EVALUATE RecordExists employeeId=$employeeId type=$recordType" }
        val result = runCatching {
            contract.evaluateTransaction("RecordExists", employeeId, recordType)
        }.getOrElse { handleFabricError("RecordExists", it) }
        return String(result).trim().equals("true", ignoreCase = true)
    }

    /**
     * Lấy tất cả record của một employee (PROFILE + CONTRACT + PAYROLL).
     */
    fun getRecordsByEmployee(employeeId: String): List<IdentityRecord> {
        log.debug { "EVALUATE GetRecordsByEmployee employeeId=$employeeId" }
        val result = runCatching {
            contract.evaluateTransaction("GetRecordsByEmployee", employeeId)
        }.getOrElse { handleFabricError("GetRecordsByEmployee", it) }
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
    fun getRecordHistory(employeeId: String, recordType: String): List<IdentityRecord> {
        log.debug { "EVALUATE GetRecordHistory employeeId=$employeeId type=$recordType" }
        val result = runCatching {
            contract.evaluateTransaction("GetRecordHistory", employeeId, recordType)
        }.getOrElse { handleFabricError("GetRecordHistory", it) }
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