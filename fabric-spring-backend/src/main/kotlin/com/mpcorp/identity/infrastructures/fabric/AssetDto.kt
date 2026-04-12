package com.mpcorp.identity.infrastructures.fabric

/**
 * Mirrors the Asset struct defined in the chaincode (Asset.java).
 *
 * Field names must match the JSON keys produced by Genson serialization in the chaincode,
 * which uses camelCase by default.
 */
data class AssetDto(
    val assetId: String = "",
    val color: String = "",
    val size: Int = 0,
    val owner: String = "",
    val appraisedValue: Int = 0,
)

/** Request body for creating a new asset. */
data class CreateAssetRequest(
    val assetId: String,
    val color: String,
    val size: Int,
    val owner: String,
    val appraisedValue: Int,
)

/** Request body for updating an existing asset. */
data class UpdateAssetRequest(
    val color: String,
    val size: Int,
    val owner: String,
    val appraisedValue: Int,
)

/** Request body for transferring asset ownership. */
data class TransferAssetRequest(
    val newOwner: String,
)
