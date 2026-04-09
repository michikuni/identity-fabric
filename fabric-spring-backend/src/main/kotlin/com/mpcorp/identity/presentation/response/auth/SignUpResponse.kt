package com.mpcorp.identity.presentation.response.auth

data class SignUpResponse (
    val status: Int,
    val message: String,
    val token: String,
)