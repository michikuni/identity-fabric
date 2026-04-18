package com.mpcorp.identity.application.usecase.auth

import com.mpcorp.identity.application.dto.auth.SignInCommand
import com.mpcorp.identity.common.enums.AccountStatus
import com.mpcorp.identity.common.exception.AccountPendingException
import com.mpcorp.identity.common.exception.AccountRejectedException
import com.mpcorp.identity.common.exception.InvalidPasswordException
import com.mpcorp.identity.common.exception.UserNotFoundException
import com.mpcorp.identity.common.utils.JwtUtils
import com.mpcorp.identity.domain.repository.AuthRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class SignInUseCase (
    private val authRepository: AuthRepository,
    private val jwtUtils: JwtUtils,
    private val passwordEncoder: BCryptPasswordEncoder,
){
    data class Result(val token: String, val id: String, val role: String, val email: String)

    fun execute(signInCommand: SignInCommand) : Result {
        val user = authRepository.findByUsername(signInCommand.username) ?: throw UserNotFoundException()
        if (!passwordEncoder.matches(signInCommand.password, user.password)) {
            throw InvalidPasswordException()
        }
        when (user.accountStatus) {
            AccountStatus.PENDING  -> throw AccountPendingException()
            AccountStatus.REJECTED -> throw AccountRejectedException()
            AccountStatus.ACTIVE   -> Unit
        }
        val token = jwtUtils.generateToken(userId = user.id.toString(), role = user.role.name)
        return Result(token = token, id = user.id.toString(), role = user.role.name, email = user.email)
    }
}