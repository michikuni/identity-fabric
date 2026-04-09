package com.mpcorp.identity.application.mapper

import com.mpcorp.identity.application.dto.employee.GetEmployeeResponseCommand
import com.mpcorp.identity.application.mapper.toAuthRefModel
import com.mpcorp.identity.application.mapper.toRefModel
import com.mpcorp.identity.application.references.AuthRefModel
import com.mpcorp.identity.application.references.ContractRefModel
import com.mpcorp.identity.application.references.EmployeeRefModel
import com.mpcorp.identity.application.references.IdentifierModel
import com.mpcorp.identity.application.references.PayrollRefModel
import com.mpcorp.identity.application.references.ProfileRefModel
import com.mpcorp.identity.domain.entity.AuthEntity
import com.mpcorp.identity.domain.entity.EmployeeEntity

fun com.mpcorp.identity.domain.entity.AuthEntity.toAuthRefModel(): com.mpcorp.identity.application.references.AuthRefModel =
    _root_ide_package_.com.mpcorp.identity.application.references.AuthRefModel(
        id = id,
        email = email,
        phone = phone,
        password = password,
        role = role,
    )

fun com.mpcorp.identity.domain.entity.EmployeeEntity.toRefModel(): com.mpcorp.identity.application.references.EmployeeRefModel =
    _root_ide_package_.com.mpcorp.identity.application.references.EmployeeRefModel(
        id = id?.let { _root_ide_package_.com.mpcorp.identity.application.references.IdentifierModel(it.toString()) },
        authId = auth.id?.let { _root_ide_package_.com.mpcorp.identity.application.references.IdentifierModel(it.toString()) },
    )

fun com.mpcorp.identity.domain.entity.EmployeeEntity.toGetEmployeeResponseCommand(): com.mpcorp.identity.application.dto.employee.GetEmployeeResponseCommand =
    _root_ide_package_.com.mpcorp.identity.application.dto.employee.GetEmployeeResponseCommand(
        id = requireNotNull(id) { "employee id missing" },
        auth = auth.toAuthRefModel(),
        department = department,
        position = position,
        status = status,
        workingType = workingType,
        isActive = isActive,
        manager = manager?.toRefModel(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        note = note,
        profile = profile?.id?.let {
            _root_ide_package_.com.mpcorp.identity.application.references.ProfileRefModel(
                _root_ide_package_.com.mpcorp.identity.application.references.IdentifierModel(it.toString())
            )
        },
        contract = contract?.id?.let {
            _root_ide_package_.com.mpcorp.identity.application.references.ContractRefModel(
                _root_ide_package_.com.mpcorp.identity.application.references.IdentifierModel(it.toString())
            )
        },
        payroll = payroll?.id?.let {
            _root_ide_package_.com.mpcorp.identity.application.references.PayrollRefModel(
                _root_ide_package_.com.mpcorp.identity.application.references.IdentifierModel(it.toString())
            )
        },
    )
