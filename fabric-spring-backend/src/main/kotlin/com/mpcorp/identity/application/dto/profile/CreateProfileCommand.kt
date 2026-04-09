package com.mpcorp.identity.application.dto.profile

import com.mpcorp.identity.application.references.EmployeeRefModel

data class CreateProfileCommand(
    val employee: com.mpcorp.identity.application.references.EmployeeRefModel,
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
    val certificate: List<String>? = null,
    val skillSet: List<String> = listOf(),
    val expYears: Int,
)
