package com.mpcorp.identity.application.usecase.auth

import com.mpcorp.identity.application.dto.auth.SignUpCommand
import com.mpcorp.identity.common.enums.AccountStatus
import com.mpcorp.identity.common.enums.EmployeeRole
import com.mpcorp.identity.common.exception.UserAlreadyExistingException
import com.mpcorp.identity.common.utils.JwtUtils
import com.mpcorp.identity.domain.entity.AuthEntity
import com.mpcorp.identity.domain.repository.AuthRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class SignUpUseCase (
    private val authRepository: AuthRepository,
    private val jwtUtils: JwtUtils,
    private val passwordEncoder: BCryptPasswordEncoder,
){
    fun execute(signUpCommand: SignUpCommand) : String {
        if (authRepository.findByUsername(signUpCommand.phone) != null) {
            throw UserAlreadyExistingException("Số điện thoại đã được sử dụng")
        }
        if (authRepository.findByUsername(signUpCommand.email) != null) {
            throw UserAlreadyExistingException("Email đã được sử dụng")
        }
        val userCreated = authRepository.create(
            AuthEntity(
                phone = signUpCommand.phone,
                email = signUpCommand.email,
                password = passwordEncoder.encode(signUpCommand.password),
                role = EmployeeRole.EMPLOYEE,
                accountStatus = AccountStatus.PENDING,
            )
        )
        val token = jwtUtils.generateToken(userId = userCreated.id.toString(), role = userCreated.role.name)
        return token
    }
}