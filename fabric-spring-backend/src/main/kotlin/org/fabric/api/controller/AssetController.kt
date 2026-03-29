package org.fabric.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.fabric.api.model.*
import org.fabric.api.service.AssetService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Asset Transfer", description = "CRUD and transfer operations on Fabric ledger assets")
class AssetController(private val assetService: AssetService) {

    // ── Init Ledger ───────────────────────────────────────────────────────────

    @PostMapping("/init")
    @Operation(
        summary = "Initialize the ledger",
        description = "Seeds the ledger with 6 sample assets. Only call once per network."
    )
    fun initLedger(): ResponseEntity<ApiResponse<Nothing>> {
        assetService.initLedger()
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Ledger initialized with sample assets")
        )
    }

    // ── Get All ───────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get all assets", description = "Returns all assets currently on the ledger")
    fun getAllAssets(): ResponseEntity<ApiResponse<List<Asset>>> {
        val assets = assetService.getAllAssets()
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Found ${assets.size} asset(s)", data = assets)
        )
    }

    // ── Get One ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get asset by ID", description = "Reads a single asset from the ledger by its ID")
    fun getAsset(
        @Parameter(description = "Asset ID", example = "asset1")
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Asset>> {
        val asset = assetService.getAsset(id)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Asset found", data = asset)
        )
    }

    // ── Exists ────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/exists")
    @Operation(summary = "Check if asset exists", description = "Returns true/false without throwing if missing")
    fun assetExists(
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Boolean>> {
        val exists = assetService.assetExists(id)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = if (exists) "Asset exists" else "Asset not found", data = exists)
        )
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new asset", description = "Creates a new asset on the ledger")
    @ApiResponses(
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Asset created"),
        io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Asset already exists")
    )
    fun createAsset(
        @Valid @RequestBody request: CreateAssetRequest
    ): ResponseEntity<ApiResponse<Asset>> {
        val asset = assetService.createAsset(
            id = request.id,
            color = request.color,
            size = request.size,
            owner = request.owner,
            appraisedValue = request.appraisedValue
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse(success = true, message = "Asset '${asset.id}' created", data = asset))
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Update an asset", description = "Overwrites all fields of an existing asset")
    fun updateAsset(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateAssetRequest
    ): ResponseEntity<ApiResponse<Asset>> {
        val asset = assetService.updateAsset(
            id = id,
            color = request.color,
            size = request.size,
            owner = request.owner,
            appraisedValue = request.appraisedValue
        )
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Asset '$id' updated", data = asset)
        )
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an asset", description = "Removes an asset from the ledger world state")
    fun deleteAsset(
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        assetService.deleteAsset(id)
        return ResponseEntity.ok(
            ApiResponse(success = true, message = "Asset '$id' deleted")
        )
    }

    // ── Transfer ──────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/transfer")
    @Operation(
        summary = "Transfer asset ownership",
        description = "Changes the owner of an asset and returns the previous owner"
    )
    fun transferAsset(
        @PathVariable id: String,
        @Valid @RequestBody request: TransferAssetRequest
    ): ResponseEntity<ApiResponse<TransferResult>> {
        val previousOwner = assetService.transferAsset(id, request.newOwner)
        val result = TransferResult(
            assetId = id,
            previousOwner = previousOwner,
            newOwner = request.newOwner
        )
        return ResponseEntity.ok(
            ApiResponse(
                success = true,
                message = "Asset '$id' transferred from '$previousOwner' to '${request.newOwner}'",
                data = result
            )
        )
    }
}
