package org.fabric.api.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Explicitly provides an ObjectMapper bean scoped to org.fabric.api.
 *
 * Spring Boot 4.x uses Jackson 3.x (tools.jackson.*) for its auto-configured ObjectMapper,
 * but the existing service code imports com.fasterxml.jackson.databind.ObjectMapper (Jackson 2.x).
 * By defining an explicit bean here we bridge the gap — Spring will inject this
 * com.fasterxml.jackson.databind.ObjectMapper wherever AssetService requests it.
 */
@Configuration
class JacksonConfig {

    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper()
        .registerModule(KotlinModule.Builder().build())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}
