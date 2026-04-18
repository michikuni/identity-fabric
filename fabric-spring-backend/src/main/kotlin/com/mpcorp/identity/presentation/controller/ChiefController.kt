package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.common.enums.AccountStatus
import com.mpcorp.identity.common.enums.EmployeeRole
import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.common.exception.UserAlreadyExistingException
import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.AuthJpaEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.EmployeeJpaEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.AuthJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
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
    private val passwordEncoder: BCryptPasswordEncoder,
) {
    data class ChangeRoleRequest(val role: String)
    data class TerminateRequest(val reason: String)
    data class CreateEmployeeRequest(
        val email: String,
        val phone: String,
        val password: String,
        val role: String = "EMPLOYEE",
        val department: String,
        val position: String,
        val workingType: String = "FULL_TIME",
        val note: String? = null,
    )

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

    @PostMapping("/employees")
    fun createEmployee(@RequestBody body: CreateEmployeeRequest): ApiResponse<Any> {
        val actor = SecurityContextHolder.getContext().authentication?.name ?: "system"
        // Chỉ chặn nếu tài khoản cũ còn ACTIVE (nhân viên đang làm)
        // Cho phép tạo lại nếu tài khoản cũ đã bị terminate (isActive = false)
        fun isActiveAccount(username: String): Boolean {
            val auth = authJpaRepository.findUserByPhoneOrEmail(username) ?: return false
            val emp = employeeJpaRepository.findAll().find { it.auth.id == auth.id }
            return emp?.isActive != false
        }
        if (isActiveAccount(body.email) || isActiveAccount(body.phone)) {
            throw UserAlreadyExistingException()
        }
        val auth = authJpaRepository.save(AuthJpaEntity(
            email = body.email,
            phone = body.phone,
            password = passwordEncoder.encode(body.password),
            role = EmployeeRole.valueOf(body.role.uppercase()),
            accountStatus = AccountStatus.ACTIVE,
        ))
        val now = Timestamp.from(Instant.now())
        val emp = employeeJpaRepository.save(EmployeeJpaEntity(
            auth = auth,
            department = body.department,
            position = body.position,
            status = "ACTIVE",
            workingType = body.workingType,
            isActive = true,
            manager = null,
            createdAt = now,
            updatedAt = now,
            createdBy = actor,
            note = body.note,
        ))
        ledgerBridge.logRequest(emp.id.toString(), "EMPLOYEE_CREATE", "CREATE", actor)
        return ApiResponse(
            status = "201", message = "Employee created",
            data = mapOf(
                "id" to emp.id,
                "email" to auth.email,
                "role" to auth.role.name,
                "department" to emp.department,
                "position" to emp.position,
            )
        )
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
