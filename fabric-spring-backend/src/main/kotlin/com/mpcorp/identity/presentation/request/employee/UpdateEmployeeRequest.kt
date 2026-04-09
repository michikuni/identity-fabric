package com.mpcorp.identity.presentation.request.employee

import com.mpcorp.identity.presentation.model.EmployeeRefPayload
import java.sql.Timestamp

data class UpdateEmployeeRequest(
    val department: String,
    val position: String,
    val status: String,
    val workingType: String,
    val isActive: Boolean,
    val manager: EmployeeRefPayload? = null,
    val updatedAt: Timestamp,
    val note: String?,
)
