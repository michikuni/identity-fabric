package com.mpcorp.identity.infrastructures.persistence.mapper

import com.mpcorp.identity.domain.entity.EmployeeEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.EmployeeJpaEntity

fun EmployeeJpaEntity.toDomainEntity(): EmployeeEntity {
    return EmployeeEntity(
        id = id,
        auth = auth.toDomainEntity(),
        department = department,
        position = position,
        status = status,
        workingType = workingType,
        isActive = isActive,
        manager = manager?.toDomainReference(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        note = note,
    )
}

fun EmployeeEntity.toPersistentEntity(): EmployeeJpaEntity {
    return EmployeeJpaEntity(
        id = id,
        auth = auth.toPersistentEntity(),
        department = department,
        position = position,
        status = status,
        workingType = workingType,
        isActive = isActive,
        manager = manager?.toPersistentReference(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        note = note,
    )
}

private fun EmployeeJpaEntity.toDomainReference(): EmployeeEntity {
    return EmployeeEntity(
        id = id,
        auth = auth.toDomainEntity(),
        department = department,
        position = position,
        status = status,
        workingType = workingType,
        isActive = isActive,
        manager = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        note = note,
    )
}

private fun EmployeeEntity.toPersistentReference(): EmployeeJpaEntity {
    return EmployeeJpaEntity(
        id = id,
        auth = auth.toPersistentEntity(),
        department = department,
        position = position,
        status = status,
        workingType = workingType,
        isActive = isActive,
        manager = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        note = note,
    )
}
