package com.mpcorp.identity.infrastructures.security

import dev.samstevens.totp.code.*
import dev.samstevens.totp.exceptions.QrGenerationException
import dev.samstevens.totp.qr.QrData
import dev.samstevens.totp.qr.ZxingPngQrGenerator
import dev.samstevens.totp.secret.DefaultSecretGenerator
import dev.samstevens.totp.time.SystemTimeProvider
import dev.samstevens.totp.util.Utils.getDataUriForImage
import org.springframework.stereotype.Service

/**
 * MfaService — TOTP 2FA per RFC 6238 (Google Authenticator / Authy compatible).
 *
 * Flow:
 *  1. Admin/Chief calls setupMfa() → gets secret + QR data URI to scan in authenticator app
 *  2. User scans QR, then calls verifySetup() with a live code to confirm
 *  3. Secret stored (encrypted) in AuthJpaEntity.mfaSecret; mfaEnabled=true
 *  4. On every login after MFA enabled, verifyCode() must return true
 */
@Service
class MfaService {

    private val secretGenerator = DefaultSecretGenerator()
    private val timeProvider    = SystemTimeProvider()
    private val codeGenerator   = DefaultCodeGenerator()
    private val codeVerifier    = DefaultCodeVerifier(codeGenerator, timeProvider).apply {
        setTimePeriod(30)
        setAllowedTimePeriodDiscrepancy(1)
    }

    /** Generate a new TOTP secret (Base32, 20 bytes). */
    fun generateSecret(): String = secretGenerator.generate()

    /**
     * Build a data URI (image/png;base64) for a QR code the user scans in their authenticator app.
     * @param email   user identifier shown in the app
     * @param secret  the secret returned by generateSecret()
     */
    fun generateQrDataUri(email: String, secret: String): String {
        val qrData = QrData.Builder()
            .label(email)
            .secret(secret)
            .issuer("TrustID")
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build()
        val generator = ZxingPngQrGenerator()
        val imageData = try {
            generator.generate(qrData)
        } catch (e: QrGenerationException) {
            throw RuntimeException("QR generation failed: ${e.message}", e)
        }
        return getDataUriForImage(imageData, generator.imageMimeType)
    }

    /**
     * Verify a 6-digit TOTP code against the stored secret.
     * Allows ±1 period drift to handle clock skew.
     */
    fun verifyCode(secret: String, code: String): Boolean =
        codeVerifier.isValidCode(secret, code)

    /**
     * Generate a set of single-use backup codes (each 8 chars, alphanumeric).
     * Caller is responsible for storing BCrypt hashes of these.
     */
    fun generateBackupCodes(count: Int = 8): List<String> =
        (1..count).map {
            (1..8).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
        }
}
