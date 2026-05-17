package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.application.dto.employee.CreateEmployeeCommand
import com.mpcorp.identity.application.dto.employee.UpdateEmployeeCommand
import com.mpcorp.identity.application.usecase.employee.CreateCurrentEmployeeUseCase
import com.mpcorp.identity.application.usecase.employee.DeleteCurrentEmployeeUseCase
import com.mpcorp.identity.application.usecase.employee.GetCurrentEmployeeUseCase
import com.mpcorp.identity.application.usecase.employee.UpdateCurrentEmployeeUseCase
import com.mpcorp.identity.common.constant.ErrorCodes
import com.mpcorp.identity.common.constant.StatusMessage
import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import com.mpcorp.identity.infrastructures.vc.StatusListService
import com.mpcorp.identity.infrastructures.vc.VcIssuerService
import com.mpcorp.identity.presentation.api.EmployeeApi
import com.mpcorp.identity.presentation.mapper.toDto
import com.mpcorp.identity.presentation.mapper.toModel
import com.mpcorp.identity.presentation.request.employee.CreateEmployeeRequest
import com.mpcorp.identity.presentation.request.employee.UpdateEmployeeRequest
import com.mpcorp.identity.presentation.response.employee.EmployeeResponse
import com.mpcorp.identity.presentation.security.BearerAuthIdResolver
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController

@RestController
class EmployeeController(
    private val bearerAuthIdResolver: BearerAuthIdResolver,
    private val createCurrentEmployeeUseCase: CreateCurrentEmployeeUseCase,
    private val getCurrentEmployeeUseCase: GetCurrentEmployeeUseCase,
    private val updateCurrentEmployeeUseCase: UpdateCurrentEmployeeUseCase,
    private val deleteCurrentEmployeeUseCase: DeleteCurrentEmployeeUseCase,
    private val employeeJpaRepository: EmployeeJpaRepository,
    private val fabricBridge: FabricLedgerBridge,
    private val vcIssuerService: VcIssuerService,
    private val statusListService: StatusListService,
) : EmployeeApi {
    override fun create(httpRequest: HttpServletRequest, request: CreateEmployeeRequest): EmployeeResponse {
        val authId = bearerAuthIdResolver.resolveAuthId(httpRequest)
        val username = currentUsername()
        val employee = createCurrentEmployeeUseCase.execute(
            username = username,
            command = CreateEmployeeCommand(
                department = request.department,
                position = request.position,
                status = request.status,
                workingType = request.workingType,
                isActive = request.isActive,
                manager = request.manager?.toModel(),
                createdBy = request.createdBy,
                note = request.note,
                publicKeyJwk = request.publicKeyJwk,
            )
        )

        // Trigger DID registration + EmploymentVC khi publicKey được nộp lần đầu
        if (!request.publicKeyJwk.isNullOrBlank()) {
            val emp = employeeJpaRepository.findEmployeeByAuthId(authId)
            if (emp != null && emp.employmentVc == null) {
                if (!emp.publicKey.isNullOrBlank()) {
                    fabricBridge.registerDID(
                        employeeId = emp.id.toString(),
                        publicKeyJwk = emp.publicKey!!,
                        approvedBy = username,
                    )
                }
                emp.id?.let { statusListService.activate(it, updatedBy = username) }
                emp.employmentVc = vcIssuerService.issueEmploymentVC(emp)
                employeeJpaRepository.save(emp)
            }
        }

        return EmployeeResponse(
            status = ErrorCodes.CREATE_SUCCESS,
            message = StatusMessage.SUCCESS,
            data = employee.toDto(),
        )
    }

    override fun get(httpRequest: HttpServletRequest): EmployeeResponse {
        val authId = bearerAuthIdResolver.resolveAuthId(httpRequest)
        val employee = getCurrentEmployeeUseCase.execute(authId)
        return EmployeeResponse(
            status = ErrorCodes.SUCCESS,
            message = StatusMessage.SUCCESS,
            data = employee.toDto(),
        )
    }

    override fun update(httpRequest: HttpServletRequest, request: UpdateEmployeeRequest): EmployeeResponse {
        val authId = bearerAuthIdResolver.resolveAuthId(httpRequest)
        val employee = updateCurrentEmployeeUseCase.execute(
            authId = authId,
            command = UpdateEmployeeCommand(
                department = request.department,
                position = request.position,
                status = request.status,
                workingType = request.workingType,
                isActive = request.isActive,
                manager = request.manager?.toModel(),
                updatedAt = request.updatedAt,
                note = request.note,
            )
        )

        return EmployeeResponse(
            status = ErrorCodes.UPDATE_SUCCESS,
            message = StatusMessage.SUCCESS,
            data = employee.toDto(),
        )
    }

    override fun delete(httpRequest: HttpServletRequest): EmployeeResponse {
        val authId = bearerAuthIdResolver.resolveAuthId(httpRequest)
        deleteCurrentEmployeeUseCase.execute(authId)
        return EmployeeResponse(
            status = ErrorCodes.DELETE_SUCCESS,
            message = StatusMessage.SUCCESS,
            data = null,
        )
    }

    private fun currentUsername(): String =
        SecurityContextHolder.getContext().authentication?.name ?: throw EmployeeNotFoundException()
}
