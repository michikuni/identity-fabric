package com.mpcorp.identity.infrastructures.persistence.jpa_repository

import com.mpcorp.identity.infrastructures.persistence.jpa_entity.DIDCommMessageJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface DIDCommMessageJpaRepository : JpaRepository<DIDCommMessageJpaEntity, UUID> {

    fun findByToDidOrderByCreatedAtDesc(toDid: String): List<DIDCommMessageJpaEntity>

    fun findByToDidAndIsReadFalseOrderByCreatedAtDesc(toDid: String): List<DIDCommMessageJpaEntity>

    @Modifying
    @Query("UPDATE DIDCommMessageJpaEntity m SET m.isRead = true WHERE m.toDid = :toDid AND m.isRead = false")
    fun markAllReadForDid(toDid: String)
}
