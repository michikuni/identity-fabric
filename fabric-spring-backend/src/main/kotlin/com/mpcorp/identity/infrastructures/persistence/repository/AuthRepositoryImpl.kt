package com.mpcorp.identity.infrastructures.persistence.repository

import com.mpcorp.identity.common.enums.AccountStatus
import com.mpcorp.identity.common.exception.UserNotFoundException
import com.mpcorp.identity.domain.entity.AuthEntity
import com.mpcorp.identity.domain.repository.AuthRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.AuthJpaRepository
import com.mpcorp.identity.infrastructures.persistence.mapper.toDomainEntity
import com.mpcorp.identity.infrastructures.persistence.mapper.toPersistentEntity
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuthRepositoryImpl(
    private val authJpaRepository: AuthJpaRepository,
) : AuthRepository {
    override fun findByUsername(username: String): AuthEntity? {
        val user = authJpaRepository.findUserByPhoneOrEmail(username) ?: return null
        return user.toDomainEntity()
    }

    override fun findById(id: UUID): AuthEntity? {
        return authJpaRepository.findById(id).orElse(null)?.toDomainEntity()
    }

    override fun findByStatus(status: AccountStatus): List<AuthEntity> {
        return authJpaRepository.findByAccountStatus(status).map { it.toDomainEntity() }
    }

    override fun create(authEntity: AuthEntity): AuthEntity {
        val save = authJpaRepository.save(authEntity.toPersistentEntity())
        return save.toDomainEntity()
    }

    override fun updateStatus(id: UUID, status: AccountStatus): AuthEntity {
        val entity = authJpaRepository.findById(id).orElseThrow { UserNotFoundException() }
        entity.accountStatus = status
        return authJpaRepository.save(entity).toDomainEntity()
    }
}