package com.mpcorp.identity.application.usecase.auth

import com.mpcorp.identity.application.dto.auth.SignInCommand
import com.mpcorp.identity.common.exception.InvalidPasswordException
import com.mpcorp.identity.common.exception.UserNotFoundException
import com.mpcorp.identity.common.utils.JwtUtils
import com.mpcorp.identity.domain.repository.AuthRepository
import org.springframework.stereotype.Service

@Service
class SignInUseCase (
    private val authRepository: AuthRepository,
    private val jwtUtils: JwtUtils
){
    fun execute(signInCommand: SignInCommand) : String {
        val user = authRepository.findByUsername(signInCommand.username) ?: throw UserNotFoundException()
        if (user.password != signInCommand.password) {
            throw InvalidPasswordException()
        }
        val token = jwtUtils.generateToken(userId = user.id.toString(), role = user.role.name)
        return token
    }
}