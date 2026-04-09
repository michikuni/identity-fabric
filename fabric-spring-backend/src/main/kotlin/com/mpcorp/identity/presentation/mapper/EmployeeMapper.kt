package com.mpcorp.identity.presentation.mapper

import com.mpcorp.identity.application.dto.employee.GetEmployeeResponseCommand
import com.mpcorp.identity.application.references.IdentifierModel
import com.mpcorp.identity.presentation.model.EmployeeRefPayload
import com.mpcorp.identity.presentation.model.IdentifierPayload
import com.mpcorp.identity.presentation.response.employee.EmployeeAuthResponse
import com.mpcorp.identity.presentation.response.employee.EmployeeResponseData

fun GetEmployeeResponseCommand.toDto(): EmployeeResponseData = EmployeeResponseData(
    id = id.toString().toIdentifierPayload(),
    auth = EmployeeAuthResponse(
        id = auth.id?.toString()?.toIdentifierPayload(),
        email = auth.email,
        phone = auth.phone,
        role = auth.role,
    ),
    department = department,
    position = position,
    status = status,
    workingType = workingType,
    isActive = isActive,
    manager = manager?.let {
        EmployeeRefPayload(
            id = it.id.toPayload(),
            authId = it.authId.toPayload(),
        )
    },
    createdAt = createdAt,
    updatedAt = updatedAt,
    createdBy = createdBy,
    note = note,
)

private fun IdentifierModel?.toPayload() = this?.let { IdentifierPayload(value = it.value) }
