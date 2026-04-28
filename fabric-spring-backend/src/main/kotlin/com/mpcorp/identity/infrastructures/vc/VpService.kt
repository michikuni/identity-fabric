package com.mpcorp.identity.infrastructures.vc

import com.fasterxml.jackson.databind.ObjectMapper
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import org.springframework.stereotype.Service

/**
 * VpService — builds OID4VP Authorization Requests and verifies submitted VP Tokens.
 *
 * VP Token format (W3C Verifiable Presentation, simplified):
 * {
 *   "@context": [...],
 *   "type": ["VerifiablePresentation"],
 *   "holder": "did:fabric:trustid:<id>",
 *   "verifiableCredential": [<VC with selective fields only>],
 *   "proof": { "type": "HMAC-SHA256", "nonce": "...", "proofValue": "..." }
 * }
 */
@Service
class VpService(
    private val objectMapper: ObjectMapper,
    private val vcIssuerService: VcIssuerService,
    private val sessionStore: VpSessionStore,
    private val employeeJpaRepository: EmployeeJpaRepository,
) {

    /**
     * Build the Authorization Request object that the Verifier sends to the Holder.
     * Based on OID4VP §5 — Authorization Request.
     */
    fun buildAuthorizationRequest(session: VpSessionStore.VpSession): Map<String, Any> {
        return mapOf(
            "response_type"          to "vp_token",
            "client_id"              to "did:fabric:trustid:org1",
            "nonce"                  to session.nonce,
            "state"                  to session.state,
            "presentation_definition" to buildPresentationDefinition(session),
            "response_uri"           to "/api/v1/oidc/vp/submit",
            "expires_in"             to 300,
        )
    }

    /**
     * Verify a submitted VP Token string.
     *
     * Steps:
     * 1. Parse VP JSON
     * 2. Verify nonce matches the session
     * 3. Extract the embedded VC(s) and verify each via VcIssuerService
     * 4. Check that all requestedClaims are present in the disclosed subject
     */
    @Suppress("UNCHECKED_CAST")
    fun verifyVpToken(vpTokenJson: String, session: VpSessionStore.VpSession): VcIssuerService.VcVerifyResult {
        return try {
            val vp = objectMapper.readValue(vpTokenJson, Map::class.java) as Map<String, Any>

            // 1. Check nonce
            val proof = vp["proof"] as? Map<*, *>
                ?: return VcIssuerService.VcVerifyResult(false, "VP missing proof")
            val vpNonce = proof["nonce"] as? String
                ?: return VcIssuerService.VcVerifyResult(false, "VP proof missing nonce")
            if (vpNonce != session.nonce) {
                return VcIssuerService.VcVerifyResult(false, "Nonce mismatch — possible replay attack")
            }

            // 2. Extract embedded VC list
            val vcList = vp["verifiableCredential"] as? List<*>
                ?: return VcIssuerService.VcVerifyResult(false, "VP missing verifiableCredential")
            if (vcList.isEmpty()) {
                return VcIssuerService.VcVerifyResult(false, "VP contains no credentials")
            }

            // 3. Verify each VC's proof using the original full VC from DB.
            // The submitted VC may have selective-disclosed (trimmed) credentialSubject,
            // so we look up the canonical VC by its `id` field and verify that instead.
            for (vcRaw in vcList) {
                @Suppress("UNCHECKED_CAST")
                val vcMap = vcRaw as? Map<String, Any> ?: continue
                val vcId = vcMap["id"] as? String
                val canonicalJson = if (vcId != null) findVcById(vcId) else null
                val vcJsonToVerify = canonicalJson ?: objectMapper.writeValueAsString(vcRaw)
                val result = vcIssuerService.verifyVC(vcJsonToVerify)
                if (!result.valid) return result
            }

            // 4. Check that all requestedClaims are disclosed
            val firstVc = vcList.first() as? Map<*, *>
            val subject = firstVc?.get("credentialSubject") as? Map<*, *> ?: emptyMap<String, Any>()
            val missing = session.requestedClaims.filter { claim ->
                !subject.containsKey(claim)
            }
            if (missing.isNotEmpty()) {
                return VcIssuerService.VcVerifyResult(false, "Missing required claims: $missing")
            }

            VcIssuerService.VcVerifyResult(true, "VP valid")
        } catch (e: Exception) {
            VcIssuerService.VcVerifyResult(false, "Parse error: ${e.message}")
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun findVcById(vcId: String): String? {
        for (employee in employeeJpaRepository.findAll()) {
            val candidates = listOfNotNull(
                employee.employmentVc,
                employee.terminationVc,
                employee.salaryRangeVc,
                employee.promotionVc,
            )
            for (vcJson in candidates) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val vc = objectMapper.readValue(vcJson, Map::class.java) as Map<String, Any>
                    if (vc["id"] == vcId) return vcJson
                } catch (_: Exception) {}
            }
        }
        return null
    }

    private fun buildPresentationDefinition(session: VpSessionStore.VpSession): Map<String, Any> {
        val fields = session.requestedClaims.map { claim ->
            mapOf("path" to listOf("$.credentialSubject.$claim"))
        }
        return mapOf(
            "id" to "pd-trustid-employment",
            "input_descriptors" to listOf(
                mapOf(
                    "id"          to "employment-credential",
                    "name"        to "TrustID Employment Credential",
                    "purpose"     to "Verify employment status",
                    "constraints" to mapOf(
                        "fields" to fields,
                        "limit_disclosure" to "required",
                    ),
                )
            ),
        )
    }
}
