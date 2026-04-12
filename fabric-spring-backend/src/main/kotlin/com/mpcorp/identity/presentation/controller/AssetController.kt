package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.infrastructures.fabric.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST API for Hyperledger Fabric asset-transfer chaincode.
 *
 * Base path: /api/v1/assets
 *
 * All endpoints are PUBLIC for easy Postman testing.
 * In production, add JWT protection via SecurityConfig.
 *
 * Endpoints:
 *   GET    /api/v1/assets          → GetAllAssets  (blockchain query)
 *   GET    /api/v1/assets/{id}     → ReadAsset     (blockchain query)
 *   POST   /api/v1/assets          → CreateAsset   (blockchain tx)
 *   PUT    /api/v1/assets/{id}     → UpdateAsset   (blockchain tx)
 *   DELETE /api/v1/assets/{id}     → DeleteAsset   (blockchain tx)
 *   PUT    /api/v1/assets/{id}/transfer → TransferAsset (blockchain tx)
 */
@RestController
@RequestMapping("/api/v1/assets")
class AssetController(private val fabricService: FabricAssetService) {

    /** Get all assets from blockchain world state */
    @GetMapping
    fun getAllAssets(): ResponseEntity<Map<String, Any>> {
        val assets = fabricService.getAllAssets()
        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "source" to "blockchain",
                "count" to assets.size,
                "data" to assets,
            )
        )
    }

    /** Get a single asset by ID */
    @GetMapping("/{assetId}")
    fun getAsset(@PathVariable assetId: String): ResponseEntity<Map<String, Any>> {
        val asset = fabricService.readAsset(assetId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                mapOf("success" to false, "message" to "Asset $assetId not found on blockchain")
            )
        return ResponseEntity.ok(
            mapOf("success" to true, "source" to "blockchain", "data" to asset)
        )
    }

    /** Check if asset exists */
    @GetMapping("/{assetId}/exists")
    fun assetExists(@PathVariable assetId: String): ResponseEntity<Map<String, Any>> {
        val exists = fabricService.assetExists(assetId)
        return ResponseEntity.ok(mapOf("assetId" to assetId, "exists" to exists))
    }

    /** Create a new asset — writes to blockchain */
    @PostMapping
    fun createAsset(@RequestBody request: CreateAssetRequest): ResponseEntity<Map<String, Any>> {
        val asset = fabricService.createAsset(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf(
                "success" to true,
                "message" to "Asset created on blockchain",
                "data" to asset,
            )
        )
    }

    /** Update an existing asset — writes to blockchain */
    @PutMapping("/{assetId}")
    fun updateAsset(
        @PathVariable assetId: String,
        @RequestBody request: UpdateAssetRequest,
    ): ResponseEntity<Map<String, Any>> {
        val asset = fabricService.updateAsset(assetId, request)
        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "message" to "Asset updated on blockchain",
                "data" to asset,
            )
        )
    }

    /** Delete an asset from blockchain */
    @DeleteMapping("/{assetId}")
    fun deleteAsset(@PathVariable assetId: String): ResponseEntity<Map<String, Any>> {
        fabricService.deleteAsset(assetId)
        return ResponseEntity.ok(
            mapOf("success" to true, "message" to "Asset $assetId deleted from blockchain")
        )
    }

    /** Transfer asset ownership to a new owner */
    @PutMapping("/{assetId}/transfer")
    fun transferAsset(
        @PathVariable assetId: String,
        @RequestBody request: TransferAssetRequest,
    ): ResponseEntity<Map<String, Any>> {
        val previousOwner = fabricService.transferAsset(assetId, request)
        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "message" to "Asset transferred",
                "assetId" to assetId,
                "previousOwner" to previousOwner,
                "newOwner" to request.newOwner,
            )
        )
    }
}
