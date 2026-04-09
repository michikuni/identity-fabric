package com.mpcorp.identity.presentation.request.auth

data class SignInRequest (
    val username: String,
    val password: String
)