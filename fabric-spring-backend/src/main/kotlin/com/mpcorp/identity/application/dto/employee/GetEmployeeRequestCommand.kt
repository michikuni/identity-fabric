package com.mpcorp.identity.application.dto.employee

import java.util.UUID

data class GetEmployeeRequestCommand(
    val authId: UUID,
)
