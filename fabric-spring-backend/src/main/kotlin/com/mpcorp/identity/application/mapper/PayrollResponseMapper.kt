package com.mpcorp.identity.application.mapper

import com.mpcorp.identity.application.dto.payroll.GetPayrollResponseCommand
import com.mpcorp.identity.application.mapper.toRefModel
import com.mpcorp.identity.application.references.IdentifierModel
import com.mpcorp.identity.domain.entity.PayrollEntity

fun com.mpcorp.identity.domain.entity.PayrollEntity.toGetPayrollResponseCommand(): com.mpcorp.identity.application.dto.payroll.GetPayrollResponseCommand =
    _root_ide_package_.com.mpcorp.identity.application.dto.payroll.GetPayrollResponseCommand(
        id = _root_ide_package_.com.mpcorp.identity.application.references.IdentifierModel(requireNotNull(id) { "payroll id missing" }.toString()),
        employee = employee.toRefModel(),
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
