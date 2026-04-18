package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.common.enums.EmployeeRole
import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.AuthJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.sql.Timestamp
import java.time.Instant

@RestController
@RequestMapping("/api/v1/chief")
@PreAuthorize("hasAnyRole('CHIEF','ADMIN')")
class ChiefController(
    private val employeeJpaRepository: EmployeeJpaRepository,
    private val authJpaRepository: AuthJpaRepository,
    private val ledgerBridge: FabricLedgerBridge,
) {
    data class ChangeRoleRequest(val role: String)
    data class TerminateRequest(val reason: String)

    @GetMapping("/employees")
    fun listEmployees(
        @RequestParam(required = false) department: String?,
        @RequestParam(required = false) role: String?,
    ): ApiResponse<Any> {
        val all = employeeJpaRepository.findAll()
        val filtered = all.filter { emp ->
            (department == null || emp.department.contains(department, ignoreCase = true)) &&
            (role == null || emp.auth.role.name.equals(role, ignoreCase = true))
        }.map { emp ->
            mapOf(
                "id" to emp.id,
                "name" to (emp.profile?.name ?: emp.auth.email),
                "email" to emp.auth.email,
                "phone" to emp.auth.phone,
                "department" to emp.department,
                "position" to emp.position,
                "role" to emp.auth.role.name,
                "status" to emp.status,
                "isActive" to emp.isActive,
            )
        }
        return ApiResponse(status = "200", message = "OK", data = filtered)
    }

    @PutMapping("/employees/{id}/role")
    fun changeRole(@PathVariable id: Long, @RequestBody body: ChangeRoleRequest): ApiResponse<Any> {
        val actor = SecurityContextHolder.getContext().authentication?.name ?: "system"
        val emp = employeeJpaRepository.findById(id).orElseThrow(::EmployeeNotFoundException)
        emp.auth.role = EmployeeRole.valueOf(body.role.uppercase())
        authJpaRepository.save(emp.auth)
        ledgerBridge.logRequest(id.toString(), "ROLE_CHANGE", body.role, actor)
        return ApiResponse(status = "200", message = "Role updated", data = mapOf("id" to id, "newRole" to body.role))
    }

    @PutMapping("/employees/{id}/terminate")
    fun terminate(@PathVariable id: Long, @RequestBody body: TerminateRequest): ApiResponse<Any> {
        val actor = SecurityContextHolder.getContext().authentication?.name ?: "system"
        val emp = employeeJpaRepository.findById(id).orElseThrow(::EmployeeNotFoundException)
        emp.isActive = false
        emp.status = "TERMINATED"
        emp.updatedAt = Timestamp.from(Instant.now())
        employeeJpaRepository.save(emp)
        ledgerBridge.logRequest(id.toString(), "TERMINATION", "DELETE", actor)
        return ApiResponse(status = "200", message = "Employee terminated", data = mapOf("id" to id))
    }
}
