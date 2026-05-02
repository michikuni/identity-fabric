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

    data class VpVerifyResult(
        val valid: Boolean,
        val reason: String,
        val disclosedFields: Map<String, Any?> = emptyMap(),
    )

    /**
     * Verify a submitted VP Token string.
     *
     * Steps:
     * 1. Parse VP JSON
     * 2. Verify nonce matches the session
     * 3. Extract the embedded VC(s) and verify each via VcIssuerService
     * 4. Check that all requestedClaims are present in the disclosed subject
     * 5. Return the actual disclosed credentialSubject fields to be shown to Verifier
     */
    @Suppress("UNCHECKED_CAST")
    fun verifyVpToken(vpTokenJson: String, session: VpSessionStore.VpSession): VpVerifyResult {
        fun fail(reason: String) = VpVerifyResult(false, reason)

        return try {
            val vp = objectMapper.readValue(vpTokenJson, Map::class.java) as Map<String, Any>

            // 1. Check nonce
            val proof = vp["proof"] as? Map<*, *>
                ?: return fail("VP missing proof")
            val vpNonce = proof["nonce"] as? String
                ?: return fail("VP proof missing nonce")
            if (vpNonce != session.nonce) {
                return fail("Nonce mismatch — possible replay attack")
            }

            // 2. Extract embedded VC list
            val vcList = vp["verifiableCredential"] as? List<*>
                ?: return fail("VP missing verifiableCredential")
            if (vcList.isEmpty()) {
                return fail("VP contains no credentials")
            }

            // 3. Verify each VC's proof using the original full VC from DB.
            // The submitted VC may have selective-disclosed (trimmed) credentialSubject,
            // so we look up the canonical VC by its `id` field and verify that instead.
            // We also keep a reference to the canonical VC's full subject to extract
            // disclosed field values (avoids any frontend serialization quirks).
            var canonicalSubject: Map<String, Any?> = emptyMap()
            for (vcRaw in vcList) {
                val vcMap = vcRaw as? Map<*, *> ?: continue
                val vcId = vcMap["id"] as? String
                val canonicalJson = if (vcId != null) findVcById(vcId) else null
                val vcJsonToVerify = canonicalJson ?: objectMapper.writeValueAsString(vcRaw)
                val result = vcIssuerService.verifyVC(vcJsonToVerify)
                if (!result.valid) return fail(result.reason)
                if (canonicalJson != null && canonicalSubject.isEmpty()) {
                    val canonicalVc = objectMapper.readValue(canonicalJson, Map::class.java)
                    canonicalSubject = (canonicalVc["credentialSubject"] as? Map<*, *>)
                        ?.entries?.associate { (k, v) -> k.toString() to v } ?: emptyMap()
                }
            }

            // 4. Check VC type matches the session's expected vcType
            val firstVc = vcList.first() as? Map<*, *>
                ?: return fail("Missing VC")
            val vcTypes = firstVc["type"] as? List<*>
                ?: return fail("VC missing type")
            if (!vcTypes.contains(session.vcType)) {
                return fail("VC type mismatch: expected ${session.vcType}, got $vcTypes")
            }

            // 5. Check that all requestedClaims are disclosed in the submitted VP
            val submittedSubject = (firstVc["credentialSubject"] as? Map<*, *>)
                ?.entries?.associate { (k, v) -> k.toString() to v } ?: emptyMap()
            val missing = session.requestedClaims.filter { claim -> !submittedSubject.containsKey(claim) }
            if (missing.isNotEmpty()) {
                return fail("Credential không hợp lệ: thiếu các trường yêu cầu ${missing}")
            }

            // 6. Build disclosed fields map — prefer canonical VC values (more reliable),
            // fall back to submitted values if canonical not found.
            // Use mutableMapOf so Jackson serializes it explicitly as a JSON object (not stripped).
            val disclosed = mutableMapOf<String, Any?>()
            for (claim in session.requestedClaims) {
                val value = canonicalSubject[claim] ?: submittedSubject[claim]
                if (value != null) {
                    disclosed[claim] = value
                }
            }

            VpVerifyResult(true, "VP valid", disclosed)
        } catch (e: Exception) {
            fail("Parse error: ${e.message}")
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
        val (descriptorId, descriptorName, purpose) = when (session.vcType) {
            "EmploymentCredential"  -> Triple("pd-trustid-employment",  "TrustID Employment Credential",   "Verify employment status")
            "SalaryRangeCredential" -> Triple("pd-trustid-salary",      "TrustID Salary Range Credential", "Verify salary band")
            "PromotionCredential"   -> Triple("pd-trustid-promotion",   "TrustID Promotion Credential",    "Verify promotion record")
            "TerminationCredential" -> Triple("pd-trustid-termination", "TrustID Termination Credential",  "Verify termination record")
            else -> throw IllegalArgumentException("Unsupported vcType: ${session.vcType}")
        }
        return mapOf(
            "id" to descriptorId,
            "input_descriptors" to listOf(
                mapOf(
                    "id"          to descriptorId,
                    "name"        to descriptorName,
                    "purpose"     to purpose,
                    "schema"      to mapOf("vcType" to session.vcType),
                    "constraints" to mapOf(
                        "fields" to fields,
                        "limit_disclosure" to "required",
                    ),
                )
            ),
        )
    }
}
