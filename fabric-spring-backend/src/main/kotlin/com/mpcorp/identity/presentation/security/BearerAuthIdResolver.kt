package com.mpcorp.identity.presentation.security

import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.common.utils.JwtUtils
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BearerAuthIdResolver(
    private val jwtUtils: JwtUtils,
) {
    fun resolveAuthId(request: HttpServletRequest): UUID {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION) ?: throw EmployeeNotFoundException()
        if (!header.startsWith("Bearer ", ignoreCase = true)) throw EmployeeNotFoundException()
        val raw = header.substring(7).trim()
        return try {
            UUID.fromString(jwtUtils.extractUserId(raw))
        } catch (_: Exception) {
            throw EmployeeNotFoundException()
        }
    }
}
