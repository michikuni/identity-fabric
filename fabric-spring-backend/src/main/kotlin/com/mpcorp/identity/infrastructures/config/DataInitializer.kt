package com.mpcorp.identity.infrastructures.config

import com.mpcorp.identity.common.enums.AccountStatus
import com.mpcorp.identity.common.enums.EmployeeRole
import com.mpcorp.identity.domain.entity.AuthEntity
import com.mpcorp.identity.domain.repository.AuthRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val authRepository: AuthRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
    @Value("\${admin.email}") private val adminEmail: String,
    @Value("\${admin.phone}") private val adminPhone: String,
    @Value("\${admin.password}") private val adminPassword: String,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(DataInitializer::class.java)

    override fun run(args: ApplicationArguments) {
        val existing = authRepository.findByUsername(adminEmail)
        if (existing != null) {
            log.info("Admin account already exists, skipping seed.")
            return
        }

        authRepository.create(
            AuthEntity(
                email = adminEmail,
                phone = adminPhone,
                password = passwordEncoder.encode(adminPassword),
                role = EmployeeRole.ADMIN,
                accountStatus = AccountStatus.ACTIVE,
            )
        )
        log.info("Admin account created: $adminEmail")
    }
}
