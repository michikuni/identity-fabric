package com.mpcorp.identity.presentation.request.auth

data class SignInRequest(
    val username: String,
    val password: String,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val devicePlatform: String? = null,
)