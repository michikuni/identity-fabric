package com.mpcorp.identity.application.dto.employee

import java.util.UUID

data class DeleteEmployeeCommand (
    val authId: UUID,
)