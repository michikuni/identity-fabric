package com.mpcorp.identity.presentation.request.profile

import com.mpcorp.identity.presentation.model.ProfileRefPayload

data class UpdateProfileRequest(
    val profile: ProfileRefPayload,
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
