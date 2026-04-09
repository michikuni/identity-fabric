package com.mpcorp.identity.presentation.api

import com.mpcorp.identity.presentation.request.auth.SignInRequest
import com.mpcorp.identity.presentation.request.auth.SignUpRequest
import com.mpcorp.identity.presentation.response.auth.SignInResponse
import com.mpcorp.identity.presentation.response.auth.SignUpResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/api/v1/auth")
interface AuthApi {
    @PostMapping("/sign-in")
    fun signIn(@RequestBody signInRequest: SignInRequest): SignInResponse

    @PostMapping("/sign-up")
    fun signUp(@RequestBody signUpRequest: SignUpRequest): SignUpResponse
}