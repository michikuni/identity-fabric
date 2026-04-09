package com.mpcorp.identity.domain.entity

import java.sql.Timestamp

class EmployeeEntity(
    val id: Long? = null,
    val auth: AuthEntity,
    val department: String,
    val position: String,
    val status: String,
    val workingType: String,
    val isActive: Boolean,
    val manager: EmployeeEntity?,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
    val createdBy: String,
    val note: String?,
    val profile: ProfileEntity? = null,
    val contract: ContractEntity? = null,
    val payroll: PayrollEntity? = null
)