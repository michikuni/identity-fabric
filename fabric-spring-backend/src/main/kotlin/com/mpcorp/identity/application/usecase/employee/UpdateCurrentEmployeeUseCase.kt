package com.mpcorp.identity.application.usecase.employee

import com.mpcorp.identity.application.dto.employee.GetEmployeeResponseCommand
import com.mpcorp.identity.application.dto.employee.UpdateEmployeeCommand
import com.mpcorp.identity.application.mapper.toGetEmployeeResponseCommand
import com.mpcorp.identity.application.support.resolveEmployee
import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.domain.entity.EmployeeEntity
import com.mpcorp.identity.domain.repository.EmployeeRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UpdateCurrentEmployeeUseCase(
    private val employeeRepository: EmployeeRepository,
) {
    fun execute(authId: UUID, command: UpdateEmployeeCommand): GetEmployeeResponseCommand {
        val currentEmployee = employeeRepository.findEmployeeByAuthId(authId) ?: throw EmployeeNotFoundException()
        val manager = command.manager?.resolveEmployee(employeeRepository)
        val updated = EmployeeEntity(
            id = currentEmployee.id,
            auth = currentEmployee.auth,
            department = command.department,
            position = command.position,
            status = command.status,
            workingType = command.workingType,
            isActive = command.isActive,
            manager = manager,
            createdAt = currentEmployee.createdAt,
            updatedAt = command.updatedAt,
            createdBy = currentEmployee.createdBy,
            note = command.note,
        )
        return employeeRepository.updateEmployeeByAuthId(updated).toGetEmployeeResponseCommand()
    }
}
