package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.infrastructures.vc.VpService
import com.mpcorp.identity.infrastructures.vc.VpSessionStore
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

/**
 * Oidc4VpController — OID4VP (OpenID for Verifiable Presentations) endpoints.
 *
 * Flow:
 *   1. Verifier  → POST /oidc/vp/request           — create session, get Authorization Request
 *   2. Holder    → POST /oidc/vp/submit             — submit VP Token (signed Verifiable Presentation)
 *   3. Verifier  → GET  /oidc/vp/result/{state}     — poll for verification result
 *   4. Anyone    → GET  /.well-known/openid-configuration  — discovery (on root controller)
 *
 * All endpoints are public (no auth) — Verifiers are external parties.
 */
@RestController
@RequestMapping("/api/v1/oidc")
class Oidc4VpController(
    private val vpService: VpService,
    private val sessionStore: VpSessionStore,
) {

    // ── 1. Verifier creates a VP Request ──────────────────────────────────────

    data class VpRequestBody(
        /**
         * Fields the Verifier wants disclosed from credentialSubject.
         * e.g. ["employmentStatus"] — only proves employment, nothing else.
         * Default: ["employmentStatus", "position"] if omitted.
         */
        val requestedClaims: List<String> = listOf("employmentStatus", "position"),
    )

    /**
     * POST /api/v1/oidc/vp/request
     *
     * Verifier calls this to start the OID4VP flow.
     * Returns an Authorization Request the Verifier shows as QR or deeplink to the Holder.
     */
    @PostMapping("/vp/request")
    fun createVpRequest(@RequestBody body: VpRequestBody): ApiResponse<Map<String, Any>> {
        val session = sessionStore.create(body.requestedClaims)
        val authRequest = vpService.buildAuthorizationRequest(session)
        return ApiResponse(
            status = "200",
            message = "VP request created — present authorizationRequest to Holder (QR / deeplink)",
            data = mapOf(
                "state"                to session.state,
                "nonce"                to session.nonce,
                "expiresIn"            to 300,
                "authorizationRequest" to authRequest,
            ),
        )
    }

    // ── 2. Holder submits VP Token ─────────────────────────────────────────────

    data class VpSubmitBody(
        val state: String,
        val vpToken: String,   // W3C Verifiable Presentation JSON string
    )

    /**
     * POST /api/v1/oidc/vp/submit
     *
     * Flutter wallet calls this after the user selects which fields to disclose
     * and the app builds the VP Token.
     */
    @PostMapping("/vp/submit")
    fun submitVp(@RequestBody body: VpSubmitBody): ApiResponse<Map<String, Any>> {
        val session = sessionStore.get(body.state)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found or expired")

        if (!sessionStore.submitVp(body.state, body.vpToken)) {
            throw ResponseStatusException(HttpStatus.GONE, "Session expired")
        }

        // Verify immediately and cache result
        val result = vpService.verifyVpToken(body.vpToken, session)
        sessionStore.saveResult(body.state, result.valid, result.reason)

        return ApiResponse(
            status = "200",
            message = if (result.valid) "VP accepted" else "VP rejected",
            data = mapOf(
                "state"  to body.state,
                "valid"  to result.valid,
                "reason" to result.reason,
            ),
        )
    }

    // ── 3. Verifier polls for result ───────────────────────────────────────────

    /**
     * GET /api/v1/oidc/vp/result/{state}
     *
     * Verifier polls this endpoint after showing the QR to the Holder.
     * Returns pending / accepted / rejected.
     */
    @GetMapping("/vp/result/{state}")
    fun getResult(@PathVariable state: String): ApiResponse<Map<String, Any>> {
        val session = sessionStore.get(state)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found or expired")

        if (session.vpToken == null) {
            return ApiResponse(
                status = "200",
                message = "Pending — Holder has not submitted VP yet",
                data = mapOf("state" to state, "status" to "PENDING"),
            )
        }

        return ApiResponse(
            status = "200",
            message = if (session.verified == true) "VP verified" else "VP rejected",
            data = mapOf(
                "state"  to state,
                "status" to if (session.verified == true) "ACCEPTED" else "REJECTED",
                "valid"  to (session.verified ?: false),
                "reason" to (session.verifyReason ?: ""),
            ),
        )
    }

    // ── 4. OID4VP Discovery metadata ───────────────────────────────────────────

    /**
     * GET /api/v1/oidc/.well-known/openid-configuration
     *
     * Minimal OID4VP discovery document so Verifiers can auto-configure.
     */
    @GetMapping("/.well-known/openid-configuration")
    fun openidConfiguration(): Map<String, Any> {
        val base = "/api/v1/oidc"
        return mapOf(
            "issuer"                                  to "did:fabric:trustid:org1",
            "authorization_endpoint"                  to "$base/vp/request",
            "response_types_supported"                to listOf("vp_token"),
            "vp_formats_supported"                    to mapOf(
                "jwt_vp" to mapOf("alg_values_supported" to listOf("HMAC-SHA256")),
            ),
            "request_object_signing_alg_values_supported" to listOf("HMAC-SHA256"),
            "subject_types_supported"                 to listOf("pairwise"),
            "id_token_signing_alg_values_supported"   to listOf("HMAC-SHA256"),
        )
    }
}
