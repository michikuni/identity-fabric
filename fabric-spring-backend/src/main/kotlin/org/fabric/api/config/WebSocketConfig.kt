package org.fabric.api.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        // In-memory broker; topic prefix for push subscriptions
        registry.enableSimpleBroker("/topic")
        // Prefix for client-to-server messages (not used here, but good practice)
        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // Native WebSocket endpoint
        registry.addEndpoint("/ws/fabric")
            .setAllowedOriginPatterns("*")
        // SockJS fallback for browsers that don't support raw WS
        registry.addEndpoint("/ws/fabric")
            .setAllowedOriginPatterns("*")
            .withSockJS()
    }
}
