package com.mpcorp.identity.infrastructures.fabric

import com.fasterxml.jackson.databind.ObjectMapper
import com.mpcorp.identity.domain.entity.ProfileEntity
import org.hyperledger.fabric.client.Gateway
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Instant

/**
 * IdentityLedgerService — writes identity audit records to Hyperledger Fabric.
 *
 * Strategy: Fire-and-forget (async).
 *   - MySQL is the source of truth.
 *   - Blockchain captures who changed what and when, with a hash proof.
 *   - If Fabric is down, we log the error but do NOT rollback MySQL.
 *
 * On-chain record structure (IdentityRecord):
 *   - employeeId   = employee's unique ID
 *   - recordType   = PROFILE | CONTRACT | PAYROLL
 *   - status       = ACTIVE | REVOKED | DELETED
 *   - keyFields    = non-sensitive JSON summary (name, gender, educationLevel, etc.)
 *   - dataHash     = SHA-256 of full off-chain data (for integrity verification)
 *   - action       = CREATE | UPDATE | DELETE
 *   - timestamp    = when the change happened
 *   - updatedBy    = actor (system for now)
 */
@Service
class IdentityLedgerService(
    private val gateway: Gateway,
    private val props: FabricProperties,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(IdentityLedgerService::class.java)

    // ── Public API called by UseCases ─────────────────────────────────────────

    @Async
    fun upsertProfileRecord(profile: ProfileEntity, action: String = "CREATE") {
        val employeeId = profile.employee?.id?.toString() ?: profile.id.toString()
        runCatching {
            submitUpsert(
                employeeId = employeeId,
                recordType = "PROFILE",
                status     = "ACTIVE",
                keyFields  = buildProfileKeyFields(profile),
                fullData   = objectMapper.writeValueAsString(profile),
                action     = action,
                updatedBy  = "system"
            )
            log.info("[Fabric] PROFILE record written for employee=$employeeId action=$action")
        }.onFailure { ex ->
            log.warn("[Fabric] Failed to write PROFILE record for employee=$employeeId — ${ex.message}")
            // Fire-and-forget: do NOT throw, MySQL commit already succeeded
        }
    }

    @Async
    fun deleteProfileRecord(employeeId: String) {
        runCatching {
            submitUpsert(
                employeeId = employeeId,
                recordType = "PROFILE",
                status     = "DELETED",
                keyFields  = "{}",
                fullData   = "{}",
                action     = "DELETE",
                updatedBy  = "system"
            )
            log.info("[Fabric] PROFILE DELETE record written for employee=$employeeId")
        }.onFailure { ex ->
            log.warn("[Fabric] Failed to write DELETE record for employee=$employeeId — ${ex.message}")
        }
    }

    // ── Internal: call chaincode ──────────────────────────────────────────────

    private fun submitUpsert(
        employeeId: String,
        recordType: String,
        status: String,
        keyFields: String,
        fullData: String,
        action: String,
        updatedBy: String
    ) {
        val contract = gateway
            .getNetwork(props.channelName)
            .getContract(props.chaincodeName)

        contract.submitTransaction(
            "UpsertRecord",
            employeeId,
            recordType,
            status,
            keyFields,
            sha256(fullData),          // only the hash of full data goes on-chain
            action,
            Instant.now().toString(),
            updatedBy
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a JSON object of non-sensitive key fields for the profile.
     * PII (identityNumber, phone, address) is EXCLUDED and stays off-chain.
     */
    private fun buildProfileKeyFields(profile: ProfileEntity): String {
        val fields = mapOf(
            "name"           to (profile.name ?: ""),
            "gender"         to (profile.gender ?: ""),
            "educationLevel" to (profile.educationLevel ?: ""),
            "major"          to (profile.major ?: ""),
            "expYears"       to (profile.expYears ?: 0),
            // identityNumber, phone, address → NOT included (PII stays off-chain)
        )
        return objectMapper.writeValueAsString(fields)
    }

    /** Returns the SHA-256 hex digest of the given input string. */
    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
