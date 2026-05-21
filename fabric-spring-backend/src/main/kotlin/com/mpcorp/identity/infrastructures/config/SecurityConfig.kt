package com.mpcorp.identity.infrastructures.config

import com.mpcorp.identity.infrastructures.security.JwtAuthFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity, jwtAuthFilter: JwtAuthFilter): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { }   // áp dụng bean corsConfigurationSource bên dưới
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(
                    "/api/v1/auth/**",
                    "/api/v1/assets/**",
                    "/api/v1/identity/**",   // DID resolve + VC fetch — public for Verifiers
                    "/api/v1/oidc/**",       // OID4VP — public for Verifiers and Holders
                    "/api/v1/status-list/**",// W3C Status List 2021 — public so Verifiers can check revocation
                    "/api/v1/mfa/validate",  // MFA second-factor — called before JWT is issued
                    "/1.0/identifiers/**",   // DIF Universal Resolver — public
                    "/api/v1/trust-registry/issuers", // Trust Registry public list
                    "/didcomm/**",                   // DIDComm peer messaging — authenticated via DID, not JWT
                    "/api/v1/notarization/**",        // Notarization — public verify, auth required for upload
                    "/ws/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**",
                    "/v3/api-docs/**",
                ).permitAll()
                    // SD-JWT verify/present/fetch are public; only /issue/** requires Admin (handled in controller)
                    .requestMatchers(
                        org.springframework.http.HttpMethod.POST,
                        "/api/v1/sd-jwt/verify",
                        "/api/v1/sd-jwt/present",
                    ).permitAll()
                    .requestMatchers(
                        org.springframework.http.HttpMethod.GET,
                        "/api/v1/sd-jwt/*/skill",
                        "/api/v1/sd-jwt/*/education",
                    ).permitAll()
                    .requestMatchers("/api/v1/ledger/**").hasAnyRole("CHIEF", "ADMIN")
                    .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "CHIEF")
                    .requestMatchers("/api/v1/chief/**").hasAnyRole("CHIEF", "ADMIN")
                    .requestMatchers("/api/v1/manager/**").hasAnyRole("MANAGER", "CHIEF", "ADMIN")
                    .anyRequest().authenticated()
            }.sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val cfg = CorsConfiguration().apply {
            allowedOrigins = listOf(
                "https://verify.michikuni.cloud",
                "https://michikuni.cloud",
            )
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", cfg)
        }
    }
}