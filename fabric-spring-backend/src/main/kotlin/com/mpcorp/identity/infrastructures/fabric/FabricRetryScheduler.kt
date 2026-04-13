package com.mpcorp.identity.infrastructures.fabric

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * FabricRetryScheduler — trigger định kỳ để xử lý hàng đợi retry Fabric.
 *
 * Chạy mỗi 30 giây (fixedDelay — tính từ lúc lần chạy trước kết thúc).
 * Delegate toàn bộ logic retry cho [FabricOutboxService.processDueEvents].
 *
 * Lưu ý: fixedDelay đảm bảo không chồng lấn lần chạy — nếu processDueEvents
 * mất 25s thì lần tiếp theo bắt đầu sau 30s nữa, không phải 5s sau.
 *
 * Cần @EnableScheduling trên Application class (đã có trong FabricApplication).
 */
@Component
class FabricRetryScheduler(
    private val outboxService: FabricOutboxService,
) {
    private val log = LoggerFactory.getLogger(FabricRetryScheduler::class.java)

    @Scheduled(fixedDelay = 30_000)
    fun tick() {
        log.debug("[FabricRetryScheduler] Checking fabric outbox for due events")
        runCatching {
            outboxService.processDueEvents()
        }.onFailure { ex ->
            // Scheduler không được crash — log và tiếp tục
            log.error("[FabricRetryScheduler] Unexpected error during outbox processing: ${ex.message}", ex)
        }
    }
}