package com.mpcorp.identity.domain.entity

import java.sql.Timestamp

data class PayrollEntity(
    val id: Long? = null,
    val employee: EmployeeEntity,
    val salaryType: String,
    val baseSalary: Double,
    val bonusSalary: Double?,
    val overTimeRate: Double?,
    val totalIncome: Double,
    val currency: String,
    val payDay: Timestamp,
    val bankAccountNumber: String,
    val bankAccountName: String,
    val bankName: String,
    val bankBranch: String?
)