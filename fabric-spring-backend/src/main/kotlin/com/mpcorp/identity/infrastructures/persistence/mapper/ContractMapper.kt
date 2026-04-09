package com.mpcorp.identity.infrastructures.persistence.mapper

import com.mpcorp.identity.domain.entity.ContractEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.ContractJpaEntity

fun ContractJpaEntity.toDomainEntity(): ContractEntity {
    return ContractEntity(
        id = id,
        contractExpire = contractExpire,
        employee = employee.toDomainEntity(),
        endDate = endDate,
        startDate = startDate,
        taxCode = taxCode,
        typeContract = typeContract,
        probationEndDate = probationEndDate,
        probationStartDate = probationStartDate,
        healthInsuranceNumber = healthInsuranceNumber,
        socialInsuranceNumber = socialInsuranceNumber,
    )
}

fun ContractEntity.toPersistentEntity(): ContractJpaEntity {
    return ContractJpaEntity(
        id = id,
        contractExpire = contractExpire,
        employee = employee.toPersistentEntity(),
        endDate = endDate,
        startDate = startDate,
        taxCode = taxCode,
        typeContract = typeContract,
        probationEndDate = probationEndDate,
        probationStartDate = probationStartDate,
        healthInsuranceNumber = healthInsuranceNumber,
        socialInsuranceNumber = socialInsuranceNumber,
    )
}
