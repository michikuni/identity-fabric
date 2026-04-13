package com.mpcorp.identity.infrastructures.persistence.jpa_repository

import com.mpcorp.identity.infrastructures.persistence.jpa_entity.FabricOutboxJpaEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.OutboxEventStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

/**
 * Spring Data JPA repository cho fabric_outbox_events.
 *
 * Query chính: tìm các event đến hạn retry (nextRetryAt <= now)
 * với status PENDING hoặc RETRYING.
 */
interface FabricOutboxJpaRepository : JpaRepository<FabricOutboxJpaEntity, Long> {

    /**
     * Tìm các event đã đến hạn retry.
     * Được gọi bởi [com.mpcorp.identity.infrastructures.fabric.FabricRetryScheduler]
     * mỗi 30 giây.
     *
     * @param statuses  danh sách trạng thái cần xử lý (PENDING, RETRYING)
     * @param now       thời điểm hiện tại — event có nextRetryAt <= now sẽ được chọn
     */
    fun findByEventStatusInAndNextRetryAtLessThanEqual(
        statuses: Collection<OutboxEventStatus>,
        now: Instant,
    ): List<FabricOutboxJpaEntity>
}