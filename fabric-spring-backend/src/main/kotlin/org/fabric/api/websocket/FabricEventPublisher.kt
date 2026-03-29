package org.fabric.api.websocket

import mu.KotlinLogging
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Thin wrapper around [SimpMessagingTemplate] that broadcasts
 * [FabricEvent]s to two STOMP topics:
 *
 *   - `/topic/assets`       — all asset events (global feed)
 *   - `/topic/assets/{id}`  — events for a specific asset ID
 *
 * Usage:
 *   publisher.publish(FabricEvent(EventType.ASSET_CREATED, "asset7", asset))
 */
@Component
class FabricEventPublisher(private val messagingTemplate: SimpMessagingTemplate) {

    fun publish(event: FabricEvent) {
        log.debug { "Publishing WS event: ${event.type} for asset=${event.assetId}" }

        // Broadcast to global topic
        messagingTemplate.convertAndSend("/topic/assets", event)

        // Broadcast to per-asset topic (skip for init which has no specific ID)
        event.assetId?.let { id ->
            messagingTemplate.convertAndSend("/topic/assets/$id", event)
        }
    }
}
