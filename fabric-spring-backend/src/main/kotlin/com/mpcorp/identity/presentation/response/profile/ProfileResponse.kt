package com.mpcorp.identity.presentation.response.profile

import com.mpcorp.identity.presentation.model.EmployeeRefPayload
import com.mpcorp.identity.presentation.model.IdentifierPayload

data class ProfileResponseData(
    val id: IdentifierPayload?,
    val employee: EmployeeRefPayload,
    val name: String,
    val gender: String,
    val identityType: String,
    val identityNumber: String,
    val identityIssueDate: Int,
    val identityIssuePlace: String,
    val email: String,
    val phone: String,
    val emergencyName: String,
    val emergencyPhone: String,
    val emergencyRelationship: String,
    val dateOfBirth: String,
    val health: String,
    val married: String,
    val permanentResidence: String,
    val nowResidence: String,
    val avatarUrl: String?,
    val educationLevel: String,
    val major: String,
    val certificate: List<String>?,
    val skillSet: List<String>,
    val expYears: Int,
)

data class ProfileResponse(
    val status: Int,
    val message: String,
    val data: ProfileResponseData?,
)
