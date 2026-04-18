package com.mpcorp.identity.infrastructures.persistence.mapper

import com.mpcorp.identity.domain.entity.AuthEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.AuthJpaEntity

fun AuthJpaEntity.toDomainEntity() : AuthEntity {
    return AuthEntity(
        id = id,
        email = email,
        password = password,
        phone = phone,
        role = role,
        accountStatus = accountStatus,
    )
}

fun AuthEntity.toPersistentEntity() : AuthJpaEntity {
    return AuthJpaEntity(
        id = id,
        email = email,
        password = password,
        phone = phone,
        role = role,
        accountStatus = accountStatus,
    )
}