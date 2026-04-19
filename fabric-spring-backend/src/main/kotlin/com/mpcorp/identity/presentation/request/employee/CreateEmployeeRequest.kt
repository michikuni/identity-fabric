package com.mpcorp.identity.presentation.request.employee

import com.mpcorp.identity.presentation.model.EmployeeRefPayload

data class CreateEmployeeRequest(
    val department: String,
    val position: String,
    val status: String,
    val workingType: String,
    val isActive: Boolean,
    val manager: EmployeeRefPayload? = null,
    val createdBy: String,
    val note: String?,
    val publicKeyJwk: String? = null,
)
