package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.common.enums.AccountStatus
import com.mpcorp.identity.common.exception.UserNotFoundException
import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.domain.repository.AuthRepository
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.AttendanceJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.LeaveRequestJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.PayrollJpaRepository
import com.mpcorp.identity.infrastructures.vc.VcIssuerService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
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
    private val payrollJpaRepository: PayrollJpaRepository,
) {
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
            // Issue DID on Fabric (async)
            if (!employee.publicKey.isNullOrBlank()) {
                fabricBridge.registerDID(
                    employeeId   = employee.id.toString(),
                    publicKeyJwk = employee.publicKey!!,
                    approvedBy   = updated.email,
                )
            }
            // Issue EmploymentVC and persist
            val vc = vcIssuerService.issueEmploymentVC(employee)
            employee.employmentVc = vc
            employeeJpaRepository.save(employee)
        }

        return ApiResponse(
            status = "200", message = "Account approved",
            data = mapOf("id" to updated.id.toString(), "accountStatus" to updated.accountStatus.name)
        )
    }

    /**
     * Issue SalaryRangeVC dựa trên payroll hiện tại của nhân viên.
     * PUT /api/v1/admin/employees/{employeeId}/issue-salary-vc
     */
    @PutMapping("/employees/{employeeId}/issue-salary-vc")
    fun issueSalaryRangeVC(@PathVariable employeeId: Long): ApiResponse<Any> {
        val employee = employeeJpaRepository.findById(employeeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found")
        }
        val payroll = payrollJpaRepository.findPayrollByEmployeeId(employeeId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No payroll found for employee — assign payroll first")

        val vc = vcIssuerService.issueSalaryRangeVC(
            employee   = employee,
            baseSalary = payroll.baseSalary,
            currency   = payroll.currency,
        )
        employee.salaryRangeVc = vc
        employeeJpaRepository.save(employee)
        return ApiResponse(status = "200", message = "SalaryRangeVC issued", data = mapOf("employeeId" to employeeId))
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
