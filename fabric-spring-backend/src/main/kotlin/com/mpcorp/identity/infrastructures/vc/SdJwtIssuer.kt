package com.mpcorp.identity.infrastructures.vc

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * SdJwtIssuer — IETF SD-JWT (draft-ietf-oauth-selective-disclosure-jwt) issuer.
 *
 * ## Wire format
 *
 *     <jwt> "~" <disclosure_1> "~" <disclosure_2> ... "~" [<kb_jwt>]
 *
 * - `<jwt>` is a normal JWS (header.payload.signature). The payload includes:
 *   - `_sd`: array of base64url-encoded SHA-256 digests of each disclosure
 *   - `_sd_alg`: "sha-256"
 *   - Standard claims (iss, iat, exp, vct, sub, cnf) left in the clear
 * - `<disclosure_n>` is `base64url(json([salt, claim_name, claim_value]))`
 *
 * ## Holder presentation
 *
 * Holder picks which disclosures to reveal and concatenates them after the JWT:
 *
 *     header.payload.signature~d3~d7~d8~
 *
 * Trailing `~` marks the end of disclosures (no key-binding JWT in this PoC).
 *
 * ## Signing
 *
 * HMAC-SHA256 with a server secret distinct from the legacy VC secret.
 * Production should migrate to Ed25519 or ECDSA P-256 with proper key
 * management — the JWT header already declares `alg` so the wire format
 * survives the migration without holder changes.
 */
@Service
class SdJwtIssuer(
    private val objectMapper: ObjectMapper,
    @Value("\${sd-jwt.secret}") private val sdJwtSecret: String,
    @Value("\${vc.issuer-did:did:fabric:trustid:org1}") private val issuerDid: String,
) {

    private val secureRandom = SecureRandom()
    private val urlEncoder   = Base64.getUrlEncoder().withoutPadding()

    data class IssuedSdJwt(
        /** Compact SD-JWT: `header.payload.signature~d1~d2~...~` */
        val sdJwt: String,
        /** Each disclosure verbatim (base64url string) — issuer keeps these for the holder. */
        val disclosures: List<String>,
        /** Convenience: parsed claim list `[(name, value)]` for UI display in the holder's wallet. */
        val claims: List<Pair<String, Any?>>,
    )

    /**
     * Issue an SD-JWT VC where ALL `selectiveClaims` are individually disclosable
     * and `alwaysVisible` claims are embedded directly in the JWT payload.
     *
     * @param vct             "vct" claim per SD-JWT VC spec, e.g. "SkillCredential"
     * @param subjectDid      holder DID (`sub` claim)
     * @param selectiveClaims claims the holder can selectively disclose
     * @param alwaysVisible   claims always in the clear (e.g. issuer, vct)
     * @param expirySeconds   seconds from now until VC expires
     */
    fun issue(
        vct: String,
        subjectDid: String,
        selectiveClaims: Map<String, Any?>,
        alwaysVisible: Map<String, Any?> = emptyMap(),
        expirySeconds: Long = 365 * 24 * 3600,
        credentialStatus: Map<String, Any>? = null,
    ): IssuedSdJwt {
        require(selectiveClaims.isNotEmpty()) { "selectiveClaims must not be empty — use a regular VC instead" }

        val now = Instant.now()
        val exp = now.plus(expirySeconds, ChronoUnit.SECONDS)

        // 1. Build disclosure per selective claim: base64url(json([salt, name, value]))
        val disclosures = selectiveClaims.map { (name, value) ->
            buildDisclosure(name, value)
        }
        // 2. Compute SHA-256 digests of each disclosure (order randomised so the
        //    digest list does not leak the claim names embedded inside).
        val sdDigests = disclosures.map { sha256Base64Url(it) }.shuffled(secureRandom)

        // 3. Build JWT payload — clear claims + _sd array + _sd_alg
        val payload = buildMap<String, Any?> {
            put("iss",     issuerDid)
            put("vct",     vct)
            put("sub",     subjectDid)
            put("iat",     now.epochSecond)
            put("exp",     exp.epochSecond)
            put("_sd_alg", "sha-256")
            put("_sd",     sdDigests)
            credentialStatus?.let { put("status", it) }
            alwaysVisible.forEach { (k, v) -> if (v != null) put(k, v) }
        }

        // 4. Sign the JWT (HMAC-SHA256)
        val headerB64    = urlEncoder.encodeToString(
            objectMapper.writeValueAsBytes(mapOf("alg" to "HS256", "typ" to "sd+jwt"))
        )
        val payloadB64   = urlEncoder.encodeToString(objectMapper.writeValueAsBytes(payload))
        val signingInput = "$headerB64.$payloadB64"
        val signature    = urlEncoder.encodeToString(hmacSha256Raw(signingInput))
        val jwt          = "$signingInput.$signature"

        // 5. Concatenate JWT + disclosures (issuer's full SD-JWT — holder stores this)
        val sdJwt = jwt + "~" + disclosures.joinToString("~") + "~"

        return IssuedSdJwt(
            sdJwt       = sdJwt,
            disclosures = disclosures,
            claims      = selectiveClaims.entries.map { it.key to it.value },
        )
    }

    /**
     * Build a presentation from an issued SD-JWT by keeping only the disclosures
     * for the given claim names. Order of remaining disclosures is preserved.
     *
     * Holder typically does this in the wallet, but we provide a backend helper
     * for the demo and for the verifier portal to drive selective disclosure
     * end-to-end from a single REST call.
     */
    fun present(issued: IssuedSdJwt, claimsToReveal: Collection<String>): String {
        val keep = claimsToReveal.toSet()
        val jwt = issued.sdJwt.substringBefore("~")

        val kept = issued.disclosures.filter { d ->
            val parsed = parseDisclosure(d) ?: return@filter false
            parsed.second in keep
        }
        return jwt + "~" + kept.joinToString("~") + "~"
    }

    /**
     * Decode a disclosure into (salt, claim_name, claim_value).
     * Returns null on malformed input.
     */
    fun parseDisclosure(disclosureB64: String): Triple<String, String, Any?>? {
        return try {
            val bytes = Base64.getUrlDecoder().decode(disclosureB64)
            @Suppress("UNCHECKED_CAST")
            val arr = objectMapper.readValue(bytes, List::class.java) as List<Any?>
            if (arr.size != 3) return null
            Triple(arr[0] as String, arr[1] as String, arr[2])
        } catch (_: Exception) {
            null
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun buildDisclosure(name: String, value: Any?): String {
        val salt = ByteArray(16).also { secureRandom.nextBytes(it) }
        val saltStr = urlEncoder.encodeToString(salt)
        val arr = listOf(saltStr, name, value)
        return urlEncoder.encodeToString(objectMapper.writeValueAsBytes(arr))
    }

    fun sha256Base64Url(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return urlEncoder.encodeToString(digest)
    }

    fun hmacSha256Raw(data: String): ByteArray {
        val key = SecretKeySpec(sdJwtSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256").apply { init(key) }
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }
}
