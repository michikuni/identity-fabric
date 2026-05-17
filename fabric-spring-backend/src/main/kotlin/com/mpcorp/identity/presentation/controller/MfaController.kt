package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.AuthJpaRepository
import com.mpcorp.identity.infrastructures.security.MfaService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * MfaController — TOTP 2FA setup and verification for Admin/Chief accounts.
 *
 * Endpoints:
 *   POST /api/v1/mfa/setup      — generate secret + QR code
 *   POST /api/v1/mfa/verify-setup — confirm code before enabling
 *   POST /api/v1/mfa/disable    — disable MFA (requires valid TOTP code)
 *   POST /api/v1/mfa/validate   — called during login second step (public — no JWT yet)
 *   GET  /api/v1/mfa/status     — check whether MFA is enabled for current user
 */
@RestController
@RequestMapping("/api/v1/mfa")
class MfaController(
    private val mfaService: MfaService,
    private val authJpaRepository: AuthJpaRepository,
) {
    private val objectMapper = jacksonObjectMapper()

    data class SetupRequest(val userId: String)
    data class VerifySetupRequest(val userId: String, val code: String)
    data class DisableRequest(val userId: String, val code: String)
    data class ValidateRequest(val userId: String, val code: String)

    /**
     * Step 1 of MFA setup: generate secret + QR code data URI.
     * The secret is stored temporarily (mfaSecret) but mfaEnabled stays false
     * until verify-setup succeeds.
     */
    @PostMapping("/setup")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    fun setup(@RequestBody body: SetupRequest): ApiResponse<Any> {
        val auth = authJpaRepository.findById(UUID.fromString(body.userId))
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        val secret = mfaService.generateSecret()
        val qrDataUri = mfaService.generateQrDataUri(auth.email, secret)

        auth.mfaSecret = secret
        authJpaRepository.save(auth)

        return ApiResponse(
            status = "200", message = "MFA secret generated — scan QR then call /verify-setup",
            data = mapOf(
                "userId"    to auth.id.toString(),
                "qrDataUri" to qrDataUri,
                "secret"    to secret,
            )
        )
    }

    /**
     * Step 2: verify the TOTP code from the authenticator app.
     * On success, sets mfaEnabled=true and generates 8 backup codes.
     */
    @PostMapping("/verify-setup")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    fun verifySetup(@RequestBody body: VerifySetupRequest): ApiResponse<Any> {
        val auth = authJpaRepository.findById(UUID.fromString(body.userId))
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        val secret = auth.mfaSecret
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Call /setup first")

        if (!mfaService.verifyCode(secret, body.code)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid TOTP code")
        }

        val backupCodes = mfaService.generateBackupCodes()
        auth.mfaEnabled = true
        auth.mfaBackupCodes = objectMapper.writeValueAsString(backupCodes)
        authJpaRepository.save(auth)

        return ApiResponse(
            status = "200", message = "MFA enabled",
            data = mapOf(
                "userId"      to auth.id.toString(),
                "backupCodes" to backupCodes,
                "warning"     to "Store these backup codes safely — they will not be shown again",
            )
        )
    }

    /** Disable MFA for a user — requires a valid current TOTP code to prevent abuse. */
    @PostMapping("/disable")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    fun disable(@RequestBody body: DisableRequest): ApiResponse<Any> {
        val auth = authJpaRepository.findById(UUID.fromString(body.userId))
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        if (!auth.mfaEnabled) {
            return ApiResponse(status = "200", message = "MFA was not enabled", data = null)
        }

        val secret = auth.mfaSecret
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No MFA secret found")

        if (!mfaService.verifyCode(secret, body.code)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid TOTP code")
        }

        auth.mfaEnabled    = false
        auth.mfaSecret     = null
        auth.mfaBackupCodes = null
        authJpaRepository.save(auth)

        return ApiResponse(status = "200", message = "MFA disabled", data = mapOf("userId" to auth.id.toString()))
    }

    /**
     * Second-factor validation during login.
     * Called after password auth succeeds — client sends userId + TOTP code.
     * Public (no JWT required) because the JWT hasn't been issued yet at this stage.
     */
    @PostMapping("/validate")
    fun validate(@RequestBody body: ValidateRequest): ApiResponse<Any> {
        val auth = authJpaRepository.findById(UUID.fromString(body.userId))
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }

        if (!auth.mfaEnabled) {
            return ApiResponse(status = "200", message = "MFA not required", data = mapOf("mfaRequired" to false))
        }

        val secret = auth.mfaSecret
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "MFA misconfigured")

        // Check backup codes first
        val backupCodes = auth.mfaBackupCodes?.let {
            @Suppress("UNCHECKED_CAST")
            objectMapper.readValue(it, List::class.java) as List<String>
        } ?: emptyList()

        val usedBackup = backupCodes.contains(body.code)
        if (usedBackup) {
            // Consume the backup code
            val remaining = backupCodes - body.code
            auth.mfaBackupCodes = objectMapper.writeValueAsString(remaining)
            authJpaRepository.save(auth)
            return ApiResponse(
                status = "200", message = "Authenticated via backup code",
                data = mapOf("valid" to true, "backupCodesRemaining" to remaining.size)
            )
        }

        val valid = mfaService.verifyCode(secret, body.code)
        if (!valid) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid TOTP code")
        }

        return ApiResponse(status = "200", message = "TOTP verified", data = mapOf("valid" to true))
    }

    /** Check MFA status for a given userId. */
    @GetMapping("/status/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    fun status(@PathVariable userId: String): ApiResponse<Any> {
        val auth = authJpaRepository.findById(UUID.fromString(userId))
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        return ApiResponse(
            status = "200", message = "OK",
            data = mapOf(
                "userId"     to auth.id.toString(),
                "mfaEnabled" to auth.mfaEnabled,
                "hasSecret"  to (auth.mfaSecret != null),
            )
        )
    }
}
