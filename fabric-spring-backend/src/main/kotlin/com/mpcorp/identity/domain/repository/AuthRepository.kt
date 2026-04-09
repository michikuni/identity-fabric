package com.mpcorp.identity.domain.repository

import com.mpcorp.identity.domain.entity.AuthEntity

interface AuthRepository {
    fun findByUsername(username: String): AuthEntity?
    fun create(authEntity: AuthEntity): AuthEntity
}