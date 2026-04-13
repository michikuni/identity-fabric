package com.mpcorp.identity.infrastructures.persistence.jpa_entity

import jakarta.persistence.*
import java.time.Instant

/**
 * Trạng thái vòng đời của một outbox event:
 *
 *   PENDING     → mới tạo, chờ retry lần đầu
 *   RETRYING    → đang trong quá trình retry (đã thất bại ít nhất 1 lần)
 *   COMPLETED   → ghi Fabric thành công
 *   DEAD_LETTER → đã retry quá MAX_RETRIES lần, cần xử lý thủ công
 */
enum class OutboxEventStatus {
    PENDING, RETRYING, COMPLETED, DEAD_LETTER
}

/**
 * FabricOutboxJpaEntity — bảng hàng đợi retry cho các giao dịch Fabric thất bại.
 *
 * Khi [com.mpcorp.identity.infrastructures.fabric.IdentityLedgerBridge] gọi Fabric
 * và bị lỗi, thay vì bỏ qua, nó sẽ lưu event vào bảng này.
 * [com.mpcorp.identity.infrastructures.fabric.FabricRetryScheduler] sẽ định kỳ
 * đọc các event đến hạn và thực hiện retry với chiến lược exponential backoff.
 *
 * Exponential backoff (BASE_DELAY = 30s):
 *   Retry 1 → +60s  (30 * 2^1)
 *   Retry 2 → +120s (30 * 2^2)
 *   Retry 3 → +240s (30 * 2^3)
 *   Retry 4 → +480s (30 * 2^4)
 *   Retry 5 → DEAD_LETTER
 */
@Entity
@Table(
    name = "fabric_outbox_events",
    indexes = [
        Index(name = "idx_outbox_status_next_retry", columnList = "event_status, next_retry_at"),
    ]
)
data class FabricOutboxJpaEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    /** Employee ID nguồn từ com.mpcorp.identity */
    @Column(nullable = false)
    var employeeId: String,

    /** PROFILE | CONTRACT | PAYROLL */
    @Column(nullable = false)
    var recordType: String,

    /** ACTIVE | DELETED — trạng thái record tại thời điểm ghi */
    @Column(nullable = false)
    var recordStatus: String,

    /** JSON summary của các trường không nhạy cảm (partial snapshot) */
    @Column(columnDefinition = "TEXT", nullable = false)
    var keyFields: String,

    /** SHA-256 hex của toàn bộ off-chain data tại thời điểm ghi */
    @Column(nullable = false, length = 64)
    var dataHash: String,

    /** CREATE | UPDATE | DELETE */
    @Column(nullable = false)
    var action: String,

    /** Actor thực hiện thay đổi */
    @Column(nullable = false)
    var updatedBy: String,

    /** Trạng thái xử lý hiện tại của event này */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", nullable = false)
    var eventStatus: OutboxEventStatus = OutboxEventStatus.PENDING,

    /** Số lần đã retry (tính cả lần đầu tiên thất bại) */
    @Column(nullable = false)
    var retryCount: Int = 0,

    /** Thời điểm event được tạo lần đầu */
    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),

    /** Thời điểm thực hiện lần retry gần nhất */
    var lastAttemptAt: Instant? = null,

    /** Thời điểm sớm nhất có thể retry tiếp theo (exponential backoff) */
    @Column(name = "next_retry_at")
    var nextRetryAt: Instant? = null,

    /** Message lỗi của lần thất bại gần nhất (tối đa 500 ký tự) */
    @Column(columnDefinition = "TEXT")
    var lastError: String? = null,
)