package com.mpcorp.identity.application.support

import com.mpcorp.identity.application.references.EmployeeRefModel
import com.mpcorp.identity.application.references.IdentifierModel
import com.mpcorp.identity.application.support.toLongValue
import com.mpcorp.identity.application.support.toUuidValue
import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.domain.entity.EmployeeEntity
import com.mpcorp.identity.domain.repository.EmployeeRepository
import java.util.UUID

fun com.mpcorp.identity.application.references.IdentifierModel.toLongValue(): Long = value.toLong()

fun com.mpcorp.identity.application.references.IdentifierModel.toUuidValue(): UUID = UUID.fromString(value)

fun com.mpcorp.identity.application.references.EmployeeRefModel.requireEmployeeId(): Long =
    id?.toLongValue() ?: throw _root_ide_package_.com.mpcorp.identity.common.exception.EmployeeNotFoundException()

fun com.mpcorp.identity.application.references.EmployeeRefModel.resolveEmployee(employeeRepository: com.mpcorp.identity.domain.repository.EmployeeRepository): com.mpcorp.identity.domain.entity.EmployeeEntity {
    val employee = when {
        id != null -> employeeRepository.findEmployeeById(id.toLongValue())
        authId != null -> employeeRepository.findEmployeeByAuthId(authId.toUuidValue())
        else -> null
    }

    return employee ?: throw _root_ide_package_.com.mpcorp.identity.common.exception.EmployeeNotFoundException()
}
