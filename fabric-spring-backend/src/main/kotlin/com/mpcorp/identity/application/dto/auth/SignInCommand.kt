package com.mpcorp.identity.application.dto.auth

data class SignInCommand(
    val username: String,
    val password: String
)