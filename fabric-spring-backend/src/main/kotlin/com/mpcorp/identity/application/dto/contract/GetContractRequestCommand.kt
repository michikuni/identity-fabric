package com.mpcorp.identity.application.dto.contract

import com.mpcorp.identity.application.references.EmployeeRefModel

data class GetContractRequestCommand(
    val employee: com.mpcorp.identity.application.references.EmployeeRefModel,
)
