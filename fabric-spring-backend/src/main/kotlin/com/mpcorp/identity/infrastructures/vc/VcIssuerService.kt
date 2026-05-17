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
 * VC format: W3C Verifiable Credentials Data Model v1.1 (simplified).
 *
 * ## Revocation (W3C Status List 2021)
 *
 * Mỗi VC được phát ra với block `credentialStatus` trỏ đến bit duy nhất
 * trong một Status List. Khi Admin/Chief revoke (terminate), StatusListService
 * lật bit 0→1 và đẩy snapshot mới lên Fabric. Verifier resolve URL
 * `statusListCredential` để lấy bitstring và kiểm tra bit tại
 * `statusListIndex`. Xem [StatusListService] để biết chi tiết.
 *
 * Quy ước index: `statusListIndex = employee.id` (Long, đảm bảo unique và
 * duy trì trong toàn bộ vòng đời của nhân viên — không phát lại index khi
 * tái phát VC).
 */
@Service
class VcIssuerService(
    private val objectMapper: ObjectMapper,
    private val statusListService: StatusListService,
    private val sdJwtIssuer: SdJwtIssuer,
    @Value("\${vc.secret}") private val vcSecret: String,
) {

    /**
     * Quy ước index: dùng `employee.id` (Long) làm statusListIndex.
     * Trả về null nếu employee chưa có id (chưa persist) — caller cần persist trước.
     */
    private fun statusListIndexFor(employeeId: Long?): Long? = employeeId?.takeIf { it >= 0 }

    /**
     * Build credentialStatus block cho VC nếu employee đã có id hợp lệ,
     * ngược lại trả null (Jackson sẽ bỏ qua field null khi serialize nếu
     * không có trong map — caller append có điều kiện).
     */
    private fun credentialStatusFor(employeeId: Long?): Map<String, Any>? {
        val index = statusListIndexFor(employeeId) ?: return null
        return statusListService.buildCredentialStatus(index)
    }

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

        val vcBody = buildMap<String, Any> {
            put("@context",          listOf("https://www.w3.org/2018/credentials/v1"))
            put("type",              listOf("VerifiableCredential", "EmploymentCredential"))
            put("id",                "vc:trustid:employment:$employeeId:${now.epochSecond}")
            put("issuer",            "did:fabric:trustid:org1")
            put("issuanceDate",      now.toString())
            put("expirationDate",    expiry.toString())
            put("credentialSubject", credentialSubject)
            credentialStatusFor(employee.id)?.let { put("credentialStatus", it) }
        }

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

            // Status List 2021 revocation check
            val credentialStatus = vc["credentialStatus"] as? Map<*, *>
            if (credentialStatus != null && credentialStatus["type"] == "StatusList2021Entry") {
                val statusUrl   = credentialStatus["statusListCredential"] as? String
                val indexString = credentialStatus["statusListIndex"] as? String
                val listId      = statusUrl?.substringAfterLast("/")
                val index       = indexString?.toLongOrNull()
                if (listId != null && index != null) {
                    if (statusListService.isRevoked(index, listId)) {
                        return VcVerifyResult(false, "VC revoked (status list $listId index $index)")
                    }
                }
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

        val vcBody = buildMap<String, Any> {
            put("@context",          listOf("https://www.w3.org/2018/credentials/v1"))
            put("type",              listOf("VerifiableCredential", "SalaryRangeCredential"))
            put("id",                "vc:trustid:salary:$employeeId:${now.epochSecond}")
            put("issuer",            "did:fabric:trustid:org1")
            put("issuanceDate",      now.toString())
            put("expirationDate",    expiry.toString())
            put("credentialSubject", credentialSubject)
            credentialStatusFor(employee.id)?.let { put("credentialStatus", it) }
        }

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

        val vcBody = buildMap<String, Any> {
            put("@context",          listOf("https://www.w3.org/2018/credentials/v1"))
            put("type",              listOf("VerifiableCredential", "PromotionCredential"))
            put("id",                "vc:trustid:promotion:$employeeId:${now.epochSecond}")
            put("issuer",            "did:fabric:trustid:org1")
            put("issuanceDate",      now.toString())
            put("credentialSubject", credentialSubject)
            credentialStatusFor(employee.id)?.let { put("credentialStatus", it) }
        }

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

        val vcBody = buildMap<String, Any> {
            put("@context",          listOf("https://www.w3.org/2018/credentials/v1"))
            put("type",              listOf("VerifiableCredential", "TerminationCredential"))
            put("id",                "vc:trustid:termination:$employeeId:${now.epochSecond}")
            put("issuer",            "did:fabric:trustid:org1")
            put("issuanceDate",      now.toString())
            put("credentialSubject", credentialSubject)
            credentialStatusFor(employee.id)?.let { put("credentialStatus", it) }
        }

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

    /**
     * Phát hành SkillCredential dưới dạng SD-JWT (selective disclosure).
     *
     * Mỗi skill là một disclosure độc lập — holder có thể chọn lộ 3/10 skill
     * cho Verifier mà vẫn giữ chữ ký Issuer hợp lệ.
     *
     * @param employee  nhân viên sở hữu credential
     * @param skills    map { skillName → proficiency } (e.g. {"Kotlin": "ADVANCED", "Spring": "INTERMEDIATE"})
     * @param issuedBy  ai phát (lưu vào claim luôn hiển thị)
     */
    fun issueSkillSdJwt(
        employee: EmployeeJpaEntity,
        skills: Map<String, Any?>,
        issuedBy: String = "did:fabric:trustid:org1",
    ): SdJwtIssuer.IssuedSdJwt {
        require(skills.isNotEmpty()) { "skills must not be empty" }
        val employeeId = employee.id.toString()
        val did = "did:fabric:trustid:$employeeId"
        return sdJwtIssuer.issue(
            vct             = "SkillCredential",
            subjectDid      = did,
            selectiveClaims = skills,
            alwaysVisible   = mapOf(
                "department" to employee.department,
                "position"   to employee.position,
                "issuedBy"   to issuedBy,
            ),
            credentialStatus = credentialStatusFor(employee.id),
        )
    }

    /**
     * Phát hành EducationCredential dưới dạng SD-JWT.
     *
     * Common selective claims: degree, major, university, gpa, graduationYear, honors.
     */
    fun issueEducationSdJwt(
        employee: EmployeeJpaEntity,
        education: Map<String, Any?>,
        issuedBy: String = "did:fabric:trustid:org1",
    ): SdJwtIssuer.IssuedSdJwt {
        require(education.isNotEmpty()) { "education claims must not be empty" }
        val employeeId = employee.id.toString()
        val did = "did:fabric:trustid:$employeeId"
        return sdJwtIssuer.issue(
            vct             = "EducationCredential",
            subjectDid      = did,
            selectiveClaims = education,
            alwaysVisible   = mapOf(
                "issuedBy" to issuedBy,
            ),
            credentialStatus = credentialStatusFor(employee.id),
        )
    }

    /**
     * Phát hành TrainingVC khi nhân viên hoàn thành một khoá đào tạo.
     *
     * @param employee     nhân viên sở hữu credential
     * @param trainingName tên khoá đào tạo (e.g. "AWS Solutions Architect")
     * @param provider     đơn vị đào tạo (e.g. "AWS Training", "Coursera")
     * @param completedDate ngày hoàn thành ISO-8601 string
     * @param score        điểm (nếu có), null nếu không có đánh giá
     * @param issuedBy     DID của issuer
     */
    fun issueTrainingVC(
        employee: EmployeeJpaEntity,
        trainingName: String,
        provider: String,
        completedDate: String,
        score: String? = null,
        issuedBy: String = "did:fabric:trustid:org1",
    ): String {
        val now = Instant.now()
        val expiry = now.plus(365 * 3, ChronoUnit.DAYS)
        val employeeId = employee.id.toString()
        val did = "did:fabric:trustid:$employeeId"

        val credentialSubject = buildMap<String, Any?> {
            put("id",           did)
            put("department",   employee.department)
            put("position",     employee.position)
            put("trainingName", trainingName)
            put("provider",     provider)
            put("completedDate", completedDate)
            put("status",       "COMPLETED")
            if (score != null) put("score", score)
        }

        val vcBody = buildMap<String, Any> {
            put("@context",          listOf("https://www.w3.org/2018/credentials/v1"))
            put("type",              listOf("VerifiableCredential", "TrainingCredential"))
            put("id",                "vc:trustid:training:$employeeId:${now.epochSecond}")
            put("issuer",            issuedBy)
            put("issuanceDate",      now.toString())
            put("expirationDate",    expiry.toString())
            put("credentialSubject", credentialSubject)
            credentialStatusFor(employee.id)?.let { put("credentialStatus", it) }
        }

        val vcBodyJson = objectMapper.writeValueAsString(vcBody)
        val proofValue = hmacSha256(vcBodyJson)
        val vcWithProof = vcBody + mapOf(
            "proof" to mapOf(
                "type"               to "HMAC-SHA256",
                "created"            to now.toString(),
                "verificationMethod" to "$issuedBy#key-1",
                "proofValue"         to proofValue,
            )
        )
        return objectMapper.writeValueAsString(vcWithProof)
    }

    /**
     * Phát hành NDA-AcceptedVC khi nhân viên ký kết NDA.
     *
     * Không lộ nội dung NDA — chỉ xác nhận "đã ký NDA với tổ chức issuer".
     * docHash giúp Verifier confirm NDA version mà không đọc nội dung.
     *
     * @param employee   nhân viên sở hữu credential
     * @param ndaTitle   tiêu đề NDA (e.g. "General Employee NDA 2026")
     * @param docHash    SHA-256 hex của PDF NDA (tính phía backend trước khi gọi)
     * @param signedDate ngày ký ISO-8601 string
     * @param issuedBy   DID của issuer
     */
    fun issueNdaAcceptedVC(
        employee: EmployeeJpaEntity,
        ndaTitle: String,
        docHash: String,
        signedDate: String,
        issuedBy: String = "did:fabric:trustid:org1",
    ): String {
        val now = Instant.now()
        val employeeId = employee.id.toString()
        val did = "did:fabric:trustid:$employeeId"

        val credentialSubject = mapOf(
            "id"         to did,
            "ndaTitle"   to ndaTitle,
            "docHash"    to docHash,
            "signedDate" to signedDate,
            "status"     to "ACCEPTED",
            "issuedBy"   to issuedBy,
        )

        val vcBody = buildMap<String, Any> {
            put("@context",          listOf("https://www.w3.org/2018/credentials/v1"))
            put("type",              listOf("VerifiableCredential", "NDAAcceptedCredential"))
            put("id",                "vc:trustid:nda:$employeeId:${now.epochSecond}")
            put("issuer",            issuedBy)
            put("issuanceDate",      now.toString())
            put("credentialSubject", credentialSubject)
            credentialStatusFor(employee.id)?.let { put("credentialStatus", it) }
        }

        val vcBodyJson = objectMapper.writeValueAsString(vcBody)
        val proofValue = hmacSha256(vcBodyJson)
        val vcWithProof = vcBody + mapOf(
            "proof" to mapOf(
                "type"               to "HMAC-SHA256",
                "created"            to now.toString(),
                "verificationMethod" to "$issuedBy#key-1",
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
