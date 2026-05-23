package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.common.enums.AccountStatus
import com.mpcorp.identity.common.exception.UserNotFoundException
import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.domain.repository.AuthRepository
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.PayrollJpaEntity
import com.mpcorp.identity.infrastructures.persistence.mapper.toDomainEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.AttendanceJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.ContractJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.LeaveRequestJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.PayrollJpaRepository
import com.mpcorp.identity.infrastructures.vc.StatusListService
import com.mpcorp.identity.infrastructures.vc.VcIssuerService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN','CHIEF')")
class AdminController(
    private val employeeJpaRepository: EmployeeJpaRepository,
    private val attendanceJpaRepository: AttendanceJpaRepository,
    private val leaveRequestJpaRepository: LeaveRequestJpaRepository,
    private val authRepository: AuthRepository,
    private val fabricBridge: FabricLedgerBridge,
    private val vcIssuerService: VcIssuerService,
    private val statusListService: StatusListService,
    private val payrollJpaRepository: PayrollJpaRepository,
    private val contractJpaRepository: ContractJpaRepository,
) {
    data class UpsertPayrollRequest(
        val salaryType: String = "MONTHLY",
        val baseSalary: Double,
        val totalIncome: Double? = null,
        val bonusSalary: Double? = null,
        val overTimeRate: Double? = null,
        val currency: String = "VND",
        val payDay: String? = null,
        val bankAccountNumber: String? = null,
        val bankAccountName: String? = null,
        val bankName: String? = null,
        val bankBranch: String? = null,
    )

    data class UpsertContractRequest(
        val typeContract: String = "FULL_TIME",
        val startDate: String,
        val endDate: String? = null,
        val probationStartDate: String? = null,
        val probationEndDate: String? = null,
        val taxCode: String? = null,
        val socialInsuranceNumber: String? = null,
        val healthInsuranceNumber: String? = null,
    )

    /** Parse ISO-8601 string linh hoạt → Timestamp. Chấp nhận:
     *  - "2026-04-28T00:00:00.000" (Flutter toIso8601String không có Z)
     *  - "2026-04-28T00:00:00Z"    (có Z)
     *  - "2026-04-28"              (date only)
     */
    private fun parseTimestamp(raw: String?): Timestamp? {
        if (raw == null) return null
        return try {
            Timestamp.from(Instant.parse(if (raw.length == 10) "${raw}T00:00:00Z" else raw))
        } catch (_: Exception) {
            try {
                Timestamp.from(LocalDateTime.parse(raw.take(19)).toInstant(ZoneOffset.UTC))
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun Timestamp?.toIsoString(): String? = this?.toInstant()?.toString()
    @GetMapping("/dashboard")
    fun dashboard(): ApiResponse<Any> {
        val totalEmployees = employeeJpaRepository.count()
        val activeEmployees = employeeJpaRepository.findAll().count { it.isActive }
        val todayAttendance = attendanceJpaRepository.findAll()
            .count { it.workDate == LocalDate.now() && it.checkInTime != null }
        val pendingRequests = leaveRequestJpaRepository.findAll().count { it.status == "PENDING" }
        val pendingAccounts = authRepository.findByStatus(AccountStatus.PENDING).size

        return ApiResponse(
            status = "200", message = "OK",
            data = mapOf(
                "totalEmployees" to totalEmployees,
                "activeEmployees" to activeEmployees,
                "todayAttendance" to todayAttendance,
                "pendingRequests" to pendingRequests,
                "pendingAccounts" to pendingAccounts,
            )
        )
    }

    /**
     * Issuer Console KPIs — SSI-flavored metrics for Phase 0 / 3.3.
     * Aggregated from existing data:
     *  - credentialsIssued   = count of employees with any non-null VC field
     *  - employmentVcCount   = employees with employment VC
     *  - salaryVcCount       = employees with salary range VC
     *  - promotionVcCount    = employees with promotion VC
     *  - skillSdJwtCount     = employees with skill SD-JWT
     *  - educationSdJwtCount = employees with education SD-JWT
     *  - activeDids          = employees with DID && isActive
     *  - revokedThisMonth    = employees terminated this month (proxy for revoke count)
     *  - trustedIssuers      = static 1 (org1) until Trust Registry (Phase 1 / 4.6) ships
     *  - pendingAccounts     = mirrors dashboard for banner
     */
    @GetMapping("/issuer-stats")
    fun issuerStats(): ApiResponse<Any> {
        val all = employeeJpaRepository.findAll()
        val employmentVcCount = all.count { !it.employmentVc.isNullOrBlank() }
        val salaryVcCount     = all.count { !it.salaryRangeVc.isNullOrBlank() }
        val promotionVcCount  = all.count { !it.promotionVc.isNullOrBlank() }
        val skillSdJwtCount   = all.count {
            try {
                val field = it.javaClass.getDeclaredField("skillSdJwt")
                field.isAccessible = true
                !(field.get(it) as? String).isNullOrBlank()
            } catch (_: Exception) { false }
        }
        val educationSdJwtCount = all.count {
            try {
                val field = it.javaClass.getDeclaredField("educationSdJwt")
                field.isAccessible = true
                !(field.get(it) as? String).isNullOrBlank()
            } catch (_: Exception) { false }
        }
        val credentialsIssued = employmentVcCount + salaryVcCount + promotionVcCount +
                                skillSdJwtCount + educationSdJwtCount
        val activeDids = all.count { it.isActive && !it.did.isNullOrBlank() }

        val now = LocalDate.now()
        val revokedThisMonth = all.count {
            !it.terminationVc.isNullOrBlank() &&
            it.updatedAt.toLocalDateTime().toLocalDate().let { d ->
                d.year == now.year && d.month == now.month
            }
        }

        val pendingAccounts = authRepository.findByStatus(AccountStatus.PENDING).size
        val trustedIssuers = runCatching {
            fabricBridge.listTrustedIssuers().count { it["active"] == true }
        }.getOrElse { 1 }

        return ApiResponse(
            status = "200", message = "OK",
            data = mapOf(
                "credentialsIssued"   to credentialsIssued,
                "employmentVcCount"   to employmentVcCount,
                "salaryVcCount"       to salaryVcCount,
                "promotionVcCount"    to promotionVcCount,
                "skillSdJwtCount"     to skillSdJwtCount,
                "educationSdJwtCount" to educationSdJwtCount,
                "activeDids"          to activeDids,
                "revokedThisMonth"    to revokedThisMonth,
                "trustedIssuers"      to trustedIssuers,
                "pendingAccounts"     to pendingAccounts,
            )
        )
    }

    // ── Pending accounts management ───────────────────────────────────────────

    @GetMapping("/pending-accounts")
    fun getPendingAccounts(): ApiResponse<Any> {
        val accounts = authRepository.findByStatus(AccountStatus.PENDING).map {
            mapOf(
                "id"    to it.id.toString(),
                "email" to it.email,
                "phone" to it.phone,
                "role"  to it.role.name,
            )
        }
        return ApiResponse(status = "200", message = "OK", data = accounts)
    }

    @PutMapping("/accounts/{id}/approve")
    fun approveAccount(@PathVariable id: String): ApiResponse<Any> {
        val uuid = UUID.fromString(id)
        val updated = authRepository.updateStatus(uuid, AccountStatus.ACTIVE)

        // publicKeyJwk được nhân viên gửi lên lúc onboarding (POST /employee)
        // Admin chỉ approve — backend tự lấy key từ DB
        val employee = employeeJpaRepository.findEmployeeByAuthId(uuid)
        if (employee != null) {
            // Issue DID on Fabric (async) and persist DID into DB immediately
            // (DID is deterministic: did:fabric:trustid:<employeeId>)
            if (!employee.publicKey.isNullOrBlank()) {
                employee.did = "did:fabric:trustid:${employee.id}"
                fabricBridge.registerDID(
                    employeeId   = employee.id.toString(),
                    publicKeyJwk = employee.publicKey!!,
                    approvedBy   = updated.email,
                )
            }
            // Ensure status list bit is cleared (ACTIVE) before issuing the VC.
            // If this employeeId was previously revoked, the new VC must start fresh.
            employee.id?.let { statusListService.activate(it, updatedBy = updated.email) }
            // Issue EmploymentVC and persist
            val vc = vcIssuerService.issueEmploymentVC(employee)
            employee.employmentVc = vc
            employeeJpaRepository.save(employee)
            fabricBridge.upsertVcRecord(
                employeeId   = employee.id.toString(),
                vcRecordType = "EMPLOYMENT_VC",
                vcJsonOrCompact = vc,
                action       = "ISSUE",
                updatedBy    = updated.email,
            )
        }

        return ApiResponse(
            status = "200", message = "Account approved",
            data = mapOf("id" to updated.id.toString(), "accountStatus" to updated.accountStatus.name)
        )
    }

    /**
     * Issue SalaryRangeVC dựa trên payroll hiện tại của nhân viên.
     * PUT /api/v1/admin/employees/issue-salary-vc?email=...
     */
    @PutMapping("/employees/issue-salary-vc")
    fun issueSalaryRangeVC(@RequestParam email: String): ApiResponse<Any> {
        val employee = employeeJpaRepository.findEmployeeByAuthEmail(email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên với email: $email")
        val payroll = payrollJpaRepository.findPayrollByEmployeeId(employee.id!!)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Nhân viên chưa có payroll — vui lòng gán payroll trước")

        val vc = vcIssuerService.issueSalaryRangeVC(
            employee   = employee,
            baseSalary = payroll.baseSalary,
            currency   = payroll.currency,
        )
        employee.salaryRangeVc = vc
        employeeJpaRepository.save(employee)
        val actor = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication?.name ?: "system"
        fabricBridge.upsertVcRecord(
            employeeId      = employee.id.toString(),
            vcRecordType    = "SALARY_RANGE_VC",
            vcJsonOrCompact = vc,
            action          = "ISSUE",
            updatedBy       = actor,
        )
        return ApiResponse(status = "200", message = "SalaryRangeVC issued", data = mapOf("email" to email))
    }

    // ── Payroll management by employeeId ─────────────────────────────────────

    @GetMapping("/employees/{employeeId}/payroll")
    fun getPayroll(@PathVariable employeeId: Long): ApiResponse<Any> {
        val payroll = payrollJpaRepository.findPayrollByEmployeeId(employeeId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No payroll assigned yet")
        return ApiResponse(status = "200", message = "OK", data = mapOf(
            "salaryType" to payroll.salaryType,
            "baseSalary" to payroll.baseSalary,
            "totalIncome" to payroll.totalIncome,
            "bonusSalary" to payroll.bonusSalary,
            "overTimeRate" to payroll.overTimeRate,
            "currency" to payroll.currency,
            "payDay" to payroll.payDay.toIsoString(),
            "bankAccountNumber" to payroll.bankAccountNumber,
            "bankAccountName" to payroll.bankAccountName,
            "bankName" to payroll.bankName,
            "bankBranch" to payroll.bankBranch,
        ))
    }

    @PostMapping("/employees/{employeeId}/payroll")
    fun upsertPayroll(@PathVariable employeeId: Long, @RequestBody body: UpsertPayrollRequest): ApiResponse<Any> {
        val employee = employeeJpaRepository.findById(employeeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found")
        }
        val payDay = parseTimestamp(body.payDay) ?: Timestamp.from(Instant.now())
        val existing = payrollJpaRepository.findPayrollByEmployeeId(employeeId)
        val saved: PayrollJpaEntity
        val action: String
        if (existing != null) {
            existing.salaryType = body.salaryType
            existing.baseSalary = body.baseSalary
            existing.totalIncome = body.totalIncome ?: body.baseSalary
            existing.bonusSalary = body.bonusSalary
            existing.overTimeRate = body.overTimeRate
            existing.currency = body.currency
            existing.payDay = payDay
            existing.bankAccountNumber = body.bankAccountNumber ?: existing.bankAccountNumber
            existing.bankAccountName = body.bankAccountName ?: existing.bankAccountName
            existing.bankName = body.bankName ?: existing.bankName
            existing.bankBranch = body.bankBranch ?: existing.bankBranch
            saved = payrollJpaRepository.save(existing)
            action = "UPDATE"
        } else {
            saved = payrollJpaRepository.save(PayrollJpaEntity(
                employee = employee,
                salaryType = body.salaryType,
                baseSalary = body.baseSalary,
                totalIncome = body.totalIncome ?: body.baseSalary,
                bonusSalary = body.bonusSalary,
                overTimeRate = body.overTimeRate,
                currency = body.currency,
                payDay = payDay,
                bankAccountNumber = body.bankAccountNumber ?: "",
                bankAccountName = body.bankAccountName ?: "",
                bankName = body.bankName ?: "",
                bankBranch = body.bankBranch,
            ))
            action = "CREATE"
        }
        fabricBridge.upsertPayrollRecord(saved.toDomainEntity(), action = action)
        return ApiResponse(status = "200", message = "Payroll saved", data = mapOf("employeeId" to employeeId))
    }

    // ── Contract management by employeeId ────────────────────────────────────

    @GetMapping("/employees/{employeeId}/contract")
    fun getContract(@PathVariable employeeId: Long): ApiResponse<Any> {
        val contract = contractJpaRepository.findContractByEmployeeId(employeeId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No contract assigned yet")
        return ApiResponse(status = "200", message = "OK", data = mapOf(
            "typeContract" to contract.typeContract,
            "startDate" to contract.startDate.toIsoString(),
            "endDate" to contract.endDate.toIsoString(),
            "probationStartDate" to contract.probationStartDate.toIsoString(),
            "probationEndDate" to contract.probationEndDate.toIsoString(),
            "taxCode" to contract.taxCode,
            "socialInsuranceNumber" to contract.socialInsuranceNumber,
            "healthInsuranceNumber" to contract.healthInsuranceNumber,
        ))
    }

    @PutMapping("/employees/{employeeId}/contract")
    fun upsertContract(@PathVariable employeeId: Long, @RequestBody body: UpsertContractRequest): ApiResponse<Any> {
        val employee = employeeJpaRepository.findById(employeeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found")
        }
        val startDate = parseTimestamp(body.startDate)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid startDate")
        val existing = contractJpaRepository.findContractByEmployeeId(employeeId)
        val saved: com.mpcorp.identity.infrastructures.persistence.jpa_entity.ContractJpaEntity
        val action: String
        if (existing != null) {
            existing.typeContract = body.typeContract
            existing.startDate = startDate
            existing.endDate = parseTimestamp(body.endDate)
            existing.probationStartDate = parseTimestamp(body.probationStartDate)
            existing.probationEndDate = parseTimestamp(body.probationEndDate)
            if (body.taxCode != null) existing.taxCode = body.taxCode
            if (body.socialInsuranceNumber != null) existing.socialInsuranceNumber = body.socialInsuranceNumber
            if (body.healthInsuranceNumber != null) existing.healthInsuranceNumber = body.healthInsuranceNumber
            saved = contractJpaRepository.save(existing)
            action = "UPDATE"
        } else {
            saved = contractJpaRepository.save(
                com.mpcorp.identity.infrastructures.persistence.jpa_entity.ContractJpaEntity(
                    employee = employee,
                    typeContract = body.typeContract,
                    startDate = startDate,
                    endDate = parseTimestamp(body.endDate),
                    contractExpire = null,
                    probationStartDate = parseTimestamp(body.probationStartDate),
                    probationEndDate = parseTimestamp(body.probationEndDate),
                    taxCode = body.taxCode ?: "",
                    socialInsuranceNumber = body.socialInsuranceNumber,
                    healthInsuranceNumber = body.healthInsuranceNumber,
                )
            )
            action = "CREATE"
        }
        fabricBridge.upsertContractRecord(saved.toDomainEntity(), action = action)
        return ApiResponse(status = "200", message = "Contract saved", data = mapOf("employeeId" to employeeId))
    }

    // ── New VC types (Phase 2 / 5.3) ─────────────────────────────────────────

    data class IssueTrainingVcRequest(
        val trainingName: String,
        val provider: String,
        val completedDate: String,
        val score: String? = null,
    )

    data class IssueNdaVcRequest(
        val ndaTitle: String,
        val docHash: String,
        val signedDate: String,
    )

    /**
     * Phát hành TrainingVC khi nhân viên hoàn thành khoá đào tạo.
     * POST /api/v1/admin/employees/{employeeId}/issue-training-vc
     */
    @PostMapping("/employees/{employeeId}/issue-training-vc")
    fun issueTrainingVC(
        @PathVariable employeeId: Long,
        @RequestBody body: IssueTrainingVcRequest,
    ): ApiResponse<Any> {
        val employee = employeeJpaRepository.findById(employeeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found")
        }
        val vc = vcIssuerService.issueTrainingVC(
            employee      = employee,
            trainingName  = body.trainingName,
            provider      = body.provider,
            completedDate = body.completedDate,
            score         = body.score,
        )
        employee.trainingVc = vc
        employeeJpaRepository.save(employee)
        val actor = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication?.name ?: "system"
        fabricBridge.upsertVcRecord(
            employeeId      = employeeId.toString(),
            vcRecordType    = "TRAINING_VC",
            vcJsonOrCompact = vc,
            action          = "ISSUE",
            updatedBy       = actor,
        )
        return ApiResponse(
            status = "200", message = "TrainingVC issued",
            data = mapOf("employeeId" to employeeId, "vc" to vc)
        )
    }

    /**
     * Phát hành NDA-AcceptedVC khi nhân viên ký NDA.
     * POST /api/v1/admin/employees/{employeeId}/issue-nda-vc
     */
    @PostMapping("/employees/{employeeId}/issue-nda-vc")
    fun issueNdaVC(
        @PathVariable employeeId: Long,
        @RequestBody body: IssueNdaVcRequest,
    ): ApiResponse<Any> {
        val employee = employeeJpaRepository.findById(employeeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found")
        }
        val vc = vcIssuerService.issueNdaAcceptedVC(
            employee   = employee,
            ndaTitle   = body.ndaTitle,
            docHash    = body.docHash,
            signedDate = body.signedDate,
        )
        employee.ndaAcceptedVc = vc
        employeeJpaRepository.save(employee)
        val actor = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication?.name ?: "system"
        fabricBridge.upsertVcRecord(
            employeeId      = employeeId.toString(),
            vcRecordType    = "NDA_VC",
            vcJsonOrCompact = vc,
            action          = "ISSUE",
            updatedBy       = actor,
        )
        return ApiResponse(
            status = "200", message = "NDA-AcceptedVC issued",
            data = mapOf("employeeId" to employeeId, "vc" to vc)
        )
    }

    @PutMapping("/accounts/{id}/reject")
    fun rejectAccount(@PathVariable id: String): ApiResponse<Any> {
        val uuid = UUID.fromString(id)
        val updated = authRepository.updateStatus(uuid, AccountStatus.REJECTED)
        return ApiResponse(
            status = "200", message = "Account rejected",
            data = mapOf("id" to updated.id.toString(), "accountStatus" to updated.accountStatus.name)
        )
    }
}
