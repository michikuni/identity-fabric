package com.mpcorp.identity.infrastructures.persistence.repository

import com.mpcorp.identity.domain.entity.AuthEntity
import com.mpcorp.identity.domain.repository.AuthRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.AuthJpaRepository
import com.mpcorp.identity.infrastructures.persistence.mapper.toDomainEntity
import com.mpcorp.identity.infrastructures.persistence.mapper.toPersistentEntity
import org.springframework.stereotype.Service

@Service
class AuthRepositoryImpl(
    private val authJpaRepository: AuthJpaRepository,
) : AuthRepository {
    override fun findByUsername(username: String): AuthEntity? {
        val user = authJpaRepository.findUserByPhoneOrEmail(username) ?: return null
        return user.toDomainEntity()
    }

    override fun create(authEntity: AuthEntity): AuthEntity {
        val save = authJpaRepository.save(authEntity.toPersistentEntity())
        return save.toDomainEntity()
    }
}