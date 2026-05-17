package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.ContractJpaRepository
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.util.Base64

/**
 * E-sign Contract endpoints — on-chain ECDSA P-256 signature anchoring.
 *
 * Flow:
 *   1. Employee wallet computes SHA-256(PDF bytes) → docHash
 *   2. Signs docHash with ECDSA P-256 private key (biometric-protected in Flutter SecureStorage)
 *   3. Sends signatureBase64 + docHash + signerDid to POST /api/v1/contracts/{id}/sign
 *   4. Backend anchors { contractId, signerDid, signatureBase64, docHash, timestamp } on Fabric
 *   5. GET /api/v1/contracts/{id}/signatures returns all anchored signatures
 *
 * Verification:
 *   - Resolve signerDid → get publicKeyJwk → verify ECDSA P-256 signature against docHash
 *   - Compare docHash against SHA-256 of current contract PDF bytes
 */
@RestController
@RequestMapping("/api/v1/contracts")
class ContractSignatureController(
    private val fabricBridge: FabricLedgerBridge,
    private val contractJpaRepository: ContractJpaRepository,
) {

    data class SignContractRequest(
        /** Base64-encoded ECDSA P-256 signature over SHA-256(docBytes) */
        val signatureBase64: String,
        /** Hex-encoded SHA-256 of the document bytes that were signed */
        val docHash: String,
        /** DID of the signer, e.g. "did:fabric:trustid:42" */
        val signerDid: String,
    )

    data class VerifySignatureRequest(
        /** Raw document bytes (Base64-encoded) to re-hash and compare */
        val documentBase64: String,
    )

    @PostMapping("/{contractId}/sign")
    @PreAuthorize("isAuthenticated()")
    fun signContract(
        @PathVariable contractId: Long,
        @RequestBody body: SignContractRequest,
    ): ApiResponse<Any> {
        val actor = SecurityContextHolder.getContext().authentication?.name ?: "system"

        contractJpaRepository.findById(contractId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Contract $contractId not found")
        }

        if (body.signatureBase64.isBlank() || body.docHash.isBlank() || body.signerDid.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "signatureBase64, docHash, and signerDid are required")
        }

        fabricBridge.recordSignature(
            contractId      = contractId.toString(),
            signerDid       = body.signerDid,
            signatureBase64 = body.signatureBase64,
            docHash         = body.docHash,
            updatedBy       = actor,
        )

        return ApiResponse(
            status  = "200",
            message = "Contract signed and anchored on Fabric",
            data    = mapOf(
                "contractId"  to contractId,
                "signerDid"   to body.signerDid,
                "docHash"     to body.docHash,
                "anchoredBy"  to actor,
            )
        )
    }

    @GetMapping("/{contractId}/signatures")
    @PreAuthorize("isAuthenticated()")
    fun getSignatures(@PathVariable contractId: Long): ApiResponse<Any> {
        contractJpaRepository.findById(contractId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Contract $contractId not found")
        }
        val sigs = fabricBridge.getSignatures(contractId.toString())
        return ApiResponse(status = "200", message = "OK", data = sigs)
    }

    /** Helper: compute SHA-256 of Base64-decoded document bytes — useful for clients to pre-compute docHash. */
    @PostMapping("/hash")
    fun computeDocHash(@RequestBody body: Map<String, String>): ApiResponse<Any> {
        val documentBase64 = body["documentBase64"]
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "documentBase64 is required")
        val bytes = try {
            Base64.getDecoder().decode(documentBase64)
        } catch (_: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "documentBase64 is not valid Base64")
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes).joinToString("") { "%02x".format(it) }
        return ApiResponse(status = "200", message = "OK", data = mapOf("docHash" to hash, "sizeBytes" to bytes.size))
    }
}
