package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.infrastructures.vc.StatusListService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * StatusListController — exposes W3C Status List 2021 credentials publicly.
 *
 * Verifiers follow the `credentialStatus.statusListCredential` URL embedded in
 * any TrustID-issued VC to land here. The response body is a signed
 * StatusList2021Credential VC (JSON) whose `credentialSubject.encodedList`
 * carries the gzip + base64url-encoded bitstring.
 *
 * Endpoints are unauthenticated by design — anyone holding a VC must be able
 * to check its revocation state, including external verifiers that have no
 * TrustID account.
 */
@RestController
@RequestMapping("/api/v1/status-list")
class StatusListController(
    private val statusListService: StatusListService,
) {

    /**
     * GET /api/v1/status-list/{listId}
     * Returns the signed StatusList2021Credential VC.
     */
    @GetMapping("/{listId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getStatusList(@PathVariable listId: String): ResponseEntity<String> {
        val vcJson = statusListService.getStatusListCredential(listId)
        return ResponseEntity.ok()
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .body(vcJson)
    }

    /**
     * GET /api/v1/status-list/{listId}/entry?index=42
     *
     * Convenience endpoint for the mobile wallet / verifier UI when they don't
     * want to decode the bitstring themselves. Returns `{revoked: bool}`.
     */
    @GetMapping("/{listId}/entry")
    fun checkEntry(
        @PathVariable listId: String,
        @RequestParam index: Long,
    ): Map<String, Any> {
        val revoked = statusListService.isRevoked(index, listId)
        return mapOf(
            "listId"          to listId,
            "statusListIndex" to index,
            "revoked"         to revoked,
            "status"          to if (revoked) "REVOKED" else "ACTIVE",
        )
    }
}
