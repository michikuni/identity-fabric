package com.mpcorp.identity.application.references

import com.mpcorp.identity.common.enums.EmployeeRole
import java.util.UUID

data class AuthRefModel (
    val id: UUID? = null,
    val email: String,
    val phone: String,
    val password: String?,
    val role: com.mpcorp.identity.common.enums.EmployeeRole,
)