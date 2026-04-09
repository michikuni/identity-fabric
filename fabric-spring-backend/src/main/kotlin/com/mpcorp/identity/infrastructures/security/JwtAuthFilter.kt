package com.mpcorp.identity.infrastructures.security

import com.mpcorp.identity.infrastructures.security.user_details.CustomUserDetailsService
import com.mpcorp.identity.common.utils.JwtUtils
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class JwtAuthFilter(
    private val jwtUtils: JwtUtils,
    private val userDetailsService: CustomUserDetailsService
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        println("Authorization Header: $authHeader")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val jwt = authHeader.substring(7)
        try {
            val userId = jwtUtils.extractUserId(jwt)
            println("ok8$userId")

            if (SecurityContextHolder.getContext().authentication == null) {
                val userDetails = userDetailsService.loadUserByUserId(UUID.fromString(userId))
                val id = userDetails.getId() ?: throw RuntimeException("User id null")
                if (jwtUtils.validateToken(jwt, id.toString())) {
                    val authToken = UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.authorities

                    )
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authToken
                }
            }
        } catch (e: Exception) {
            println("Auth Filter exception: $e")
        }
        println("ok9")
        filterChain.doFilter(request, response)
    }
}