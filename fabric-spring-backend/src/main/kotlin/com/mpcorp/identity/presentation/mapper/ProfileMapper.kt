package com.mpcorp.identity.presentation.mapper

import com.mpcorp.identity.application.dto.profile.GetProfileResponseCommand
import com.mpcorp.identity.application.references.IdentifierModel
import com.mpcorp.identity.presentation.model.EmployeeRefPayload
import com.mpcorp.identity.presentation.model.IdentifierPayload
import com.mpcorp.identity.presentation.response.profile.ProfileResponseData

fun GetProfileResponseCommand.toDto(): ProfileResponseData = ProfileResponseData(
    id = IdentifierPayload(id.value),
    employee = EmployeeRefPayload(
        id = employee.id.toPayload(),
        authId = employee.authId.toPayload(),
    ),
    name = name,
    gender = gender,
    identityType = identityType,
    identityNumber = identityNumber,
    identityIssueDate = identityIssueDate,
    identityIssuePlace = identityIssuePlace,
    email = email,
    phone = phone,
    emergencyName = emergencyName,
    emergencyPhone = emergencyPhone,
    emergencyRelationship = emergencyRelationship,
    dateOfBirth = dateOfBirth,
    health = health,
    married = married,
    permanentResidence = permanentResidence,
    nowResidence = nowResidence,
    avatarUrl = avatarUrl,
    educationLevel = educationLevel,
    major = major,
    certificate = certificate,
    skillSet = skillSet,
    expYears = expYears,
)

private fun IdentifierModel?.toPayload() = this?.let { IdentifierPayload(value = it.value) }
