package com.mpcorp.identity.application.usecase.auth

import com.mpcorp.identity.application.dto.auth.SignInCommand
import com.mpcorp.identity.common.enums.AccountStatus
import com.mpcorp.identity.common.exception.AccountPendingException
import com.mpcorp.identity.common.exception.AccountRejectedException
import com.mpcorp.identity.common.exception.InvalidPasswordException
import com.mpcorp.identity.common.exception.UserNotFoundException
import com.mpcorp.identity.common.utils.JwtUtils
import com.mpcorp.identity.domain.repository.AuthRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.UserSessionJpaEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.AuthJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.UserSessionJpaRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp
import java.time.Instant

@Service
class SignInUseCase(
    private val authRepository: AuthRepository,
    private val authJpaRepository: AuthJpaRepository,
    private val sessionRepository: UserSessionJpaRepository,
    private val jwtUtils: JwtUtils,
    private val passwordEncoder: BCryptPasswordEncoder,
) {
    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val LOCK_MINUTES = 15L
    }

    data class Result(val token: String, val id: String, val role: String, val email: String)

    fun execute(signInCommand: SignInCommand): Result {
        val user = authRepository.findByUsername(signInCommand.username) ?: throw UserNotFoundException()

        // Account lockout check
        val jpaUser = authJpaRepository.findUserByPhoneOrEmail(signInCommand.username)
            ?: throw UserNotFoundException()

        val lockedUntil = jpaUser.lockedUntil
        if (lockedUntil != null && Instant.now().isBefore(lockedUntil.toInstant())) {
            val remaining = (lockedUntil.toInstant().epochSecond - Instant.now().epochSecond) / 60 + 1
            throw ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Account locked due to too many failed attempts. Try again in ~$remaining minutes."
            )
        }

        if (!passwordEncoder.matches(signInCommand.password, user.password)) {
            // Increment failed attempts
            jpaUser.failedLoginAttempts = (jpaUser.failedLoginAttempts ?: 0) + 1
            if ((jpaUser.failedLoginAttempts ?: 0) >= MAX_ATTEMPTS) {
                jpaUser.lockedUntil = Timestamp.from(Instant.now().plusSeconds(LOCK_MINUTES * 60))
                jpaUser.failedLoginAttempts = 0
            }
            authJpaRepository.save(jpaUser)
            throw InvalidPasswordException()
        }

        // Successful login — reset lockout counters
        if ((jpaUser.failedLoginAttempts ?: 0) > 0 || jpaUser.lockedUntil != null) {
            jpaUser.failedLoginAttempts = 0
            jpaUser.lockedUntil = null
            authJpaRepository.save(jpaUser)
        }

        when (user.accountStatus) {
            AccountStatus.PENDING  -> throw AccountPendingException()
            AccountStatus.REJECTED -> throw AccountRejectedException()
            AccountStatus.ACTIVE   -> Unit
        }

        val token = jwtUtils.generateToken(
            userId   = user.id.toString(),
            role     = user.role.name,
            deviceId = signInCommand.deviceId,
        )

        // Register/update device session if deviceId provided
        if (signInCommand.deviceId != null && user.id != null) {
            val now = Timestamp.from(Instant.now())
            val existing = sessionRepository.findByUserIdAndDeviceId(user.id!!, signInCommand.deviceId)
            if (existing != null) {
                existing.lastSeen       = now
                existing.deviceName     = signInCommand.deviceName ?: existing.deviceName
                existing.devicePlatform = signInCommand.devicePlatform ?: existing.devicePlatform
                existing.isActive       = true
                sessionRepository.save(existing)
            } else {
                sessionRepository.save(
                    UserSessionJpaEntity(
                        userId         = user.id!!,
                        deviceId       = signInCommand.deviceId,
                        deviceName     = signInCommand.deviceName,
                        devicePlatform = signInCommand.devicePlatform,
                        lastSeen       = now,
                        createdAt      = now,
                        isActive       = true,
                    )
                )
            }
        }

        return Result(token = token, id = user.id.toString(), role = user.role.name, email = user.email)
    }
}