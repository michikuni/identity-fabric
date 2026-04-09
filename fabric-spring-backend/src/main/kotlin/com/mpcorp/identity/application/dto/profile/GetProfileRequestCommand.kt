package com.mpcorp.identity.application.dto.profile

import com.mpcorp.identity.application.references.EmployeeRefModel

data class GetProfileRequestCommand(
    val employee: com.mpcorp.identity.application.references.EmployeeRefModel,
)
