package com.mpcorp.identity.domain.repository

import com.mpcorp.identity.common.enums.AccountStatus
import com.mpcorp.identity.domain.entity.AuthEntity
import java.util.UUID

interface AuthRepository {
    fun findByUsername(username: String): AuthEntity?
    fun findById(id: UUID): AuthEntity?
    fun findByStatus(status: AccountStatus): List<AuthEntity>
    fun create(authEntity: AuthEntity): AuthEntity
    fun updateStatus(id: UUID, status: AccountStatus): AuthEntity
}