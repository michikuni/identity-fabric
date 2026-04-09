package com.mpcorp.identity.application.dto.employee

import com.mpcorp.identity.application.references.EmployeeRefModel
import java.sql.Timestamp

data class CreateEmployeeCommand(
    val department: String,
    val position: String,
    val status: String,
    val workingType: String,
    val isActive: Boolean,
    val manager: com.mpcorp.identity.application.references.EmployeeRefModel?,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
    val createdBy: String,
    val note: String?,
)
