package com.mpcorp.identity.infrastructures.persistence.mapper

import com.mpcorp.identity.domain.entity.CompanyEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.CompanyJpaEntity

fun CompanyJpaEntity.toDomainEntity(): CompanyEntity = CompanyEntity(
    id = id,
    taxCode = taxCode,
    companyName = companyName,
    legalRepName = legalRepName,
    legalRepTitle = legalRepTitle,
    legalRepIdNumber = legalRepIdNumber,
    address = address,
    phone = phone,
    email = email,
    registeredAt = registeredAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    createdBy = createdBy,
)
