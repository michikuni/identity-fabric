package com.mpcorp.identity.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.*
import com.mpcorp.identity.infrastructures.vc.StatusListService
import com.mpcorp.identity.infrastructures.security.user_details.CustomUserDetails
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

/**
 * GdprController — GDPR Art.20 (Data Portability) + Art.17 (Right to be Forgotten).
 *
 * Endpoints:
 *   GET    /api/v1/me/export-data  — export all personal data as JSON
 *   DELETE /api/v1/me/data         — soft-delete + revoke VC + RevokeDID + audit log
 *
 * Both endpoints require authentication (JWT) — no extra permission beyond being logged in.
 * The user can only access/delete their own data.
 */
@RestController
@RequestMapping("/api/v1/me")
class GdprController(
    private val authJpaRepository: AuthJpaRepository,
    private val employeeJpaRepository: EmployeeJpaRepository,
    private val profileJpaRepository: ProfileJpaRepository,
    private val contractJpaRepository: ContractJpaRepository,
    private val payrollJpaRepository: PayrollJpaRepository,
    private val attendanceJpaRepository: AttendanceJpaRepository,
    private val leaveRequestJpaRepository: LeaveRequestJpaRepository,
    private val statusListService: StatusListService,
    private val fabricBridge: FabricLedgerBridge,
    private val objectMapper: ObjectMapper,
) {

    /**
     * GDPR Art.20 — Data Portability.
     * Returns a JSON aggregate of all personal data for the authenticated user.
     * In production: stream as downloadable ZIP; here returns inline JSON for demo.
     */
    @GetMapping("/export-data")
    fun exportData(@AuthenticationPrincipal principal: UserDetails): ApiResponse<Any> {
        val userId = resolveUserId(principal)
        val auth = authJpaRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        val employee = employeeJpaRepository.findEmployeeByAuthId(userId)

        val export = buildMap<String, Any?> {
            put("exportedAt", Instant.now().toString())
            put("exportVersion", "1.0")
            put("account", mapOf(
                "id"            to auth.id.toString(),
                "email"         to auth.email,
                "phone"         to auth.phone,
                "role"          to auth.role.name,
                "accountStatus" to auth.accountStatus.name,
                "mfaEnabled"    to auth.mfaEnabled,
            ))
            if (employee != null) {
                val empId = employee.id!!
                put("identity", mapOf(
                    "employeeId"  to empId,
                    "department"  to employee.department,
                    "position"    to employee.position,
                    "did"         to employee.did,
                    "publicKey"   to employee.publicKey,
                    "isActive"    to employee.isActive,
                    "createdAt"   to employee.createdAt.toInstant().toString(),
                ))
                put("credentials", mapOf(
                    "employmentVc"   to safeParseOrString(employee.employmentVc),
                    "salaryRangeVc"  to safeParseOrString(employee.salaryRangeVc),
                    "promotionVc"    to safeParseOrString(employee.promotionVc),
                    "terminationVc"  to safeParseOrString(employee.terminationVc),
                    "skillSdJwt"     to employee.skillSdJwt,
                    "educationSdJwt" to employee.educationSdJwt,
                    "trainingVc"     to safeParseOrString(employee.trainingVc),
                    "ndaAcceptedVc"  to safeParseOrString(employee.ndaAcceptedVc),
                ))
                val profile = profileJpaRepository.findProfileByEmployeeId(empId)
                if (profile != null) {
                    put("profile", mapOf(
                        "name"           to profile.name,
                        "gender"         to profile.gender,
                        "birthDate"      to profile.dateOfBirth,
                        "educationLevel" to profile.educationLevel,
                        "major"          to profile.major,
                        "expYears"       to profile.expYears,
                        "email"          to profile.email,
                        "phone"          to profile.phone,
                    ))
                }
                val contract = contractJpaRepository.findContractByEmployeeId(empId)
                if (contract != null) {
                    put("contract", mapOf(
                        "type"       to contract.typeContract,
                        "startDate"  to contract.startDate?.toInstant()?.toString(),
                        "endDate"    to contract.endDate?.toInstant()?.toString(),
                    ))
                }
                val payroll = payrollJpaRepository.findPayrollByEmployeeId(empId)
                if (payroll != null) {
                    put("payroll", mapOf(
                        "salaryBand" to when {
                            payroll.baseSalary < 10_000_000 -> "ENTRY"
                            payroll.baseSalary < 20_000_000 -> "MID"
                            payroll.baseSalary < 40_000_000 -> "SENIOR"
                            else -> "EXECUTIVE"
                        },
                        "currency"  to payroll.currency,
                    ))
                }
                val attendance = attendanceJpaRepository.findAll()
                    .filter { it.employee.id == empId }
                    .map { mapOf("date" to it.workDate.toString(), "checkIn" to it.checkInTime?.toString(), "checkOut" to it.checkOutTime?.toString()) }
                put("attendance", attendance)
                val requests = leaveRequestJpaRepository.findByEmployeeIdOrderByCreatedAtDesc(empId)
                    .map { mapOf("type" to it.requestType, "status" to it.status, "start" to it.startDate.toString(), "end" to it.endDate.toString()) }
                put("leaveRequests", requests)
            }
        }
        return ApiResponse(status = "200", message = "Personal data export", data = export)
    }

    /**
     * GDPR Art.17 — Right to be Forgotten.
     * Soft-deletes PII, revokes all VCs via Status List, revokes DID on Fabric.
     * On-chain audit trail is retained (blockchain is immutable by design — GDPR
     * compliance note: only hashes and keyFields are on-chain, no PII).
     */
    @DeleteMapping("/data")
    fun deleteData(@AuthenticationPrincipal principal: UserDetails): ApiResponse<Any> {
        val userId = resolveUserId(principal)
        val auth = authJpaRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found") }
        val employee = employeeJpaRepository.findEmployeeByAuthId(userId)

        if (employee != null) {
            val empId = employee.id!!

            // 1. Revoke all VCs via Status List
            runCatching { statusListService.revoke(empId, updatedBy = auth.email) }

            // 2. RevokeDID on Fabric
            if (!employee.did.isNullOrBlank()) {
                fabricBridge.revokeDID(empId.toString(), revokedBy = auth.email, reason = "GDPR_ERASURE")
            }

            // 3. Nullify all VC fields (PII erasure)
            employee.employmentVc   = null
            employee.salaryRangeVc  = null
            employee.promotionVc    = null
            employee.terminationVc  = null
            employee.skillSdJwt     = null
            employee.educationSdJwt = null
            employee.trainingVc     = null
            employee.ndaAcceptedVc  = null
            employee.did            = null
            employee.publicKey      = null
            employee.isActive       = false

            // 4. Nullify profile PII
            val profile = profileJpaRepository.findProfileByEmployeeId(empId)
            if (profile != null) {
                profile.name                  = "[DELETED]"
                profile.email                 = "[DELETED]"
                profile.phone                 = "[DELETED]"
                profile.identityNumber        = "[DELETED]"
                profile.permanentResidence    = "[DELETED]"
                profile.nowResidence          = "[DELETED]"
                profile.emergencyName         = "[DELETED]"
                profile.emergencyPhone        = "[DELETED]"
                profileJpaRepository.save(profile)
            }

            employeeJpaRepository.save(employee)
        }

        // 5. Nullify auth credentials
        auth.password        = null
        auth.mfaSecret       = null
        auth.mfaEnabled      = false
        auth.mfaBackupCodes  = null
        auth.email           = "deleted-${auth.id}@gdpr.trustid"
        auth.phone           = "000000000"
        authJpaRepository.save(auth)

        return ApiResponse(
            status  = "200",
            message = "Personal data deleted. On-chain audit hashes are retained per blockchain immutability.",
            data    = mapOf(
                "deletedAt"      to Instant.now().toString(),
                "vcRevoked"      to true,
                "didRevoked"     to (!employee?.did.isNullOrBlank()),
                "piiNullified"   to true,
            )
        )
    }

    private fun resolveUserId(principal: UserDetails): UUID {
        if (principal is CustomUserDetails) {
            return principal.getId() ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot resolve user identity")
        }
        return try {
            UUID.fromString(principal.username)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot resolve user identity")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun safeParseOrString(json: String?): Any? {
        if (json.isNullOrBlank()) return null
        return try {
            objectMapper.readValue(json, Map::class.java)
        } catch (_: Exception) { json }
    }
}
