package com.mpcorp.identity.infrastructures.fabric

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.hyperledger.fabric.client.Gateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service layer that wraps all Hyperledger Fabric chaincode interactions.
 *
 * Chaincode: asset-transfer (Java)
 * Channel  : mychannel
 *
 * EVALUATE  → read-only query (no transaction on ledger)
 * SUBMIT    → creates/modifies ledger state (goes through orderer)
 */
@Service
class FabricAssetService(
    private val gateway: Gateway,
    private val props: FabricProperties,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(FabricAssetService::class.java)

    private fun contract() = gateway
        .getNetwork(props.channelName)
        .getContract(props.chaincodeName)

    // ── Read ──────────────────────────────────────────────────────────────────

    /** Returns all assets stored on the ledger. */
    fun getAllAssets(): List<AssetDto> {
        log.debug("EVALUATE GetAllAssets")
        val resultBytes = contract().evaluateTransaction("GetAllAssets")
        val json = String(resultBytes)
        return objectMapper.readValue<List<AssetDto>>(json)
    }

    /** Returns a single asset by ID, or null if not found. */
    fun readAsset(assetId: String): AssetDto? {
        log.debug("EVALUATE ReadAsset id={}", assetId)
        return runCatching {
            val resultBytes = contract().evaluateTransaction("ReadAsset", assetId)
            objectMapper.readValue<AssetDto>(String(resultBytes))
        }.getOrElse { ex ->
            log.warn("ReadAsset failed for id={}: {}", assetId, ex.message)
            null
        }
    }

    /** Checks whether an asset exists. */
    fun assetExists(assetId: String): Boolean {
        log.debug("EVALUATE AssetExists id={}", assetId)
        val result = contract().evaluateTransaction("AssetExists", assetId)
        return String(result).trim().equals("true", ignoreCase = true)
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /** Creates a new asset on the ledger. */
    fun createAsset(req: CreateAssetRequest): AssetDto {
        log.info("SUBMIT CreateAsset id={}", req.assetId)
        val result = contract().submitTransaction(
            "CreateAsset",
            req.assetId,
            req.color,
            req.size.toString(),
            req.owner,
            req.appraisedValue.toString(),
        )
        return objectMapper.readValue<AssetDto>(String(result))
    }

    /** Updates all fields of an existing asset. */
    fun updateAsset(assetId: String, req: UpdateAssetRequest): AssetDto {
        log.info("SUBMIT UpdateAsset id={}", assetId)
        val result = contract().submitTransaction(
            "UpdateAsset",
            assetId,
            req.color,
            req.size.toString(),
            req.owner,
            req.appraisedValue.toString(),
        )
        return objectMapper.readValue<AssetDto>(String(result))
    }

    /** Deletes an asset from the ledger (history is preserved). */
    fun deleteAsset(assetId: String) {
        log.info("SUBMIT DeleteAsset id={}", assetId)
        contract().submitTransaction("DeleteAsset", assetId)
    }

    /** Transfers asset ownership; returns the previous owner name. */
    fun transferAsset(assetId: String, req: TransferAssetRequest): String {
        log.info("SUBMIT TransferAsset id={} newOwner={}", assetId, req.newOwner)
        val result = contract().submitTransaction("TransferAsset", assetId, req.newOwner)
        return String(result)
    }
}
