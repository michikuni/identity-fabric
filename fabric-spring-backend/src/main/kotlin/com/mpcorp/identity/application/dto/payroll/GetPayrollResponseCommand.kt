package com.mpcorp.identity.application.dto.payroll

import com.mpcorp.identity.application.references.EmployeeRefModel
import com.mpcorp.identity.application.references.IdentifierModel
import java.sql.Timestamp

data class GetPayrollResponseCommand(
    val id: com.mpcorp.identity.application.references.IdentifierModel,
    val employee: com.mpcorp.identity.application.references.EmployeeRefModel,
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
