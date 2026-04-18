package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.common.enums.AccountStatus
import com.mpcorp.identity.common.exception.UserNotFoundException
import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.domain.repository.AuthRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.AttendanceJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.LeaveRequestJpaRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
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
        return ApiResponse(
            status = "200", message = "Account approved",
            data = mapOf("id" to updated.id.toString(), "accountStatus" to updated.accountStatus.name)
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
