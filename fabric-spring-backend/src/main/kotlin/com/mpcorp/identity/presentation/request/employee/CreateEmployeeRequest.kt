package com.mpcorp.identity.presentation.request.employee

import com.mpcorp.identity.presentation.model.EmployeeRefPayload
import java.sql.Timestamp

data class CreateEmployeeRequest(
    val department: String,
    val position: String,
    val status: String,
    val workingType: String,
    val isActive: Boolean,
    val manager: EmployeeRefPayload? = null,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
    val createdBy: String,
    val note: String?,
)
