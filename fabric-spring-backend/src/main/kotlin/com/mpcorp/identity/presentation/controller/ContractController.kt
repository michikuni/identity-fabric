package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.application.dto.contract.CreateContractCommand
import com.mpcorp.identity.application.dto.contract.DeleteContractCommand
import com.mpcorp.identity.application.dto.contract.GetContractRequestCommand
import com.mpcorp.identity.application.dto.contract.UpdateContractCommand
import com.mpcorp.identity.application.usecase.contract.CreateContractUseCase
import com.mpcorp.identity.application.usecase.contract.DeleteContractUseCase
import com.mpcorp.identity.application.usecase.contract.GetContractUseCase
import com.mpcorp.identity.application.usecase.contract.UpdateContractUseCase
import com.mpcorp.identity.application.usecase.employee.ResolveCurrentEmployeeRefUseCase
import com.mpcorp.identity.common.constant.ErrorCodes
import com.mpcorp.identity.common.constant.StatusMessage
import com.mpcorp.identity.common.exception.ContractNotFoundException
import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.presentation.api.ContractApi
import com.mpcorp.identity.presentation.mapper.toDto
import com.mpcorp.identity.presentation.mapper.toModel
import com.mpcorp.identity.presentation.request.contract.CreateContractRequest
import com.mpcorp.identity.presentation.request.contract.UpdateContractRequest
import com.mpcorp.identity.presentation.response.contract.ContractResponse
import com.mpcorp.identity.presentation.security.BearerAuthIdResolver
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.RestController

@RestController
class ContractController(
    private val bearerAuthIdResolver: BearerAuthIdResolver,
    private val resolveCurrentEmployeeRefUseCase: ResolveCurrentEmployeeRefUseCase,
    private val createContractUseCase: CreateContractUseCase,
    private val updateContractUseCase: UpdateContractUseCase,
    private val getContractUseCase: GetContractUseCase,
    private val deleteContractUseCase: DeleteContractUseCase,
) : ContractApi {

    override fun createContract(httpRequest: HttpServletRequest, request: CreateContractRequest): ContractResponse {
        val contract = createContractUseCase.execute(
            CreateContractCommand(
                employee = currentEmployeeRef(httpRequest),
                typeContract = request.typeContract,
                startDate = request.startDate,
                endDate = request.endDate,
                contractExpire = request.contractExpire,
                probationStartDate = request.probationStartDate,
                probationEndDate = request.probationEndDate,
                taxCode = request.taxCode,
                socialInsuranceNumber = request.socialInsuranceNumber,
                healthInsuranceNumber = request.healthInsuranceNumber,
            )
        )

        return ContractResponse(
            status = ErrorCodes.CREATE_SUCCESS,
            message = StatusMessage.SUCCESS,
            data = contract.toDto(),
        )
    }

    override fun updateContract(httpRequest: HttpServletRequest, request: UpdateContractRequest): ContractResponse {
        val contract = updateContractUseCase.execute(
            UpdateContractCommand(
                contract = request.contract.toModel(),
                employee = currentEmployeeRef(httpRequest),
                typeContract = request.typeContract,
                startDate = request.startDate,
                endDate = request.endDate,
                contractExpire = request.contractExpire,
                probationStartDate = request.probationStartDate,
                probationEndDate = request.probationEndDate,
                taxCode = request.taxCode,
                socialInsuranceNumber = request.socialInsuranceNumber,
                healthInsuranceNumber = request.healthInsuranceNumber,
            )
        )

        return ContractResponse(
            status = ErrorCodes.UPDATE_SUCCESS,
            message = StatusMessage.SUCCESS,
            data = contract.toDto(),
        )
    }

    override fun getContract(httpRequest: HttpServletRequest): ContractResponse {
        val contract = getContractUseCase.execute(GetContractRequestCommand(employee = currentEmployeeRef(httpRequest)))
            ?: throw ContractNotFoundException()

        return ContractResponse(
            status = ErrorCodes.SUCCESS,
            message = StatusMessage.SUCCESS,
            data = contract.toDto(),
        )
    }

    override fun deleteContract(httpRequest: HttpServletRequest): ContractResponse {
        deleteContractUseCase.execute(DeleteContractCommand(employee = currentEmployeeRef(httpRequest)))
        return ContractResponse(
            status = ErrorCodes.DELETE_SUCCESS,
            message = StatusMessage.SUCCESS,
            data = null,
        )
    }

    private fun currentEmployeeRef(httpRequest: HttpServletRequest) =
        resolveCurrentEmployeeRefUseCase.execute(bearerAuthIdResolver.resolveAuthId(httpRequest))
}
