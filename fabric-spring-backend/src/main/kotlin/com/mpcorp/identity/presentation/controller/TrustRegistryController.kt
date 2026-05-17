package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * Trust Registry REST API — on-chain list of trusted credential issuers.
 *
 * Pattern: EBSI / eIDAS Trust Registry.
 * - CHIEF registers / revokes issuers (governance).
 * - Public read endpoint for verifiers (no auth required).
 *
 * Chaincode: RegisterIssuer, RevokeIssuer, IsTrustedIssuer, ListTrustedIssuers.
 */
@RestController
@RequestMapping("/api/v1/trust-registry")
class TrustRegistryController(
    private val fabricBridge: FabricLedgerBridge,
) {

    data class RegisterIssuerRequest(
        val did: String,
        val name: String,
        val role: String = "ISSUER",
        val scope: String = "EmploymentCredential",
    )

    /** Public endpoint — Verifier Portal and external tools can call without auth. */
    @GetMapping("/issuers")
    fun listIssuers(): List<Map<String, Any?>> = fabricBridge.listTrustedIssuers()

    @GetMapping("/issuers/{did}/trusted")
    fun checkTrusted(@PathVariable did: String): Map<String, Any> {
        val trusted = fabricBridge.isTrustedIssuer(did)
        return mapOf("did" to did, "trusted" to trusted)
    }

    @PostMapping("/issuers")
    @PreAuthorize("hasAnyRole('CHIEF','ADMIN')")
    fun registerIssuer(@RequestBody body: RegisterIssuerRequest): ApiResponse<Any> {
        val actor = SecurityContextHolder.getContext().authentication?.name ?: "system"
        fabricBridge.registerIssuer(
            did          = body.did,
            name         = body.name,
            role         = body.role,
            scope        = body.scope,
            registeredBy = actor,
        )
        return ApiResponse(status = "201", message = "Issuer registered in Trust Registry", data = mapOf("did" to body.did))
    }

    @DeleteMapping("/issuers/{did}")
    @PreAuthorize("hasAnyRole('CHIEF','ADMIN')")
    fun revokeIssuer(@PathVariable did: String): ApiResponse<Any> {
        val actor = SecurityContextHolder.getContext().authentication?.name ?: "system"
        fabricBridge.revokeIssuer(did = did, revokedBy = actor)
        return ApiResponse(status = "200", message = "Issuer revoked from Trust Registry", data = mapOf("did" to did))
    }
}
