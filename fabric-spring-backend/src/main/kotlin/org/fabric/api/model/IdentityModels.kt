package org.fabric.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

// ── On-chain record (mirrors IdentityRecord.java in chaincode) ────────────────

@Schema(description = "Identity record stored on the Fabric ledger")
data class IdentityRecord(
    @Schema(description = "Composite key: '{recordType}:{employeeId}'", example = "profile:123")
    @JsonProperty("recordId")
    val recordId: String,

    @Schema(description = "Unique employee identifier", example = "123")
    @JsonProperty("employeeId")
    val employeeId: String,

    @Schema(description = "PROFILE | CONTRACT | PAYROLL")
    @JsonProperty("recordType")
    val recordType: String,

    @Schema(description = "ACTIVE | REVOKED | DELETED")
    @JsonProperty("status")
    val status: String,

    @Schema(description = "JSON of non-sensitive summary fields")
    @JsonProperty("keyFields")
    val keyFields: String,

    @Schema(description = "SHA-256 hex digest of the full off-chain data payload")
    @JsonProperty("dataHash")
    val dataHash: String,

    @Schema(description = "CREATE | UPDATE | DELETE")
    @JsonProperty("action")
    val action: String,

    @Schema(description = "ISO-8601 UTC timestamp")
    @JsonProperty("timestamp")
    val timestamp: String,

    @Schema(description = "Actor who made the change")
    @JsonProperty("updatedBy")
    val updatedBy: String,
)

// ── Request DTOs ──────────────────────────────────────────────────────────────

@Schema(description = "Request to upsert an identity record on the ledger")
data class UpsertIdentityRecordRequest(
    @field:NotBlank
    @Schema(example = "emp-123")
    val employeeId: String,

    @field:NotBlank
    @Schema(allowableValues = ["PROFILE", "CONTRACT", "PAYROLL"])
    val recordType: String,

    @field:NotBlank
    @Schema(allowableValues = ["ACTIVE", "REVOKED", "DELETED"], example = "ACTIVE")
    val status: String,

    @Schema(description = "Non-sensitive JSON summary of the record", example = """{"name":"Nguyen Van A","gender":"M"}""")
    val keyFields: String = "{}",

    @field:NotBlank
    @Schema(description = "SHA-256 hex of full off-chain payload")
    val dataHash: String,

    @field:NotBlank
    @Schema(allowableValues = ["CREATE", "UPDATE", "DELETE"], example = "CREATE")
    val action: String,

    @Schema(example = "system")
    val updatedBy: String = "system",
)

@Schema(description = "Request to delete an identity record from the ledger")
data class DeleteIdentityRecordRequest(
    @field:NotBlank val employeeId: String,
    @field:NotBlank val recordType: String,
    val updatedBy: String = "system",
)

@Schema(description = "Request body for InitLedger")
data class InitLedgerRequest(val force: Boolean = false)

// ── Response DTOs ─────────────────────────────────────────────────────────────

@Schema(description = "Generic API response wrapper")
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
)

@Schema(description = "Result of on-chain hash verification against off-chain MySQL data")
data class VerifyRecordResponse(
    @Schema(description = "true if the provided hash matches the hash stored on-chain")
    @JsonProperty("valid")
    val valid: Boolean,

    @Schema(description = "Human-readable explanation of the verification result")
    @JsonProperty("reason")
    val reason: String?,

    @Schema(description = "Composite key on ledger: '{recordType}:{employeeId}'")
    @JsonProperty("recordId")
    val recordId: String,

    @Schema(description = "SHA-256 hash stored immutably on-chain at last write time")
    @JsonProperty("storedHash")
    val storedHash: String,

    @Schema(description = "SHA-256 hash computed from current off-chain data (provided by caller)")
    @JsonProperty("providedHash")
    val providedHash: String,

    @Schema(description = "ISO-8601 timestamp of the on-chain record")
    @JsonProperty("timestamp")
    val timestamp: String,
)