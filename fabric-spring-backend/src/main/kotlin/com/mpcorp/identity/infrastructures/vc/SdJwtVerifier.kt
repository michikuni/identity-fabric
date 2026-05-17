package com.mpcorp.identity.infrastructures.vc

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

/**
 * SdJwtVerifier — verifies an SD-JWT presentation per IETF draft-ietf-oauth-sd-jwt.
 *
 * Verification steps:
 *   1. Split presentation on `~` → JWT + revealed disclosures + (optional KB-JWT)
 *   2. Split JWT on `.` → header.payload.signature
 *   3. Recompute HMAC(header.payload) → compare to signature
 *   4. For each revealed disclosure, recompute SHA-256, base64url → must be present
 *      in `_sd` array of payload (otherwise issuer didn't authorize this claim)
 *   5. Check `iat`/`exp` if present
 *   6. Check `status` (Status List 2021) revocation if present
 *   7. Reconstruct disclosed claims and return them
 */
@Service
class SdJwtVerifier(
    private val objectMapper: ObjectMapper,
    private val sdJwtIssuer: SdJwtIssuer,
    private val statusListService: StatusListService,
) {

    private val urlDecoder = Base64.getUrlDecoder()

    data class SdJwtVerifyResult(
        val valid: Boolean,
        val reason: String,
        val disclosedClaims: Map<String, Any?> = emptyMap(),
        val alwaysVisible: Map<String, Any?>   = emptyMap(),
        val issuer: String?  = null,
        val subject: String? = null,
        val vct: String?     = null,
    )

    /**
     * Verify an SD-JWT presentation string.
     *
     * @param presentation  full SD-JWT: `header.payload.signature~d1~d2~...~`
     * @param requireClaims optional whitelist — verification fails if any required
     *                      claim wasn't disclosed
     */
    fun verify(presentation: String, requireClaims: Collection<String> = emptyList()): SdJwtVerifyResult {
        return try {
            val parts = presentation.split("~")
            if (parts.size < 2) return fail("Malformed SD-JWT — missing tilde separator")

            val jwt = parts[0]
            // Trailing empty piece after final `~` plus optional KB-JWT slot.
            val disclosures = parts.drop(1)
                .filter { it.isNotEmpty() }
                .filter { !it.contains('.') } // exclude KB-JWT (form: jwt-shaped)

            // 1. Verify JWT signature
            val jwtParts = jwt.split(".")
            if (jwtParts.size != 3) return fail("Invalid JWT structure — expected 3 parts")
            val (headerB64, payloadB64, signatureB64) = jwtParts
            val expectedSig = sdJwtIssuer.hmacSha256Raw("$headerB64.$payloadB64")
            val actualSig   = try { urlDecoder.decode(signatureB64) } catch (_: Exception) {
                return fail("Signature is not valid base64url")
            }
            if (!MessageDigest.isEqual(expectedSig, actualSig)) {
                return fail("Signature mismatch — VC may be tampered")
            }

            // 2. Parse payload
            val payloadBytes = urlDecoder.decode(payloadB64)
            @Suppress("UNCHECKED_CAST")
            val payload = objectMapper.readValue(payloadBytes, Map::class.java) as Map<String, Any?>

            // 3. Check expiry
            (payload["exp"] as? Number)?.toLong()?.let { exp ->
                if (Instant.now().epochSecond > exp) {
                    return fail("SD-JWT expired at ${Instant.ofEpochSecond(exp)}")
                }
            }

            // 4. Resolve disclosures against _sd array
            val sdDigests = (payload["_sd"] as? List<*>)
                ?.filterIsInstance<String>()?.toSet() ?: emptySet()
            if (sdDigests.isEmpty() && disclosures.isNotEmpty()) {
                return fail("Payload missing _sd array but presentation contains disclosures")
            }

            val disclosedClaims = mutableMapOf<String, Any?>()
            for (d in disclosures) {
                val digest = sdJwtIssuer.sha256Base64Url(d)
                if (digest !in sdDigests) {
                    return fail("Disclosure not endorsed by issuer (digest missing from _sd)")
                }
                val parsed = sdJwtIssuer.parseDisclosure(d)
                    ?: return fail("Malformed disclosure: $d")
                disclosedClaims[parsed.second] = parsed.third
            }

            // 5. Check required claims
            val missing = requireClaims.filter { it !in disclosedClaims.keys }
            if (missing.isNotEmpty()) {
                return fail("Missing required disclosed claims: $missing")
            }

            // 6. Status List 2021 revocation check (if status present)
            @Suppress("UNCHECKED_CAST")
            val status = payload["status"] as? Map<String, Any?>
            if (status != null && status["type"] == "StatusList2021Entry") {
                val listId = (status["statusListCredential"] as? String)?.substringAfterLast("/")
                val index  = (status["statusListIndex"] as? String)?.toLongOrNull()
                if (listId != null && index != null && statusListService.isRevoked(index, listId)) {
                    return fail("VC revoked (status list $listId index $index)")
                }
            }

            // 7. Compose always-visible claims (payload minus SD machinery)
            val visible = payload.filterKeys { it !in SD_MACHINERY_KEYS }

            SdJwtVerifyResult(
                valid           = true,
                reason          = "Valid",
                disclosedClaims = disclosedClaims,
                alwaysVisible   = visible,
                issuer          = payload["iss"] as? String,
                subject         = payload["sub"] as? String,
                vct             = payload["vct"] as? String,
            )
        } catch (e: Exception) {
            fail("Parse error: ${e.message}")
        }
    }

    private fun fail(reason: String) = SdJwtVerifyResult(false, reason)

    companion object {
        private val SD_MACHINERY_KEYS = setOf("_sd", "_sd_alg")
    }
}
