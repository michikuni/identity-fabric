package org.fabric.api.config

import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * Standalone security config — chỉ dùng khi chạy FabricApplication riêng lẻ
 * (không có com.mpcorp.identity.infrastructures.config.SecurityConfig).
 *
 * Khi chạy unified app (FabricApplication + com.mpcorp.identity):
 *   SecurityConfig từ com.mpcorp.identity đã handle toàn bộ security
 *   (bao gồm /api/v1/ledger/**, /ws/**, JWT cho identity routes).
 *   Class này KHÔNG có @Configuration để tránh UnreachableFilterChainException.
 *
 * Nếu muốn chạy FabricApplication standalone, thêm lại @Configuration.
 */
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
