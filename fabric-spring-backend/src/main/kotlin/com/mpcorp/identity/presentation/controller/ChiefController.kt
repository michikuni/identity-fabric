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
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.LeaveRequestJpaRepository
import com.mpcorp.identity.infrastructures.vc.StatusListService
import com.mpcorp.identity.infrastructures.vc.VcIssuerService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
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
    private val vcIssuerService: VcIssuerService,
    private val statusListService: StatusListService,
    private val leaveRequestJpaRepository: LeaveRequestJpaRepository,
) {
    data class ChangeRoleRequest(val role: String, val position: String? = null)
    data class TerminateRequest(val reason: String)
    data class AssignManagerRequest(val managerId: Long?)
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
                "managerId" to emp.manager?.id,
                "managerName" to (emp.manager?.profile?.name ?: emp.manager?.auth?.email),
            )
        }
        return ApiResponse(status = "200", message = "OK", data = filtered)
    }

    @PostMapping("/employees")
    @org.springframework.transaction.annotation.Transactional
    fun createEmployee(@RequestBody body: CreateEmployeeRequest): ApiResponse<Any> {
        val actor = SecurityContextHolder.getContext().authentication?.name ?: "system"
        val now = Timestamp.from(Instant.now())

        val byEmail = authJpaRepository.findUserByPhoneOrEmail(body.email)
        val byPhone = authJpaRepository.findUserByPhoneOrEmail(body.phone)

        // Chặn nếu tài khoản ACTIVE đã có employee đang làm việc
        listOfNotNull(byEmail, byPhone).distinctBy { it.id }.forEach { existingAuth ->
            if (existingAuth.accountStatus == AccountStatus.ACTIVE) {
                val existingEmp = employeeJpaRepository.findEmployeeByAuthId(existingAuth.id!!)
                if (existingEmp?.isActive == true) throw UserAlreadyExistingException()
            }
        }

        // Kích hoạt tài khoản PENDING hoặc tạo tài khoản mới ACTIVE
        val auth = (byEmail ?: byPhone)?.let { existing ->
            existing.accountStatus = AccountStatus.ACTIVE
            existing.role = EmployeeRole.valueOf(body.role.uppercase())
            authJpaRepository.save(existing)
        } ?: authJpaRepository.save(AuthJpaEntity(
            email = body.email,
            phone = body.phone,
            password = passwordEncoder.encode(body.password),
            role = EmployeeRole.valueOf(body.role.uppercase()),
            accountStatus = AccountStatus.ACTIVE,
        ))

        // Tạo mới hoặc kích hoạt lại employee profile
        val existingEmp = employeeJpaRepository.findEmployeeByAuthId(auth.id!!)
        val emp = if (existingEmp != null) {
            existingEmp.department = body.department
            existingEmp.position = body.position
            existingEmp.status = "ACTIVE"
            existingEmp.isActive = true
            existingEmp.workingType = body.workingType
            existingEmp.updatedAt = now
            existingEmp.note = body.note
            employeeJpaRepository.save(existingEmp)
        } else {
            employeeJpaRepository.save(EmployeeJpaEntity(
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
        }

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
        val oldPosition = emp.position
        emp.auth.role = EmployeeRole.valueOf(body.role.uppercase())
        // Update position to match new role if provided
        val newPosition = body.position ?: emp.position
        emp.position = newPosition
        authJpaRepository.save(emp.auth)
        // Issue PromotionVC if position actually changed
        var newPromotionVc: String? = null
        if (oldPosition != newPosition || body.position != null) {
            newPromotionVc = vcIssuerService.issuePromotionVC(
                employee    = emp,
                oldPosition = oldPosition,
                newPosition = newPosition,
                promotedBy  = actor,
            )
            emp.promotionVc = newPromotionVc
        }
        employeeJpaRepository.save(emp)
        newPromotionVc?.let { vc ->
            ledgerBridge.upsertVcRecord(
                employeeId      = id.toString(),
                vcRecordType    = "PROMOTION_VC",
                vcJsonOrCompact = vc,
                action          = "ISSUE",
                updatedBy       = actor,
            )
        }
        ledgerBridge.logRequest(id.toString(), "ROLE_CHANGE", body.role, actor)
        return ApiResponse(status = "200", message = "Role updated", data = mapOf("id" to id, "newRole" to body.role))
    }

    @PutMapping("/employees/{id}/manager")
    fun assignManager(@PathVariable id: Long, @RequestBody body: AssignManagerRequest): ApiResponse<Any> {
        val emp = employeeJpaRepository.findById(id).orElseThrow(::EmployeeNotFoundException)
        val manager = body.managerId?.let {
            employeeJpaRepository.findById(it).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Manager not found")
            }
        }
        emp.manager = manager
        emp.updatedAt = Timestamp.from(Instant.now())
        employeeJpaRepository.save(emp)
        return ApiResponse(
            status = "200", message = "Manager assigned",
            data = mapOf(
                "employeeId" to id,
                "managerId" to manager?.id,
                "managerName" to (manager?.profile?.name ?: manager?.auth?.email),
            )
        )
    }

    @Transactional(readOnly = true)
    @GetMapping("/requests")
    fun listAllRequests(
        @RequestParam(defaultValue = "false") pendingOnly: Boolean,
    ): ApiResponse<Any> {
        val requests = if (pendingOnly) {
            leaveRequestJpaRepository.findAll().filter { it.status == "PENDING" }
        } else {
            leaveRequestJpaRepository.findAll().sortedByDescending { it.createdAt }
        }
        val data = requests.map { r ->
            mapOf(
                "id" to r.id,
                "requestType" to r.requestType,
                "status" to r.status,
                "startDate" to r.startDate?.toString(),
                "endDate" to r.endDate?.toString(),
                "session" to r.session,
                "reason" to r.reason,
                "photoUrl" to r.photoUrl,
                "approverId" to r.approver?.id,
                "approverName" to (r.approver?.profile?.name ?: r.approver?.auth?.email),
                "employeeId" to r.employee?.id,
                "employeeName" to (r.employee?.profile?.name ?: r.employee?.auth?.email),
                "approvedAt" to r.approvedAt?.toInstant()?.toString(),
                "rejectedReason" to r.rejectedReason,
                "createdAt" to r.createdAt?.toInstant()?.toString(),
            )
        }
        return ApiResponse(status = "200", message = "OK", data = data)
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
        // Revoke DID on Fabric when employee is terminated
        ledgerBridge.revokeDID(employeeId = id.toString(), revokedBy = actor, reason = body.reason)
        // Flip the Status List 2021 bit so every VC referencing this index
        // (Employment / SalaryRange / Promotion) shows REVOKED to verifiers.
        emp.id?.let { statusListService.revoke(it, revokedBy = actor) }
        // Issue TerminationVC and persist
        val terminationVc = vcIssuerService.issueTerminationVC(emp, revokedBy = actor, reason = body.reason)
        emp.terminationVc = terminationVc
        employeeJpaRepository.save(emp)
        return ApiResponse(status = "200", message = "Employee terminated", data = mapOf("id" to id))
    }
}
