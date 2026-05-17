package com.mpcorp.identity.infrastructures.persistence.jpa_repository

import com.mpcorp.identity.infrastructures.persistence.jpa_entity.UserSessionJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface UserSessionJpaRepository : JpaRepository<UserSessionJpaEntity, UUID> {

    fun findByUserIdAndIsActiveTrue(userId: UUID): List<UserSessionJpaEntity>

    fun findByUserIdAndDeviceId(userId: UUID, deviceId: String): UserSessionJpaEntity?

    @Modifying
    @Query("UPDATE UserSessionJpaEntity s SET s.isActive = false WHERE s.userId = :userId")
    fun deactivateAllByUserId(userId: UUID)

    @Modifying
    @Query("UPDATE UserSessionJpaEntity s SET s.isActive = false WHERE s.userId = :userId AND s.deviceId = :deviceId")
    fun deactivateByUserIdAndDeviceId(userId: UUID, deviceId: String)
}
