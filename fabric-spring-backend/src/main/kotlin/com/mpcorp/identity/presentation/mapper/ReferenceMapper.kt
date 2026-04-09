package com.mpcorp.identity.presentation.mapper

import com.mpcorp.identity.application.references.ContractRefModel
import com.mpcorp.identity.application.references.EmployeeRefModel
import com.mpcorp.identity.application.references.IdentifierModel
import com.mpcorp.identity.application.references.PayrollRefModel
import com.mpcorp.identity.application.references.ProfileRefModel
import com.mpcorp.identity.presentation.model.ContractRefPayload
import com.mpcorp.identity.presentation.model.EmployeeRefPayload
import com.mpcorp.identity.presentation.model.IdentifierPayload
import com.mpcorp.identity.presentation.model.PayrollRefPayload
import com.mpcorp.identity.presentation.model.ProfileRefPayload

fun IdentifierPayload.toModel(): IdentifierModel = IdentifierModel(value = value)

fun EmployeeRefPayload.toModel(): EmployeeRefModel = EmployeeRefModel(
    id = id?.toModel(),
    authId = authId?.toModel(),
)

fun ProfileRefPayload.toModel(): ProfileRefModel = ProfileRefModel(id = id.toModel())

fun ContractRefPayload.toModel(): ContractRefModel = ContractRefModel(id = id.toModel())

fun PayrollRefPayload.toModel(): PayrollRefModel = PayrollRefModel(id = id.toModel())

fun String.toIdentifierPayload(): IdentifierPayload = IdentifierPayload(value = this)
