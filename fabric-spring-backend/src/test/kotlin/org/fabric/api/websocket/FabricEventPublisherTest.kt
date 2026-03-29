package org.fabric.api.websocket

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.simp.SimpMessagingTemplate
import kotlin.test.assertEquals

class FabricEventPublisherTest {

    private val messagingTemplate = mockk<SimpMessagingTemplate>(relaxed = true)
    private lateinit var publisher: FabricEventPublisher

    @BeforeEach
    fun setUp() {
        publisher = FabricEventPublisher(messagingTemplate)
    }

    @Test
    fun `publish sends to global topic`() {
        val event = FabricEvent(EventType.ASSET_CREATED, "asset1", mapOf("id" to "asset1"))

        publisher.publish(event)

        verify { messagingTemplate.convertAndSend("/topic/assets", event) }
    }

    @Test
    fun `publish sends to per-asset topic when assetId present`() {
        val event = FabricEvent(EventType.ASSET_UPDATED, "asset3", mapOf("id" to "asset3"))

        publisher.publish(event)

        verify { messagingTemplate.convertAndSend("/topic/assets/asset3", event) }
    }

    @Test
    fun `publish skips per-asset topic when assetId is null`() {
        val event = FabricEvent(EventType.INIT_LEDGER, assetId = null, payload = null)

        publisher.publish(event)

        verify(exactly = 1) { messagingTemplate.convertAndSend(any<String>(), any<FabricEvent>()) }
        verify { messagingTemplate.convertAndSend("/topic/assets", event) }
    }

    @Test
    fun `publish sends ASSET_DELETED event with correct type`() {
        val event = FabricEvent(EventType.ASSET_DELETED, "asset2", mapOf("id" to "asset2"))

        publisher.publish(event)

        val topicSlot = slot<String>()
        verify { messagingTemplate.convertAndSend(capture(topicSlot), event) }
        assertEquals(EventType.ASSET_DELETED, event.type)
    }
}
