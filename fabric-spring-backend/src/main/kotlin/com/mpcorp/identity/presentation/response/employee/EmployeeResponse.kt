package com.mpcorp.identity.presentation.response.employee

import com.mpcorp.identity.common.enums.EmployeeRole
import com.mpcorp.identity.presentation.model.EmployeeRefPayload
import com.mpcorp.identity.presentation.model.IdentifierPayload
import java.sql.Timestamp

data class EmployeeAuthResponse(
    val id: IdentifierPayload?,
    val email: String,
    val phone: String,
    val role: EmployeeRole,
)

data class EmployeeResponseData(
    val id: IdentifierPayload?,
    val auth: EmployeeAuthResponse,
    val department: String,
    val position: String,
    val status: String,
    val workingType: String,
    val isActive: Boolean,
    val manager: EmployeeRefPayload?,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
    val createdBy: String,
    val note: String?,
)

data class EmployeeResponse(
    val status: Int,
    val message: String,
    val data: EmployeeResponseData?,
)
