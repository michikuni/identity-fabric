package com.mpcorp.identity.application.usecase.employee

import com.mpcorp.identity.application.references.EmployeeRefModel
import com.mpcorp.identity.application.references.IdentifierModel
import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.domain.repository.EmployeeRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ResolveCurrentEmployeeRefUseCase(
    private val employeeRepository: EmployeeRepository,
) {
    fun execute(authId: UUID): EmployeeRefModel {
        val employee = employeeRepository.findEmployeeByAuthId(authId) ?: throw EmployeeNotFoundException()
        val employeeId = employee.id?.toString() ?: throw EmployeeNotFoundException()
        return EmployeeRefModel(
            id = IdentifierModel(employeeId),
            authId = IdentifierModel(authId.toString()),
        )
    }
}
