package com.mpcorp.identity.application.dto.payroll

import com.mpcorp.identity.application.references.EmployeeRefModel

data class GetPayrollRequestCommand(
    val employee: com.mpcorp.identity.application.references.EmployeeRefModel,
)
