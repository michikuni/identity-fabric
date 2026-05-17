package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.util.Base64

/**
 * Notarization Service — anchor any document's SHA-256 fingerprint on Hyperledger Fabric.
 *
 * Demo storyline: "Upload bất kỳ file → SHA-256 → UpsertRecord(recordType=DOCUMENT) on-chain.
 * Bất kỳ ai cũng có thể verify bằng cách gửi lại file — backend recompute hash, so với on-chain."
 *
 * Endpoints:
 *   POST /api/v1/notarization/notarize  — upload file (multipart) → anchor hash on-chain
 *   POST /api/v1/notarization/verify    — verify file integrity against on-chain record
 *   GET  /api/v1/notarization/{docId}   — get notarization metadata for a docId (public)
 */
@RestController
@RequestMapping("/api/v1/notarization")
class NotarizationController(
    private val fabricBridge: FabricLedgerBridge,
) {

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    /**
     * POST /api/v1/notarization/notarize
     * Multipart upload: field "file" + optional field "label" (human-readable document name).
     * Returns a docId (UUID) that can be used to retrieve / verify later.
     */
    @PostMapping("/notarize", consumes = ["multipart/form-data"])
    @PreAuthorize("isAuthenticated()")
    fun notarize(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("label", required = false) label: String?,
        @RequestParam("signerDid", required = false) signerDid: String?,
    ): ApiResponse<Map<String, Any>> {
        if (file.isEmpty) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File must not be empty")
        if (file.size > 50 * 1024 * 1024) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds 50 MB limit")

        val actor = SecurityContextHolder.getContext().authentication?.name ?: "system"
        val bytes = file.bytes
        val docHash = sha256Hex(bytes)
        val docId = java.util.UUID.randomUUID().toString()
        val filename = file.originalFilename ?: "unknown"
        val mimeType = file.contentType ?: "application/octet-stream"
        val now = java.time.Instant.now().toString()

        // Anchor on Fabric via the existing upsertRecord infrastructure.
        // keyFields carries non-PII metadata; dataHash is the SHA-256 of the file bytes.
        fabricBridge.upsertNotarizationRecord(
            docId      = docId,
            docHash    = docHash,
            filename   = filename,
            mimeType   = mimeType,
            label      = label ?: filename,
            signerDid  = signerDid ?: actor,
            updatedBy  = actor,
        )

        return ApiResponse(
            status  = "201",
            message = "Document notarized on Fabric",
            data    = mapOf(
                "docId"     to docId,
                "docHash"   to docHash,
                "filename"  to filename,
                "mimeType"  to mimeType,
                "label"     to (label ?: filename),
                "signerDid" to (signerDid ?: actor),
                "notarizedAt" to now,
                "sizeBytes" to bytes.size,
            )
        )
    }

    /**
     * POST /api/v1/notarization/verify
     * Body: { "docId": "...", "fileBase64": "<base64 encoded file bytes>" }
     * Recomputes SHA-256 of submitted bytes and compares with on-chain record.
     */
    @PostMapping("/verify")
    fun verifyByBase64(@RequestBody body: Map<String, String>): ApiResponse<Map<String, Any>> {
        val docId = body["docId"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "docId is required")
        val fileBase64 = body["fileBase64"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "fileBase64 is required")

        val bytes = try {
            Base64.getDecoder().decode(fileBase64)
        } catch (_: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "fileBase64 is not valid Base64")
        }

        val computedHash = sha256Hex(bytes)
        val record = fabricBridge.getNotarizationRecord(docId)
            ?: return ApiResponse(
                status  = "404",
                message = "No notarization record found for docId: $docId",
                data    = mapOf("docId" to docId, "verified" to false),
            )

        val onChainHash = record["dataHash"] as? String ?: ""
        val verified = computedHash.equals(onChainHash, ignoreCase = true)

        return ApiResponse(
            status  = "200",
            message = if (verified) "Document integrity VERIFIED" else "Document integrity FAILED — hash mismatch",
            data    = mapOf(
                "docId"        to docId,
                "verified"     to verified,
                "computedHash" to computedHash,
                "onChainHash"  to onChainHash,
                "record"       to record,
            )
        )
    }

    /**
     * GET /api/v1/notarization/{docId}
     * Retrieve notarization metadata from on-chain record (public — no auth required).
     */
    @GetMapping("/{docId}")
    fun getRecord(@PathVariable docId: String): ApiResponse<Any> {
        val record = fabricBridge.getNotarizationRecord(docId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No notarization record for docId: $docId")
        return ApiResponse(status = "200", message = "OK", data = record)
    }
}
