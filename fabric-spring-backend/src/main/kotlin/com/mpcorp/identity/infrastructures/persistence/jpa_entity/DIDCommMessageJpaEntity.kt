package com.mpcorp.identity.infrastructures.persistence.jpa_entity

import jakarta.persistence.*
import java.sql.Timestamp
import java.util.UUID

/**
 * Stores DIDComm plaintext messages between DIDs.
 * This is a simplified DIDComm v2 envelope (no encryption) for demo purposes.
 * Production would use ECDH-ES + AES-256-GCM per the DIDComm Messaging spec.
 */
@Entity
@Table(
    name = "didcomm_message",
    indexes = [
        Index(name = "idx_didcomm_to_did", columnList = "to_did"),
        Index(name = "idx_didcomm_created_at", columnList = "created_at"),
    ]
)
class DIDCommMessageJpaEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    /** DIDComm message type (e.g. "https://trustid.io/didcomm/1.0/credential-offer") */
    @Column(name = "type", nullable = false, length = 255)
    var type: String,

    @Column(name = "from_did", nullable = false, length = 128)
    var fromDid: String,

    @Column(name = "to_did", nullable = false, length = 128)
    var toDid: String,

    /** JSON body of the message — arbitrary payload for the given type */
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    var body: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: Timestamp = Timestamp(System.currentTimeMillis()),

    /** Whether the recipient has fetched/read this message */
    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,
)
