package com.mpcorp.identity.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

/**
 * DIF Universal Resolver-compatible endpoint for did:fabric:trustid.
 *
 * Spec: https://w3c-ccg.github.io/did-resolution/
 * Any Universal Resolver driver can route did:fabric:trustid:* to this endpoint:
 *   GET /1.0/identifiers/{did}
 *
 * Response: DID Resolution Result JSON-LD with:
 *   - didDocument         — W3C DID Document
 *   - didDocumentMetadata — deactivated flag, created/updated timestamps
 *   - didResolutionMetadata — contentType, retrieved timestamp
 */
@RestController
@RequestMapping("/1.0/identifiers")
class UniversalResolverController(
    private val fabricBridge: FabricLedgerBridge,
    private val objectMapper: ObjectMapper,
) {

    @GetMapping(
        "/{did}",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun resolve(@PathVariable did: String): Map<String, Any?> {
        val doc = try {
            fabricBridge.resolveDid(did)
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "DID not found: $did — ${e.message}"
            )
        }

        val deactivated = doc.status == "REVOKED"
        val publicKeyJwk = try {
            @Suppress("UNCHECKED_CAST")
            objectMapper.readValue(doc.publicKeyJwk, Map::class.java) as Map<String, Any>
        } catch (_: Exception) {
            mapOf("rawJwk" to doc.publicKeyJwk)
        }

        val verificationMethodId = "${doc.did}#key-1"

        val didDocument = mapOf(
            "@context" to listOf(
                "https://www.w3.org/ns/did/v1",
                "https://w3id.org/security/suites/jws-2020/v1",
            ),
            "id"         to doc.did,
            "controller" to doc.controller,
            "verificationMethod" to listOf(
                mapOf(
                    "id"           to verificationMethodId,
                    "type"         to "JsonWebKey2020",
                    "controller"   to doc.did,
                    "publicKeyJwk" to publicKeyJwk,
                )
            ),
            "authentication"  to listOf(verificationMethodId),
            "assertionMethod" to listOf(verificationMethodId),
            "service" to listOf(
                mapOf(
                    "id"              to "${doc.did}#trustid-service",
                    "type"            to "TrustIDIdentityService",
                    "serviceEndpoint" to "https://trustid.example.com/api/v1",
                )
            ),
        )

        val didDocumentMetadata = buildMap<String, Any?> {
            put("deactivated", deactivated)
            doc.createdAt.takeIf { it.isNotBlank() }?.let { put("created", it) }
            doc.updatedAt.takeIf { it.isNotBlank() }?.let { put("updated", it) }
            if (deactivated) {
                doc.revokedAt?.let { put("deactivatedAt", it) }
                doc.revokeReason?.let { put("deactivationReason", it) }
            }
        }

        val didResolutionMetadata = mapOf(
            "contentType" to "application/did+ld+json",
            "retrieved"   to Instant.now().toString(),
        )

        return mapOf(
            "@context"              to "https://w3id.org/did-resolution/v1",
            "didDocument"           to didDocument,
            "didDocumentMetadata"   to didDocumentMetadata,
            "didResolutionMetadata" to didResolutionMetadata,
        )
    }
}
