package com.mpcorp.identity.infrastructures.security.user_details

import com.mpcorp.identity.domain.entity.AuthEntity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class CustomUserDetails(
    private val auth: AuthEntity
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_${auth.role.name}"))
    }
    fun getId() = auth.id
    override fun getPassword() = auth.password
    override fun getUsername() = auth.phone
    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = true
}