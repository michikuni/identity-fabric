package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.application.dto.auth.SignInCommand
import com.mpcorp.identity.application.dto.auth.SignUpCommand
import com.mpcorp.identity.application.usecase.auth.SignInUseCase
import com.mpcorp.identity.application.usecase.auth.SignUpUseCase
import com.mpcorp.identity.common.constant.ErrorCodes
import com.mpcorp.identity.common.constant.StatusMessage
import com.mpcorp.identity.presentation.api.AuthApi
import com.mpcorp.identity.presentation.request.auth.SignInRequest
import com.mpcorp.identity.presentation.request.auth.SignUpRequest
import com.mpcorp.identity.presentation.response.auth.SignInResponse
import com.mpcorp.identity.presentation.response.auth.SignUpResponse
import org.springframework.web.bind.annotation.*

@RestController
class AuthController(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
) : AuthApi {
    override fun signIn(@RequestBody signInRequest: SignInRequest): SignInResponse {
        val signInSuccess = signInUseCase.execute(
            SignInCommand(
                username = signInRequest.username,
                password = signInRequest.password
            )
        )
        return SignInResponse(
                status = ErrorCodes.SUCCESS,
                message = StatusMessage.SUCCESS,
                token = signInSuccess
        )
    }

    override fun signUp(@RequestBody signUpRequest: SignUpRequest): SignUpResponse {
        val signUpSuccess = signUpUseCase.execute(SignUpCommand(phone = signUpRequest.phone, password = signUpRequest.password, email = signUpRequest.email))
        return SignUpResponse(
            status = ErrorCodes.CREATE_SUCCESS,
            message = StatusMessage.SUCCESS,
            token = signUpSuccess
        )
    }
}