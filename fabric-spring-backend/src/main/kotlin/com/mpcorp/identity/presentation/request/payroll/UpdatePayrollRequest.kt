package com.mpcorp.identity.presentation.request.payroll

import com.mpcorp.identity.presentation.model.PayrollRefPayload
import java.sql.Timestamp

data class UpdatePayrollRequest(
    val payroll: PayrollRefPayload,
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
