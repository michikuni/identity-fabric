package com.mpcorp.identity.infrastructures.fabric

import com.mpcorp.identity.infrastructures.persistence.jpa_entity.FabricOutboxJpaEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.OutboxEventStatus
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.FabricOutboxJpaRepository
import org.fabric.api.model.UpsertIdentityRecordRequest
import org.fabric.api.service.IdentityLedgerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.math.min

/**
 * FabricOutboxService — quản lý hàng đợi retry cho các giao dịch Fabric thất bại.
 *
 * ## Chiến lược Fire-and-Forget + Outbox
 *
 * Khi [IdentityLedgerBridge] ghi Fabric lỗi:
 *   1. MySQL đã commit thành công (source of truth không bị ảnh hưởng)
 *   2. Event được lưu vào bảng `fabric_outbox_events` với status=PENDING
 *   3. [FabricRetryScheduler] định kỳ gọi [processDueEvents] để retry
 *
 * ## Exponential Backoff
 *
 *   BASE_DELAY = 30s, nhân đôi mỗi lần thất bại, giới hạn MAX_DELAY = 3600s
 *
 *   Retry 0 (lần đầu ghi thất bại) → enqueue, nextRetryAt = now + 30s
 *   Retry 1 → next = now + 60s   (30 * 2^1)
 *   Retry 2 → next = now + 120s  (30 * 2^2)
 *   Retry 3 → next = now + 240s  (30 * 2^3)
 *   Retry 4 → next = now + 480s  (30 * 2^4)
 *   Retry 5 → DEAD_LETTER — cần xử lý thủ công hoặc alert
 *
 * ## Dead Letter
 *
 * Sau [MAX_RETRIES] lần, event chuyển sang DEAD_LETTER và dừng retry.
 * Operator nên:
 *   - Nhận alert từ log ERROR
 *   - Query: SELECT * FROM fabric_outbox_events WHERE event_status = 'DEAD_LETTER'
 *   - Điều tra lỗi Fabric rồi manual requeue nếu cần
 */
@Service
class FabricOutboxService(
    private val repository: FabricOutboxJpaRepository,
    private val ledgerService: IdentityLedgerService,
) {
    private val log = LoggerFactory.getLogger(FabricOutboxService::class.java)

    companion object {
        /** Số lần retry tối đa trước khi chuyển DEAD_LETTER */
        const val MAX_RETRIES = 5

        /** Delay cơ sở (giây), nhân đôi mỗi lần thất bại */
        const val BASE_DELAY_SECONDS = 30L

        /** Delay tối đa — 1 giờ */
        const val MAX_DELAY_SECONDS = 3600L
    }

    // ── Enqueue ───────────────────────────────────────────────────────────────

    /**
     * Lưu một event thất bại vào outbox để retry về sau.
     * Được gọi trong catch block của [IdentityLedgerBridge].
     *
     * nextRetryAt ban đầu = now + BASE_DELAY_SECONDS (tránh retry ngay lập tức).
     */
    @Transactional
    fun enqueue(
        employeeId: String,
        recordType: String,
        recordStatus: String,
        keyFields: String,
        dataHash: String,
        action: String,
        updatedBy: String,
        error: String,
    ) {
        val event = FabricOutboxJpaEntity(
            employeeId   = employeeId,
            recordType   = recordType,
            recordStatus = recordStatus,
            keyFields    = keyFields,
            dataHash     = dataHash,
            action       = action,
            updatedBy    = updatedBy,
            eventStatus  = OutboxEventStatus.PENDING,
            retryCount   = 0,
            createdAt    = Instant.now(),
            nextRetryAt  = Instant.now().plusSeconds(BASE_DELAY_SECONDS),
            lastError    = error.take(500),
        )
        repository.save(event)
        log.info(
            "[FabricOutbox] Event enqueued — employeeId=$employeeId " +
            "type=$recordType action=$action firstRetryAt=${event.nextRetryAt}"
        )
    }

    // ── Process ───────────────────────────────────────────────────────────────

    /**
     * Được gọi bởi [FabricRetryScheduler] định kỳ.
     * Tìm tất cả event đến hạn retry và xử lý từng event một.
     */
    @Transactional
    fun processDueEvents() {
        val dueEvents = repository.findByEventStatusInAndNextRetryAtLessThanEqual(
            statuses = listOf(OutboxEventStatus.PENDING, OutboxEventStatus.RETRYING),
            now = Instant.now(),
        )
        if (dueEvents.isEmpty()) return

        log.info("[FabricOutbox] Processing ${dueEvents.size} due event(s)")
        dueEvents.forEach { processEvent(it) }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun processEvent(event: FabricOutboxJpaEntity) {
        event.lastAttemptAt = Instant.now()
        event.retryCount++

        runCatching {
            ledgerService.upsertRecord(
                UpsertIdentityRecordRequest(
                    employeeId = event.employeeId,
                    recordType = event.recordType,
                    status     = event.recordStatus,
                    keyFields  = event.keyFields,
                    dataHash   = event.dataHash,
                    action     = event.action,
                    updatedBy  = event.updatedBy,
                )
            )
            // Thành công
            event.eventStatus = OutboxEventStatus.COMPLETED
            repository.save(event)
            log.info(
                "[FabricOutbox] Event #${event.id} COMPLETED " +
                "after ${event.retryCount} attempt(s) — employeeId=${event.employeeId}"
            )
        }.onFailure { ex ->
            event.lastError = ex.message?.take(500)

            if (event.retryCount >= MAX_RETRIES) {
                // Chuyển Dead Letter
                event.eventStatus = OutboxEventStatus.DEAD_LETTER
                repository.save(event)
                log.error(
                    "[FabricOutbox] Event #${event.id} → DEAD_LETTER " +
                    "after $MAX_RETRIES retries. employeeId=${event.employeeId} " +
                    "type=${event.recordType} lastError=${event.lastError}"
                )
            } else {
                // Exponential backoff: 30 * 2^retryCount, tối đa 3600s
                val delaySeconds = min(BASE_DELAY_SECONDS * (1L shl event.retryCount), MAX_DELAY_SECONDS)
                event.eventStatus  = OutboxEventStatus.RETRYING
                event.nextRetryAt  = Instant.now().plusSeconds(delaySeconds)
                repository.save(event)
                log.warn(
                    "[FabricOutbox] Event #${event.id} retry ${event.retryCount}/$MAX_RETRIES — " +
                    "next attempt in ${delaySeconds}s at ${event.nextRetryAt}"
                )
            }
        }
    }
}