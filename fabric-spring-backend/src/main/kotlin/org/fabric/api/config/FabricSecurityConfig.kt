package org.fabric.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * Fallback security config — chỉ active khi chạy FabricApplication standalone
 * (không có SecurityConfig từ com.mpcorp.identity).
 *
 * Khi chạy unified app: SecurityConfig (@Order 1) xử lý tất cả request trước,
 * FabricSecurityConfig (@Order 2) không được invoke.
 *
 * In production: restrict this to specific IPs or add an API key filter.
 */
@Configuration
@EnableWebSecurity
@Order(2)
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
