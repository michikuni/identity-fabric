package org.fabric.api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import org.fabric.api.config.FabricProperties
import org.fabric.api.exception.AssetAlreadyExistsException
import org.fabric.api.exception.AssetNotFoundException
import org.fabric.api.exception.FabricTransactionException
import org.fabric.api.model.Asset
import org.fabric.api.model.TransferResult
import org.fabric.api.websocket.EventType
import org.fabric.api.websocket.FabricEvent
import org.fabric.api.websocket.FabricEventPublisher
import org.hyperledger.fabric.client.EndorseException
import org.hyperledger.fabric.client.Gateway
import org.hyperledger.fabric.client.Network
import org.hyperledger.fabric.client.SubmitException
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class AssetService(
    private val gateway: Gateway,
    private val props: FabricProperties,
    private val objectMapper: ObjectMapper,
    private val eventPublisher: FabricEventPublisher
) {

    private val network: Network by lazy { gateway.getNetwork(props.channelName) }
    private val contract by lazy { network.getContract(props.chaincodeName) }

    // ── Init ──────────────────────────────────────────────────────────────────

    fun initLedger() {
        log.info { "Submitting InitLedger transaction" }
        runCatching {
            contract.submitTransaction("InitLedger")
        }.onFailure { handleFabricError("InitLedger", it) }
        log.info { "InitLedger committed successfully" }
        eventPublisher.publish(FabricEvent(EventType.INIT_LEDGER, assetId = null, payload = null))
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    fun getAllAssets(): List<Asset> {
        log.debug { "Evaluating GetAllAssets" }
        val result = runCatching {
            contract.evaluateTransaction("GetAllAssets")
        }.getOrElse { handleFabricError("GetAllAssets", it) }
        return objectMapper.readValue(result)
    }

    fun getAsset(id: String): Asset {
        log.debug { "Evaluating ReadAsset($id)" }
        val result = runCatching {
            contract.evaluateTransaction("ReadAsset", id)
        }.getOrElse { e ->
            if (e.message?.contains("does not exist") == true) throw AssetNotFoundException(id)
            handleFabricError("ReadAsset", e)
        }
        return objectMapper.readValue(result)
    }

    fun assetExists(id: String): Boolean {
        log.debug { "Evaluating AssetExists($id)" }
        val result = runCatching {
            contract.evaluateTransaction("AssetExists", id)
        }.getOrElse { handleFabricError("AssetExists", it) }
        return String(result).trim().equals("true", ignoreCase = true)
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    fun createAsset(id: String, color: String, size: Int, owner: String, appraisedValue: Int): Asset {
        log.info { "Submitting CreateAsset($id)" }
        if (assetExists(id)) throw AssetAlreadyExistsException(id)
        runCatching {
            contract.submitTransaction(
                "CreateAsset", id, color, size.toString(), owner, appraisedValue.toString()
            )
        }.onFailure { handleFabricError("CreateAsset", it) }
        val asset = getAsset(id)
        eventPublisher.publish(FabricEvent(EventType.ASSET_CREATED, id, asset))
        return asset
    }

    fun updateAsset(id: String, color: String, size: Int, owner: String, appraisedValue: Int): Asset {
        log.info { "Submitting UpdateAsset($id)" }
        if (!assetExists(id)) throw AssetNotFoundException(id)
        runCatching {
            contract.submitTransaction(
                "UpdateAsset", id, color, size.toString(), owner, appraisedValue.toString()
            )
        }.onFailure { handleFabricError("UpdateAsset", it) }
        val asset = getAsset(id)
        eventPublisher.publish(FabricEvent(EventType.ASSET_UPDATED, id, asset))
        return asset
    }

    fun deleteAsset(id: String) {
        log.info { "Submitting DeleteAsset($id)" }
        if (!assetExists(id)) throw AssetNotFoundException(id)
        runCatching {
            contract.submitTransaction("DeleteAsset", id)
        }.onFailure { handleFabricError("DeleteAsset", it) }
        eventPublisher.publish(FabricEvent(EventType.ASSET_DELETED, id, mapOf("id" to id)))
    }

    fun transferAsset(id: String, newOwner: String): String {
        log.info { "Submitting TransferAsset($id -> $newOwner)" }
        if (!assetExists(id)) throw AssetNotFoundException(id)
        val result = runCatching {
            contract.submitTransaction("TransferAsset", id, newOwner)
        }.getOrElse { handleFabricError("TransferAsset", it) }
        val previousOwner = String(result).trim()
        eventPublisher.publish(
            FabricEvent(
                type = EventType.ASSET_TRANSFERRED,
                assetId = id,
                payload = TransferResult(id, previousOwner, newOwner)
            )
        )
        return previousOwner
    }

    // ── Error handling ────────────────────────────────────────────────────────

    private fun handleFabricError(fn: String, t: Throwable): Nothing {
        val msg = when (t) {
            is EndorseException -> "Endorsement failed for $fn: ${t.message}"
            is SubmitException -> "Submit failed for $fn: ${t.message}"
            else -> "Fabric error in $fn: ${t.message}"
        }
        log.error(t) { msg }
        throw FabricTransactionException(msg, t)
    }
}
