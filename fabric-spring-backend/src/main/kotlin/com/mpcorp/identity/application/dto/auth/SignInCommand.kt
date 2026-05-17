package com.mpcorp.identity.application.dto.auth

data class SignInCommand(
    val username: String,
    val password: String,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val devicePlatform: String? = null,
)