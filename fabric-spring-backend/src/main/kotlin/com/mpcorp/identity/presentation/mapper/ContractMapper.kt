package com.mpcorp.identity.presentation.mapper

import com.mpcorp.identity.application.dto.contract.GetContractResponseCommand
import com.mpcorp.identity.presentation.model.EmployeeRefPayload
import com.mpcorp.identity.presentation.response.contract.ContractResponseData

fun GetContractResponseCommand.toDto(): ContractResponseData = ContractResponseData(
    id = id.value.toIdentifierPayload(),
    employee = EmployeeRefPayload(id = employee.id?.value?.toIdentifierPayload()),
    typeContract = typeContract,
    startDate = startDate,
    endDate = endDate,
    contractExpire = contractExpire,
    probationStartDate = probationStartDate,
    probationEndDate = probationEndDate,
    taxCode = taxCode,
    socialInsuranceNumber = socialInsuranceNumber,
    healthInsuranceNumber = healthInsuranceNumber,
)
