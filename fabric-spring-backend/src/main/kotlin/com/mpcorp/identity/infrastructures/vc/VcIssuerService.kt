package com.mpcorp.identity.infrastructures.vc

import com.fasterxml.jackson.databind.ObjectMapper
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.EmployeeJpaEntity
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * VcIssuerService — phát hành Verifiable Credential cho nhân viên.
 *
 * Dùng HMAC-SHA256 với server secret để ký VC (phù hợp PoC/báo cáo).
 * Production nên dùng Ed25519 hoặc ECDSA P-256 với server keypair.
 *
 * VC format: W3C Verifiable Credentials Data Model v1.1 (simplified)
 */
@Service
class VcIssuerService(
    private val objectMapper: ObjectMapper,
    @Value("\${vc.secret}") private val vcSecret: String,
) {

    /**
     * Phát hành EmploymentVC cho nhân viên vừa được Admin approve.
     *
     * @param employee  JPA entity của nhân viên (sau khi approved)
     * @return VC JSON string đã được ký
     */
    fun issueEmploymentVC(employee: EmployeeJpaEntity): String {
        val now = Instant.now()
        val expiry = now.plus(365, ChronoUnit.DAYS)
        val employeeId = employee.id.toString()
        val did = "did:fabric:trustid:$employeeId"

        val credentialSubject = mapOf(
            "id"               to did,
            "department"       to employee.department,
            "position"         to employee.position,
            "employmentStatus" to "ACTIVE",
            "startDate"        to employee.createdAt.toInstant().toString(),
        )

        val vcBody = mapOf(
            "@context"          to listOf("https://www.w3.org/2018/credentials/v1"),
            "type"              to listOf("VerifiableCredential", "EmploymentCredential"),
            "id"                to "vc:trustid:employment:$employeeId:${now.epochSecond}",
            "issuer"            to "did:fabric:trustid:org1",
            "issuanceDate"      to now.toString(),
            "expirationDate"    to expiry.toString(),
            "credentialSubject" to credentialSubject,
        )

        val vcBodyJson = objectMapper.writeValueAsString(vcBody)
        val proofValue  = hmacSha256(vcBodyJson)

        val vcWithProof = vcBody + mapOf(
            "proof" to mapOf(
                "type"       to "HMAC-SHA256",
                "created"    to now.toString(),
                "verificationMethod" to "did:fabric:trustid:org1#key-1",
                "proofValue" to proofValue,
            )
        )

        return objectMapper.writeValueAsString(vcWithProof)
    }

    /**
     * Verify một VC string — tách proof ra, recompute HMAC trên phần body, so sánh.
     *
     * @return true nếu chữ ký hợp lệ và VC chưa hết hạn
     */
    fun verifyVC(vcJson: String): VcVerifyResult {
        return try {
            @Suppress("UNCHECKED_CAST")
            val vc = objectMapper.readValue(vcJson, Map::class.java) as Map<String, Any>
            val proof = vc["proof"] as? Map<*, *>
                ?: return VcVerifyResult(false, "Missing proof")
            val storedProof = proof["proofValue"] as? String
                ?: return VcVerifyResult(false, "Missing proofValue")

            // Recompute on body without proof
            val bodyOnly = vc - "proof"
            val bodyJson = objectMapper.writeValueAsString(bodyOnly)
            val expected = hmacSha256(bodyJson)

            if (!storedProof.equals(expected, ignoreCase = true)) {
                return VcVerifyResult(false, "Proof mismatch — VC may be tampered")
            }

            // Check expiry
            val expiry = vc["expirationDate"] as? String
            if (expiry != null && Instant.parse(expiry).isBefore(Instant.now())) {
                return VcVerifyResult(false, "VC expired at $expiry")
            }

            VcVerifyResult(true, "Valid")
        } catch (e: Exception) {
            VcVerifyResult(false, "Parse error: ${e.message}")
        }
    }

    /**
     * Phát hành SalaryRangeVC khi payroll được gán cho nhân viên.
     * Chỉ expose salary band (LOW/MID/HIGH), không tiết lộ con số cụ thể.
     */
    fun issueSalaryRangeVC(employee: EmployeeJpaEntity, baseSalary: Double, currency: String): String {
        val now = Instant.now()
        val expiry = now.plus(365, ChronoUnit.DAYS)
        val employeeId = employee.id.toString()
        val did = "did:fabric:trustid:$employeeId"

        val salaryBand = when {
            baseSalary < 10_000_000  -> "ENTRY"
            baseSalary < 20_000_000  -> "MID"
            baseSalary < 40_000_000  -> "SENIOR"
            else                     -> "EXECUTIVE"
        }

        val credentialSubject = mapOf(
            "id"         to did,
            "salaryBand" to salaryBand,
            "currency"   to currency,
            "position"   to employee.position,
            "department" to employee.department,
            "issuedAt"   to now.toString(),
        )

        val vcBody = mapOf(
            "@context"          to listOf("https://www.w3.org/2018/credentials/v1"),
            "type"              to listOf("VerifiableCredential", "SalaryRangeCredential"),
            "id"                to "vc:trustid:salary:$employeeId:${now.epochSecond}",
            "issuer"            to "did:fabric:trustid:org1",
            "issuanceDate"      to now.toString(),
            "expirationDate"    to expiry.toString(),
            "credentialSubject" to credentialSubject,
        )

        val vcBodyJson = objectMapper.writeValueAsString(vcBody)
        val proofValue = hmacSha256(vcBodyJson)

        val vcWithProof = vcBody + mapOf(
            "proof" to mapOf(
                "type"               to "HMAC-SHA256",
                "created"            to now.toString(),
                "verificationMethod" to "did:fabric:trustid:org1#key-1",
                "proofValue"         to proofValue,
            )
        )
        return objectMapper.writeValueAsString(vcWithProof)
    }

    /**
     * Phát hành PromotionVC khi nhân viên được thăng chức / đổi vị trí.
     */
    fun issuePromotionVC(
        employee: EmployeeJpaEntity,
        oldPosition: String,
        newPosition: String,
        promotedBy: String,
    ): String {
        val now = Instant.now()
        val employeeId = employee.id.toString()
        val did = "did:fabric:trustid:$employeeId"

        val credentialSubject = mapOf(
            "id"          to did,
            "department"  to employee.department,
            "oldPosition" to oldPosition,
            "newPosition" to newPosition,
            "promotionDate" to now.toString(),
            "promotedBy"  to promotedBy,
        )

        val vcBody = mapOf(
            "@context"          to listOf("https://www.w3.org/2018/credentials/v1"),
            "type"              to listOf("VerifiableCredential", "PromotionCredential"),
            "id"                to "vc:trustid:promotion:$employeeId:${now.epochSecond}",
            "issuer"            to "did:fabric:trustid:org1",
            "issuanceDate"      to now.toString(),
            "credentialSubject" to credentialSubject,
        )

        val vcBodyJson = objectMapper.writeValueAsString(vcBody)
        val proofValue = hmacSha256(vcBodyJson)

        val vcWithProof = vcBody + mapOf(
            "proof" to mapOf(
                "type"               to "HMAC-SHA256",
                "created"            to now.toString(),
                "verificationMethod" to "did:fabric:trustid:org1#key-1",
                "proofValue"         to proofValue,
            )
        )
        return objectMapper.writeValueAsString(vcWithProof)
    }

    /**
     * Phát hành TerminationVC khi nhân viên bị terminate.
     */
    fun issueTerminationVC(employee: EmployeeJpaEntity, revokedBy: String, reason: String): String {
        val now = Instant.now()
        val employeeId = employee.id.toString()
        val did = "did:fabric:trustid:$employeeId"

        val credentialSubject = mapOf(
            "id"               to did,
            "department"       to employee.department,
            "position"         to employee.position,
            "employmentStatus" to "TERMINATED",
            "terminationDate"  to now.toString(),
            "terminationReason" to reason,
            "revokedBy"        to revokedBy,
        )

        val vcBody = mapOf(
            "@context"          to listOf("https://www.w3.org/2018/credentials/v1"),
            "type"              to listOf("VerifiableCredential", "TerminationCredential"),
            "id"                to "vc:trustid:termination:$employeeId:${now.epochSecond}",
            "issuer"            to "did:fabric:trustid:org1",
            "issuanceDate"      to now.toString(),
            "credentialSubject" to credentialSubject,
        )

        val vcBodyJson = objectMapper.writeValueAsString(vcBody)
        val proofValue  = hmacSha256(vcBodyJson)

        val vcWithProof = vcBody + mapOf(
            "proof" to mapOf(
                "type"               to "HMAC-SHA256",
                "created"            to now.toString(),
                "verificationMethod" to "did:fabric:trustid:org1#key-1",
                "proofValue"         to proofValue,
            )
        )

        return objectMapper.writeValueAsString(vcWithProof)
    }

    private fun hmacSha256(data: String): String {
        val key = SecretKeySpec(vcSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256").apply { init(key) }
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    data class VcVerifyResult(val valid: Boolean, val reason: String)
}
