package org.fabric.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Asset stored on the Fabric ledger")
data class Asset(
    @Schema(description = "Unique asset identifier", example = "asset1")
    @JsonProperty("ID")
    val id: String,

    @Schema(description = "Asset color", example = "blue")
    @JsonProperty("Color")
    val color: String,

    @Schema(description = "Asset size", example = "5")
    @JsonProperty("Size")
    val size: Int,

    @Schema(description = "Current owner", example = "Tomoko")
    @JsonProperty("Owner")
    val owner: String,

    @Schema(description = "Appraised value", example = "300")
    @JsonProperty("AppraisedValue")
    val appraisedValue: Int
)

// ── Request DTOs ──────────────────────────────────────────────────────────────

@Schema(description = "Request body to create a new asset")
data class CreateAssetRequest(
    @field:NotBlank(message = "ID must not be blank")
    @field:Size(min = 1, max = 64)
    @Schema(example = "asset7")
    val id: String,

    @field:NotBlank(message = "Color must not be blank")
    @Schema(example = "purple")
    val color: String,

    @field:Min(1)
    @Schema(example = "10")
    val size: Int,

    @field:NotBlank(message = "Owner must not be blank")
    @Schema(example = "Alice")
    val owner: String,

    @field:Min(1)
    @Schema(example = "900")
    val appraisedValue: Int
)

@Schema(description = "Request body to update an existing asset")
data class UpdateAssetRequest(
    @field:NotBlank
    val color: String,

    @field:Min(1)
    val size: Int,

    @field:NotBlank
    val owner: String,

    @field:Min(1)
    val appraisedValue: Int
)

@Schema(description = "Request body to transfer asset ownership")
data class TransferAssetRequest(
    @field:NotBlank(message = "New owner must not be blank")
    @Schema(example = "Bob")
    val newOwner: String
)

// ── Response DTOs ─────────────────────────────────────────────────────────────

@Schema(description = "Generic API response wrapper")
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

@Schema(description = "Transfer result")
data class TransferResult(
    val assetId: String,
    val previousOwner: String,
    val newOwner: String
)
