package com.mpcorp.identity.application.dto.employee

import com.mpcorp.identity.application.references.EmployeeRefModel
import java.sql.Timestamp

data class UpdateEmployeeCommand(
    val department: String,
    val position: String,
    val status: String,
    val workingType: String,
    val isActive: Boolean,
    val manager: com.mpcorp.identity.application.references.EmployeeRefModel?,
    val updatedAt: Timestamp,
    val note: String?,
)
