package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.application.dto.payroll.CreatePayrollCommand
import com.mpcorp.identity.application.dto.payroll.DeletePayrollCommand
import com.mpcorp.identity.application.dto.payroll.GetPayrollRequestCommand
import com.mpcorp.identity.application.dto.payroll.UpdatePayrollCommand
import com.mpcorp.identity.application.usecase.employee.ResolveCurrentEmployeeRefUseCase
import com.mpcorp.identity.application.usecase.payroll.CreatePayrollUseCase
import com.mpcorp.identity.application.usecase.payroll.DeletePayrollUseCase
import com.mpcorp.identity.application.usecase.payroll.GetPayrollUseCase
import com.mpcorp.identity.application.usecase.payroll.UpdatePayrollUseCase
import com.mpcorp.identity.common.constant.ErrorCodes
import com.mpcorp.identity.common.constant.StatusMessage
import com.mpcorp.identity.common.exception.PayrollNotFoundException
import com.mpcorp.identity.presentation.api.PayrollApi
import com.mpcorp.identity.presentation.mapper.toDto
import com.mpcorp.identity.presentation.mapper.toModel
import com.mpcorp.identity.presentation.request.payroll.CreatePayrollRequest
import com.mpcorp.identity.presentation.request.payroll.UpdatePayrollRequest
import com.mpcorp.identity.presentation.response.payroll.PayrollResponse
import com.mpcorp.identity.presentation.security.BearerAuthIdResolver
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.RestController

@RestController
class PayrollController(
    private val bearerAuthIdResolver: BearerAuthIdResolver,
    private val resolveCurrentEmployeeRefUseCase: ResolveCurrentEmployeeRefUseCase,
    private val createPayrollUseCase: CreatePayrollUseCase,
    private val updatePayrollUseCase: UpdatePayrollUseCase,
    private val getPayrollUseCase: GetPayrollUseCase,
    private val deletePayrollUseCase: DeletePayrollUseCase,
) : PayrollApi {
    override fun create(httpRequest: HttpServletRequest, request: CreatePayrollRequest): PayrollResponse {
        val payroll = createPayrollUseCase.execute(
            command = CreatePayrollCommand(
                employee = currentEmployeeRef(httpRequest),
                salaryType = request.salaryType,
                baseSalary = request.baseSalary,
                bonusSalary = request.bonusSalary,
                overTimeRate = request.overTimeRate,
                totalIncome = request.totalIncome,
                currency = request.currency,
                payDay = request.payDay,
                bankAccountNumber = request.bankAccountNumber,
                bankAccountName = request.bankAccountName,
                bankName = request.bankName,
                bankBranch = request.bankBranch,
            )
        )

        return PayrollResponse(
            status = ErrorCodes.CREATE_SUCCESS,
            message = StatusMessage.SUCCESS,
            data = payroll.toDto(),
        )
    }

    override fun update(httpRequest: HttpServletRequest, request: UpdatePayrollRequest): PayrollResponse {
        val payroll = updatePayrollUseCase.execute(
            command = UpdatePayrollCommand(
                payroll = request.payroll.toModel(),
                employee = currentEmployeeRef(httpRequest),
                salaryType = request.salaryType,
                baseSalary = request.baseSalary,
                bonusSalary = request.bonusSalary,
                overTimeRate = request.overTimeRate,
                totalIncome = request.totalIncome,
                currency = request.currency,
                payDay = request.payDay,
                bankAccountNumber = request.bankAccountNumber,
                bankAccountName = request.bankAccountName,
                bankName = request.bankName,
                bankBranch = request.bankBranch,
            )
        )

        return PayrollResponse(
            status = ErrorCodes.UPDATE_SUCCESS,
            message = StatusMessage.SUCCESS,
            data = payroll.toDto(),
        )
    }

    override fun get(httpRequest: HttpServletRequest): PayrollResponse {
        val payroll = getPayrollUseCase.execute(GetPayrollRequestCommand(employee = currentEmployeeRef(httpRequest)))
            ?: throw PayrollNotFoundException()

        return PayrollResponse(
            status = ErrorCodes.SUCCESS,
            message = StatusMessage.SUCCESS,
            data = payroll.toDto(),
        )
    }

    override fun delete(httpRequest: HttpServletRequest): PayrollResponse {
        deletePayrollUseCase.execute(DeletePayrollCommand(employee = currentEmployeeRef(httpRequest)))

        return PayrollResponse(
            status = ErrorCodes.DELETE_SUCCESS,
            message = StatusMessage.SUCCESS,
            data = null,
        )
    }

    private fun currentEmployeeRef(httpRequest: HttpServletRequest) =
        resolveCurrentEmployeeRefUseCase.execute(bearerAuthIdResolver.resolveAuthId(httpRequest))
}
