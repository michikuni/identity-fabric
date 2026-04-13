package org.fabric.api.websocket

import mu.KotlinLogging
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * Broadcasts [FabricEvent]s tới các STOMP topics:
 *
 *   - `/topic/identity`         — tất cả identity record events
 *   - `/topic/identity/{id}`    — events theo recordId cụ thể
 */
@Component
class FabricEventPublisher(private val messagingTemplate: SimpMessagingTemplate) {

    fun publish(event: FabricEvent) {
        log.debug { "Publishing WS event: ${event.type} for record=${event.recordId}" }

        messagingTemplate.convertAndSend("/topic/identity", event)

        event.recordId?.let { id ->
            messagingTemplate.convertAndSend("/topic/identity/$id", event)
        }
    }
}