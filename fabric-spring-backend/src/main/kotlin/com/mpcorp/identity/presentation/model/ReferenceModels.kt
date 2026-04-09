package com.mpcorp.identity.presentation.model

data class IdentifierPayload(
    val value: String,
)

data class EmployeeRefPayload(
    val id: IdentifierPayload? = null,
    val authId: IdentifierPayload? = null,
)

data class ProfileRefPayload(
    val id: IdentifierPayload,
)

data class ContractRefPayload(
    val id: IdentifierPayload,
)

data class PayrollRefPayload(
    val id: IdentifierPayload,
)
