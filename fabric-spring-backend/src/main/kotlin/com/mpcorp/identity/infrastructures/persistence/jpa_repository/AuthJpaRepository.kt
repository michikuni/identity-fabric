package com.mpcorp.identity.infrastructures.persistence.jpa_repository

import com.mpcorp.identity.common.enums.AccountStatus
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.AuthJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface AuthJpaRepository : JpaRepository<AuthJpaEntity, UUID> {
    @Query(value = "SELECT * FROM auth WHERE (phone = :username OR email = :username) LIMIT 1", nativeQuery = true)
    fun findUserByPhoneOrEmail(username: String): AuthJpaEntity?

    fun findByAccountStatus(accountStatus: AccountStatus): List<AuthJpaEntity>
}