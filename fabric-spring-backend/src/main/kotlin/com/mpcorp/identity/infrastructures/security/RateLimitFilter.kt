package com.mpcorp.identity.infrastructures.security

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * RateLimitFilter — Bucket4j token-bucket rate limiter for /api/v1/auth/ endpoints.
 *
 * Policy: 10 requests / minute / IP.
 * Exceeding the limit → HTTP 429 Too Many Requests.
 *
 * Buckets are kept in-memory (ConcurrentHashMap). For multi-instance deployments,
 * swap to bucket4j-redis / bucket4j-hazelcast backed by a distributed cache.
 */
@Component
class RateLimitFilter : OncePerRequestFilter() {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    private fun buildBucket(): Bucket =
        Bucket.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(10)
                    .refillGreedy(10, Duration.ofMinutes(1))
                    .build()
            )
            .build()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        !request.requestURI.startsWith("/api/v1/auth/")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val ip = resolveClientIp(request)
        val bucket = buckets.computeIfAbsent(ip) { buildBucket() }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response)
        } else {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = "application/json"
            response.writer.write(
                """{"status":"429","message":"Too many requests — please wait before retrying","data":null}"""
            )
        }
    }

    private fun resolveClientIp(request: HttpServletRequest): String {
        val forwardedFor = request.getHeader("X-Forwarded-For")
        return if (!forwardedFor.isNullOrBlank()) forwardedFor.split(",").first().trim()
        else request.remoteAddr ?: "unknown"
    }
}
