package com.mpcorp.identity.common.utils

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.function.Function
import javax.crypto.SecretKey

@Component
class JwtUtils(

    @Value("\${jwt.secret}")
    private val secret: String,

    @Value("\${jwt.expiration}")
    private val expirationMs: Long

) {

    private fun getSigningKey(): SecretKey {
        val keyBytes = Decoders.BASE64.decode(secret)
        return Keys.hmacShaKeyFor(keyBytes)
    }

    fun generateToken(
        userId: String,
        role: String,
        deviceId: String? = null,
    ): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationMs)

        val builder = Jwts.builder()
            .subject(userId)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiryDate)
        if (deviceId != null) builder.claim("deviceId", deviceId)
        return builder.signWith(getSigningKey()).compact()
    }

    fun extractDeviceId(token: String): String? = runCatching {
        extractClaim(token) { it["deviceId"] as? String }
    }.getOrNull()

    fun extractUserId(token: String): String {
        return extractClaim(token) { it.subject }
    }

    fun extractRole(token: String): String {
        return extractClaim(token) { it["role"] as String }
    }

    fun extractExpiration(token: String): Date {
        return extractClaim(token) { it.expiration }
    }

    fun <T> extractClaim(token: String, resolver: Function<Claims, T>): T {
        val claims = extractAllClaims(token)
        return resolver.apply(claims)
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun isTokenExpired(token: String): Boolean {
        return extractExpiration(token).before(Date())
    }

    fun validateToken(token: String, userId: String): Boolean {
        val extractedUserId = extractUserId(token)
        return extractedUserId == userId && !isTokenExpired(token)
    }
}