package com.mpcorp.identity.infrastructures.security.user_details

import com.mpcorp.identity.domain.entity.AuthEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.AuthJpaRepository
import com.mpcorp.identity.infrastructures.persistence.mapper.toDomainEntity
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CustomUserDetailsService(
    private val authJpaRepository: AuthJpaRepository
) : UserDetailsService {
    fun loadUserByUserId(userId: UUID): CustomUserDetails {
        val user = authJpaRepository.findById(userId).orElseThrow { Exception("User with id $userId not found") }
        return CustomUserDetails(user.toDomainEntity())
    }

    override fun loadUserByUsername(username: String): UserDetails {
        val user =
            authJpaRepository.findUserByPhoneOrEmail(username) ?: throw Exception("User with id $username not found")
        return CustomUserDetails(
            AuthEntity(
                id = user.id,
                phone = user.phone,
                password = user.password,
                email = user.email,
                role = user.role,
            )
        )
    }
}