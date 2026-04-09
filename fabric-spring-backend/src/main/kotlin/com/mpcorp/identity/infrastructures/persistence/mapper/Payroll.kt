package com.mpcorp.identity.infrastructures.persistence.mapper

import com.mpcorp.identity.domain.entity.PayrollEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.PayrollJpaEntity

fun PayrollJpaEntity.toDomainEntity(): PayrollEntity {
    return PayrollEntity(
        id = id,
        employee = employee.toDomainEntity(),
        bankAccountName = bankAccountName,
        bankAccountNumber = bankAccountNumber,
        bankName = bankName,
        bankBranch = bankBranch,
        payDay = payDay,
        currency = currency,
        baseSalary = baseSalary,
        salaryType = salaryType,
        bonusSalary = bonusSalary,
        totalIncome = totalIncome,
        overTimeRate = overTimeRate,
    )
}

fun PayrollEntity.toPersistentEntity(): PayrollJpaEntity {
    return PayrollJpaEntity(
        id = id,
        employee = employee.toPersistentEntity(),
        bankAccountName = bankAccountName,
        bankAccountNumber = bankAccountNumber,
        bankName = bankName,
        bankBranch = bankBranch,
        payDay = payDay,
        currency = currency,
        baseSalary = baseSalary,
        salaryType = salaryType,
        bonusSalary = bonusSalary,
        totalIncome = totalIncome,
        overTimeRate = overTimeRate,
    )
}
