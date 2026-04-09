package com.mpcorp.identity.common.exception

data class ErrorResponse(
    val status: String,
    val code: Int,
    val message: String,
    val data: String
)