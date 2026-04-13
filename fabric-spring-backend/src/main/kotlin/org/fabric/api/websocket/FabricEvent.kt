package org.fabric.api.websocket

import java.time.Instant

/**
 * Tất cả Fabric transaction events được push qua WebSocket.
 *
 * Clients subscribe:
 *   /topic/identity          — mọi identity record mutation
 *   /topic/identity/{id}     — mutations cho một record cụ thể (theo recordId)
 */
data class FabricEvent(
    val type: EventType,
    val recordId: String?,
    val payload: Any?,
    val timestamp: Instant = Instant.now(),
)

enum class EventType {
    INIT_LEDGER,
    RECORD_CREATED,
    RECORD_UPDATED,
    RECORD_DELETED,
}