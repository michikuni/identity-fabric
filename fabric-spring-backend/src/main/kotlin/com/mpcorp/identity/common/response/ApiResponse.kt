package com.mpcorp.identity.common.response

data class ApiResponse<T>(
    val status: String,
    val message: String,
    val data: T?,
    val code: Int = 0,
)