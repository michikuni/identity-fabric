package com.mpcorp.identity.presentation.mapper

import com.mpcorp.identity.application.dto.payroll.GetPayrollResponseCommand
import com.mpcorp.identity.application.references.IdentifierModel
import com.mpcorp.identity.presentation.model.EmployeeRefPayload
import com.mpcorp.identity.presentation.model.IdentifierPayload
import com.mpcorp.identity.presentation.response.payroll.PayrollResponseData

fun GetPayrollResponseCommand.toDto(): PayrollResponseData = PayrollResponseData(
    id = IdentifierPayload(id.value),
    employee = EmployeeRefPayload(
        id = employee.id.toPayload(),
        authId = employee.authId.toPayload(),
    ),
    salaryType = salaryType,
    baseSalary = baseSalary,
    bonusSalary = bonusSalary,
    overTimeRate = overTimeRate,
    totalIncome = totalIncome,
    currency = currency,
    payDay = payDay,
    bankAccountNumber = bankAccountNumber,
    bankAccountName = bankAccountName,
    bankName = bankName,
    bankBranch = bankBranch,
)

private fun IdentifierModel?.toPayload() = this?.let { IdentifierPayload(value = it.value) }
