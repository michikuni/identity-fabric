package org.fabric.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * Opens all endpoints for the Fabric Gateway API service.
 *
 * This service has no user authentication — it is a blockchain API layer
 * that will be called by the Identity backend (com.mpcorp.identity) or
 * directly by the Flutter mobile app via the identity service.
 *
 * In production: restrict this to specific IPs or add an API key filter.
 */
@Configuration
@EnableWebSecurity
class FabricSecurityConfig {

    @Bean
    fun fabricSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()
            }
        return http.build()
    }
}
