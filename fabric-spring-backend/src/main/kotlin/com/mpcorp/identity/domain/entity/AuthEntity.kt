package com.mpcorp.identity.domain.entity

import com.mpcorp.identity.common.enums.AccountStatus
import com.mpcorp.identity.common.enums.EmployeeRole
import java.util.UUID

data class AuthEntity (
    val id: UUID? = null,
    val email: String,
    val phone: String,
    val password: String?,
    val role: EmployeeRole,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
)