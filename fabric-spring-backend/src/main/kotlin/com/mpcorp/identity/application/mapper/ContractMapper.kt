package com.mpcorp.identity.application.mapper

import com.mpcorp.identity.application.dto.contract.CreateContractCommand
import com.mpcorp.identity.application.dto.contract.GetContractResponseCommand
import com.mpcorp.identity.application.dto.contract.UpdateContractCommand
import com.mpcorp.identity.application.mapper.toRefModel
import com.mpcorp.identity.application.references.ContractRefModel
import com.mpcorp.identity.application.references.IdentifierModel
import com.mpcorp.identity.application.support.toLongValue
import com.mpcorp.identity.domain.entity.ContractEntity
import com.mpcorp.identity.domain.entity.EmployeeEntity

fun com.mpcorp.identity.application.dto.contract.CreateContractCommand.toDomainEntity(employee: com.mpcorp.identity.domain.entity.EmployeeEntity): com.mpcorp.identity.domain.entity.ContractEntity =
    _root_ide_package_.com.mpcorp.identity.domain.entity.ContractEntity(
        employee = employee,
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

fun com.mpcorp.identity.application.dto.contract.UpdateContractCommand.toDomainEntity(employee: com.mpcorp.identity.domain.entity.EmployeeEntity): com.mpcorp.identity.domain.entity.ContractEntity =
    _root_ide_package_.com.mpcorp.identity.domain.entity.ContractEntity(
        id = contract.id.toLongValue(),
        employee = employee,
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

fun com.mpcorp.identity.domain.entity.ContractEntity.toRefModel(): com.mpcorp.identity.application.references.ContractRefModel =
    _root_ide_package_.com.mpcorp.identity.application.references.ContractRefModel(
        id = _root_ide_package_.com.mpcorp.identity.application.references.IdentifierModel(requireNotNull(id).toString()),
    )

fun com.mpcorp.identity.domain.entity.ContractEntity.toResponseCommand(): com.mpcorp.identity.application.dto.contract.GetContractResponseCommand =
    _root_ide_package_.com.mpcorp.identity.application.dto.contract.GetContractResponseCommand(
        id = _root_ide_package_.com.mpcorp.identity.application.references.IdentifierModel(requireNotNull(id).toString()),
        employee = employee.toRefModel(),
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
