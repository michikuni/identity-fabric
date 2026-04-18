package com.mpcorp.identity.application.dto.employee

import com.mpcorp.identity.application.references.EmployeeRefModel

data class CreateEmployeeCommand(
    val department: String,
    val position: String,
    val status: String,
    val workingType: String,
    val isActive: Boolean,
    val manager: com.mpcorp.identity.application.references.EmployeeRefModel?,
    val createdBy: String,
    val note: String?,
)
