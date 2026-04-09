package com.mpcorp.identity.presentation.response.payroll

import com.mpcorp.identity.presentation.model.EmployeeRefPayload
import com.mpcorp.identity.presentation.model.IdentifierPayload
import java.sql.Timestamp

data class PayrollResponseData(
    val id: IdentifierPayload?,
    val employee: EmployeeRefPayload,
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
    val bankBranch: String?,
)

data class PayrollResponse(
    val status: Int,
    val message: String,
    val data: PayrollResponseData?,
)
