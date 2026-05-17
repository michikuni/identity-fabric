package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import com.mpcorp.identity.infrastructures.vc.SdJwtIssuer
import com.mpcorp.identity.infrastructures.vc.SdJwtVerifier
import com.mpcorp.identity.infrastructures.vc.VcIssuerService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

/**
 * SdJwtController — endpoints for SD-JWT Selective Disclosure flows.
 *
 * Three responsibilities, split across endpoints to keep authorization clean:
 *
 *   POST /api/v1/sd-jwt/issue/skill        — Admin/Chief issues a SkillCredential
 *   POST /api/v1/sd-jwt/issue/education    — Admin/Chief issues an EducationCredential
 *   GET  /api/v1/sd-jwt/{employeeId}/skill — Public: holder/verifier fetches the issued SD-JWT
 *   POST /api/v1/sd-jwt/present            — Public: holder builds a selective presentation
 *   POST /api/v1/sd-jwt/verify             — Public: verifier checks a presentation
 *
 * The "present" endpoint is a convenience used by the verifier portal / demo —
 * the real flow has the wallet build the presentation locally, but for the PoC
 * we keep a server-side helper so we can show all sides end-to-end without
 * shipping the SD-JWT logic into Flutter on day one.
 */
@RestController
@RequestMapping("/api/v1/sd-jwt")
class SdJwtController(
    private val vcIssuerService: VcIssuerService,
    private val sdJwtIssuer: SdJwtIssuer,
    private val sdJwtVerifier: SdJwtVerifier,
    private val employeeJpaRepository: EmployeeJpaRepository,
) {

    // ── DTOs ──────────────────────────────────────────────────────────────────

    data class IssueSkillRequest(
        /** map { skill_name → proficiency / level / years }. e.g. {"Kotlin": "ADVANCED"} */
        val skills: Map<String, Any?>,
    )

    data class IssueEducationRequest(
        /** map of education claims, e.g. {"degree":"BSc","university":"HUST","graduationYear":2024} */
        val education: Map<String, Any?>,
    )

    data class IssueGenericRequest(
        val vct: String,
        val subjectDid: String,
        val selectiveClaims: Map<String, Any?>,
        val alwaysVisible: Map<String, Any?> = emptyMap(),
    )

    data class PresentRequest(
        val sdJwt: String,
        /** Names of claims the holder wishes to disclose. */
        val reveal: List<String>,
    )

    data class VerifyRequest(
        val presentation: String,
        /** Optional: verifier asserts these claims must be present. */
        val requireClaims: List<String> = emptyList(),
    )

    // ── Issue endpoints (Admin/Chief only) ────────────────────────────────────

    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    @PostMapping("/issue/skill/{employeeId}")
    fun issueSkill(
        @PathVariable employeeId: Long,
        @RequestBody body: IssueSkillRequest,
    ): ApiResponse<Map<String, Any?>> {
        val employee = employeeJpaRepository.findById(employeeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found")
        }
        val issued = vcIssuerService.issueSkillSdJwt(employee, body.skills)
        employee.skillSdJwt = issued.sdJwt
        employeeJpaRepository.save(employee)
        return ApiResponse(
            status  = "200",
            message = "SkillCredential SD-JWT issued",
            data    = mapOf(
                "employeeId"  to employeeId,
                "sdJwt"       to issued.sdJwt,
                "disclosures" to issued.disclosures,
                "claims"      to issued.claims.map { (k, v) -> mapOf("name" to k, "value" to v) },
            )
        )
    }

    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    @PostMapping("/issue/education/{employeeId}")
    fun issueEducation(
        @PathVariable employeeId: Long,
        @RequestBody body: IssueEducationRequest,
    ): ApiResponse<Map<String, Any?>> {
        val employee = employeeJpaRepository.findById(employeeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found")
        }
        val issued = vcIssuerService.issueEducationSdJwt(employee, body.education)
        employee.educationSdJwt = issued.sdJwt
        employeeJpaRepository.save(employee)
        return ApiResponse(
            status  = "200",
            message = "EducationCredential SD-JWT issued",
            data    = mapOf(
                "employeeId"  to employeeId,
                "sdJwt"       to issued.sdJwt,
                "disclosures" to issued.disclosures,
                "claims"      to issued.claims.map { (k, v) -> mapOf("name" to k, "value" to v) },
            )
        )
    }

    /**
     * Generic issuer — useful when the caller (test harness, integration partner)
     * doesn't fit the SkillVC / EducationVC mould but still wants SD-JWT semantics.
     */
    @PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
    @PostMapping("/issue")
    fun issueGeneric(@RequestBody body: IssueGenericRequest): ApiResponse<Map<String, Any?>> {
        val issued = sdJwtIssuer.issue(
            vct             = body.vct,
            subjectDid      = body.subjectDid,
            selectiveClaims = body.selectiveClaims,
            alwaysVisible   = body.alwaysVisible,
        )
        return ApiResponse(
            status  = "200",
            message = "SD-JWT issued",
            data    = mapOf(
                "sdJwt"       to issued.sdJwt,
                "disclosures" to issued.disclosures,
                "claims"      to issued.claims.map { (k, v) -> mapOf("name" to k, "value" to v) },
            )
        )
    }

    // ── Holder / Verifier endpoints (public — no auth) ────────────────────────

    /**
     * Fetch issued SkillCredential SD-JWT for a given employee.
     * Holder (mobile wallet) uses this on first sync.
     */
    @GetMapping("/{employeeId}/skill")
    fun getSkill(@PathVariable employeeId: Long): ApiResponse<Map<String, String>> {
        val emp = employeeJpaRepository.findById(employeeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found")
        }
        val sdJwt = emp.skillSdJwt
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "SkillCredential not issued yet")
        return ApiResponse(status = "200", message = "OK", data = mapOf("sdJwt" to sdJwt))
    }

    @GetMapping("/{employeeId}/education")
    fun getEducation(@PathVariable employeeId: Long): ApiResponse<Map<String, String>> {
        val emp = employeeJpaRepository.findById(employeeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found")
        }
        val sdJwt = emp.educationSdJwt
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "EducationCredential not issued yet")
        return ApiResponse(status = "200", message = "OK", data = mapOf("sdJwt" to sdJwt))
    }

    /**
     * Build a presentation by stripping disclosures the holder doesn't want to share.
     *
     * Real wallets do this locally; we expose the server-side helper for the
     * demo + the verifier portal mock holder. The Verifier never calls this —
     * it would defeat the privacy guarantee.
     */
    @PostMapping("/present")
    fun present(@RequestBody body: PresentRequest): ApiResponse<Map<String, Any?>> {
        val issued = parseAsIssued(body.sdJwt)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed SD-JWT")
        val presentation = sdJwtIssuer.present(issued, body.reveal)
        return ApiResponse(
            status  = "200",
            message = "Presentation built",
            data    = mapOf(
                "presentation" to presentation,
                "revealed"     to body.reveal,
                "hidden"       to issued.disclosures
                    .mapNotNull { sdJwtIssuer.parseDisclosure(it)?.second }
                    .filter { it !in body.reveal },
            )
        )
    }

    /**
     * Verify an SD-JWT presentation. Public — anyone can verify (the whole point).
     */
    @PostMapping("/verify")
    fun verify(@RequestBody body: VerifyRequest): ApiResponse<Map<String, Any?>> {
        val result = sdJwtVerifier.verify(body.presentation, body.requireClaims)
        return ApiResponse(
            status  = "200",
            message = "OK",
            data    = mapOf(
                "valid"           to result.valid,
                "reason"          to result.reason,
                "issuer"          to result.issuer,
                "subject"         to result.subject,
                "vct"             to result.vct,
                "disclosedClaims" to result.disclosedClaims,
                "alwaysVisible"   to result.alwaysVisible,
            )
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Rebuild an [SdJwtIssuer.IssuedSdJwt] from a stored SD-JWT string. The
     * present() helper only needs the JWT prefix and the list of disclosures.
     */
    private fun parseAsIssued(sdJwt: String): SdJwtIssuer.IssuedSdJwt? {
        if (!sdJwt.contains("~")) return null
        val parts = sdJwt.split("~")
        val disclosures = parts.drop(1).filter { it.isNotEmpty() && !it.contains('.') }
        val claims = disclosures.mapNotNull { d ->
            sdJwtIssuer.parseDisclosure(d)?.let { (_, name, value) -> name to value }
        }
        return SdJwtIssuer.IssuedSdJwt(sdJwt = sdJwt, disclosures = disclosures, claims = claims)
    }
}
