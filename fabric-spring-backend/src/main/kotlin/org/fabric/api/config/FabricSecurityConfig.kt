package org.fabric.api.config

import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

// Standalone security config - only used when running FabricApplication without com.mpcorp.identity.
// When running unified app, SecurityConfig (com.mpcorp.identity) handles all security.
// @Configuration is intentionally removed to avoid UnreachableFilterChainException.
class FabricSecurityConfig {

    fun fabricSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()
            }
        return http.build()
    }
}