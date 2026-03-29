package org.fabric.api.websocket

import java.time.Instant

/**
 * All Fabric transaction events pushed over WebSocket.
 *
 * Clients subscribe to:
 *   /topic/assets          — every asset mutation
 *   /topic/assets/{id}     — mutations for a specific asset
 */
data class FabricEvent(
    /** One of: INIT_LEDGER, ASSET_CREATED, ASSET_UPDATED, ASSET_DELETED, ASSET_TRANSFERRED */
    val type: EventType,
    val assetId: String?,
    val payload: Any?,
    val timestamp: Instant = Instant.now()
)

enum class EventType {
    INIT_LEDGER,
    ASSET_CREATED,
    ASSET_UPDATED,
    ASSET_DELETED,
    ASSET_TRANSFERRED
}
