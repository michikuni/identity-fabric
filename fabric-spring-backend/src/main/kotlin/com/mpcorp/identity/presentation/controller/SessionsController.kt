package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.UserSessionJpaEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.UserSessionJpaRepository
import com.mpcorp.identity.infrastructures.security.user_details.CustomUserDetails
import com.mpcorp.identity.common.utils.JwtUtils
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * SessionsController — device binding and session management.
 *
 * Endpoints:
 *   POST   /api/v1/sessions/register        — register/update current device session
 *   GET    /api/v1/sessions                 — list all active sessions for current user
 *   DELETE /api/v1/sessions/{deviceId}      — logout a specific device
 *   DELETE /api/v1/sessions                 — logout all devices (except current if ?keepCurrent=true)
 *
 * The Flutter app should call /register on every login with deviceId + deviceName + platform.
 */
@RestController
@RequestMapping("/api/v1/sessions")
class SessionsController(
    private val sessionRepository: UserSessionJpaRepository,
    private val jwtUtils: JwtUtils,
) {

    data class RegisterSessionRequest(
        val deviceId: String,
        val deviceName: String? = null,
        val devicePlatform: String? = null,
    )

    @PostMapping("/register")
    fun registerSession(
        @RequestBody body: RegisterSessionRequest,
        @RequestHeader("Authorization") authHeader: String,
        @AuthenticationPrincipal principal: UserDetails,
    ): ApiResponse<Any> {
        val userId = resolveUserId(principal)
        val token = if (authHeader.startsWith("Bearer ")) authHeader.substring(7) else authHeader
        val tokenHash = sha256(token).take(64)

        val existing = sessionRepository.findByUserIdAndDeviceId(userId, body.deviceId)
        val now = Timestamp.from(Instant.now())

        if (existing != null) {
            existing.lastSeen      = now
            existing.deviceName    = body.deviceName ?: existing.deviceName
            existing.devicePlatform = body.devicePlatform ?: existing.devicePlatform
            existing.isActive      = true
            existing.tokenHash     = tokenHash
            sessionRepository.save(existing)
        } else {
            sessionRepository.save(
                UserSessionJpaEntity(
                    userId         = userId,
                    deviceId       = body.deviceId,
                    deviceName     = body.deviceName,
                    devicePlatform = body.devicePlatform,
                    lastSeen       = now,
                    createdAt      = now,
                    isActive       = true,
                    tokenHash      = tokenHash,
                )
            )
        }

        return ApiResponse(status = "200", message = "Session registered", data = mapOf("deviceId" to body.deviceId))
    }

    @GetMapping
    fun listSessions(@AuthenticationPrincipal principal: UserDetails): ApiResponse<Any> {
        val userId = resolveUserId(principal)
        val sessions = sessionRepository.findByUserIdAndIsActiveTrue(userId)
            .map { session ->
                mapOf(
                    "sessionId"      to session.id?.toString(),
                    "deviceId"       to session.deviceId,
                    "deviceName"     to (session.deviceName ?: "Unknown device"),
                    "devicePlatform" to (session.devicePlatform ?: "Unknown"),
                    "lastSeen"       to session.lastSeen.toInstant().toString(),
                    "createdAt"      to session.createdAt.toInstant().toString(),
                )
            }
        return ApiResponse(status = "200", message = "Active sessions (${sessions.size})", data = sessions)
    }

    @DeleteMapping("/{deviceId}")
    @Transactional
    fun logoutDevice(
        @PathVariable deviceId: String,
        @AuthenticationPrincipal principal: UserDetails,
    ): ApiResponse<Any> {
        val userId = resolveUserId(principal)
        sessionRepository.deactivateByUserIdAndDeviceId(userId, deviceId)
        return ApiResponse(status = "200", message = "Device logged out", data = mapOf("deviceId" to deviceId))
    }

    @DeleteMapping
    @Transactional
    fun logoutAllDevices(
        @RequestParam(defaultValue = "false") keepCurrent: Boolean,
        @RequestHeader("Authorization") authHeader: String,
        @AuthenticationPrincipal principal: UserDetails,
    ): ApiResponse<Any> {
        val userId = resolveUserId(principal)

        if (keepCurrent) {
            val token = if (authHeader.startsWith("Bearer ")) authHeader.substring(7) else authHeader
            val currentDeviceId = jwtUtils.extractDeviceId(token)
            if (currentDeviceId != null) {
                val all = sessionRepository.findByUserIdAndIsActiveTrue(userId)
                all.filter { it.deviceId != currentDeviceId }.forEach {
                    it.isActive = false
                    sessionRepository.save(it)
                }
                return ApiResponse(status = "200", message = "All other devices logged out", data = mapOf("keptDeviceId" to currentDeviceId))
            }
        }

        sessionRepository.deactivateAllByUserId(userId)
        return ApiResponse(status = "200", message = "All devices logged out", data = null)
    }

    private fun resolveUserId(principal: UserDetails): UUID {
        if (principal is CustomUserDetails) {
            return principal.getId() ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot resolve user identity")
        }
        return try { UUID.fromString(principal.username) }
        catch (_: IllegalArgumentException) { throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot resolve user identity") }
    }

    private fun sha256(data: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(data.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
