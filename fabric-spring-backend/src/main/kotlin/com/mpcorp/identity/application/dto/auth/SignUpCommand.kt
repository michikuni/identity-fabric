package com.mpcorp.identity.application.dto.auth

data class SignUpCommand (
    val email: String,
    val phone: String,
    val password: String
)