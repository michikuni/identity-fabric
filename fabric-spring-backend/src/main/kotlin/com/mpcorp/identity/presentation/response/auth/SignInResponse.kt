package com.mpcorp.identity.presentation.response.auth

data class SignInResponse (
    val status: Int,
    val message: String,
    val token: String,
    val id: String,
    val role: String,
    val email: String,
)