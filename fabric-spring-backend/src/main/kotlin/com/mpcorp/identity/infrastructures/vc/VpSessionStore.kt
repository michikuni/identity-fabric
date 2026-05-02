package com.mpcorp.identity.infrastructures.vc

import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory store for OID4VP Authorization Request sessions.
 *
 * Each session lives for [SESSION_TTL_SECONDS] seconds.
 * The Verifier creates a session (nonce + state), Flutter submits a VP Token
 * referencing the same state, and the Verifier polls for the result.
 */
@Component
class VpSessionStore {

    companion object {
        private const val SESSION_TTL_SECONDS = 300L
    }

    data class VpSession(
        val state: String,
        val nonce: String,
        val vcType: String,                  // EmploymentCredential | SalaryRangeCredential | PromotionCredential | TerminationCredential
        val requestedClaims: List<String>,   // fields the Verifier wants disclosed
        val createdAt: Instant = Instant.now(),
        var vpToken: String? = null,         // submitted by Holder
        var verified: Boolean? = null,
        var verifyReason: String? = null,
        var disclosedFields: Map<String, Any?> = emptyMap(), // actual fields Employee shared
    ) {
        fun isExpired() = Instant.now().isAfter(createdAt.plusSeconds(SESSION_TTL_SECONDS))
    }

    private val sessions = ConcurrentHashMap<String, VpSession>()

    fun create(vcType: String, requestedClaims: List<String>): VpSession {
        val session = VpSession(
            state  = UUID.randomUUID().toString(),
            nonce  = UUID.randomUUID().toString(),
            vcType = vcType,
            requestedClaims = requestedClaims,
        )
        sessions[session.state] = session
        return session
    }

    fun get(state: String): VpSession? = sessions[state]?.takeUnless { it.isExpired() }

    fun submitVp(state: String, vpToken: String): Boolean {
        val session = get(state) ?: return false
        session.vpToken = vpToken
        return true
    }

    fun saveResult(state: String, verified: Boolean, reason: String, disclosedFields: Map<String, Any?> = emptyMap()) {
        sessions[state]?.let {
            it.verified = verified
            it.verifyReason = reason
            it.disclosedFields = disclosedFields
        }
    }

    fun cleanup() {
        sessions.entries.removeIf { it.value.isExpired() }
    }
}
