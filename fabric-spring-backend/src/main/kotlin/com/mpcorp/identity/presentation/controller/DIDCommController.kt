package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.DIDCommMessageJpaEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.DIDCommMessageJpaRepository
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.sql.Timestamp

/**
 * DIDComm Messaging — simplified peer-to-peer encrypted channel between DIDs.
 *
 * This is a PoC implementation for demo: messages stored plaintext in MySQL.
 * Production would use DIDComm v2 full spec (ECDH-1PU authcrypt, JWM envelope).
 *
 * Endpoints:
 *   POST /didcomm/send          — send a message from one DID to another
 *   GET  /didcomm/inbox/{did}   — fetch inbox of a DID (unread by default)
 *   POST /didcomm/inbox/{did}/mark-read — mark all messages as read
 */
@RestController
@RequestMapping("/didcomm")
class DIDCommController(
    private val messageRepo: DIDCommMessageJpaRepository,
) {

    data class SendMessageRequest(
        /** DIDComm message type URI, e.g. "https://trustid.io/didcomm/1.0/credential-offer" */
        val type: String,
        val fromDid: String,
        val toDid: String,
        /** Arbitrary JSON body as a String — caller serializes before sending */
        val body: String,
    )

    data class MessageDto(
        val id: String,
        val type: String,
        val fromDid: String,
        val toDid: String,
        val body: String,
        val createdAt: String,
        val isRead: Boolean,
    )

    private fun DIDCommMessageJpaEntity.toDto() = MessageDto(
        id        = id.toString(),
        type      = type,
        fromDid   = fromDid,
        toDid     = toDid,
        body      = body,
        createdAt = createdAt.toInstant().toString(),
        isRead    = isRead,
    )

    /**
     * POST /didcomm/send
     * Send a DIDComm message. The sender must be authenticated; fromDid is validated
     * against the JWT principal in production — here we accept it from request body for
     * demo simplicity.
     */
    @PostMapping("/send")
    fun send(@RequestBody req: SendMessageRequest): ApiResponse<MessageDto> {
        if (req.fromDid.isBlank() || req.toDid.isBlank()) {
            throw org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST, "fromDid and toDid are required"
            )
        }
        val saved = messageRepo.save(
            DIDCommMessageJpaEntity(
                type      = req.type.ifBlank { "https://trustid.io/didcomm/1.0/message" },
                fromDid   = req.fromDid,
                toDid     = req.toDid,
                body      = req.body,
                createdAt = Timestamp(System.currentTimeMillis()),
            )
        )
        return ApiResponse(status = "201", message = "Message sent", data = saved.toDto())
    }

    /**
     * GET /didcomm/inbox/{did}?unreadOnly=true
     * Fetch messages addressed to a DID. Default: unread only.
     * Mark messages as read automatically when fetched (if markRead=true, default false).
     */
    @GetMapping("/inbox/{did}")
    @Transactional
    fun inbox(
        @PathVariable did: String,
        @RequestParam(defaultValue = "true") unreadOnly: Boolean,
        @RequestParam(defaultValue = "false") markRead: Boolean,
    ): ApiResponse<List<MessageDto>> {
        val messages = if (unreadOnly)
            messageRepo.findByToDidAndIsReadFalseOrderByCreatedAtDesc(did)
        else
            messageRepo.findByToDidOrderByCreatedAtDesc(did)

        if (markRead && messages.isNotEmpty()) {
            messageRepo.markAllReadForDid(did)
        }

        return ApiResponse(
            status  = "200",
            message = "OK",
            data    = messages.map { it.toDto() },
        )
    }

    /**
     * POST /didcomm/inbox/{did}/mark-read
     * Mark all unread messages for a DID as read.
     */
    @PostMapping("/inbox/{did}/mark-read")
    @Transactional
    fun markRead(@PathVariable did: String): ApiResponse<Map<String, Any>> {
        messageRepo.markAllReadForDid(did)
        return ApiResponse(status = "200", message = "Marked as read", data = mapOf("did" to did))
    }
}
