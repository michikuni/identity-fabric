package com.mpcorp.identity.application.usecase.employee

import com.mpcorp.identity.application.dto.employee.CreateEmployeeCommand
import com.mpcorp.identity.application.dto.employee.GetEmployeeResponseCommand
import com.mpcorp.identity.application.mapper.toGetEmployeeResponseCommand
import com.mpcorp.identity.application.support.resolveEmployee
import com.mpcorp.identity.common.exception.EmployeeAlreadyExistingException
import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.domain.entity.EmployeeEntity
import com.mpcorp.identity.domain.repository.AuthRepository
import com.mpcorp.identity.domain.repository.EmployeeRepository
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant

@Service
class CreateCurrentEmployeeUseCase(
    private val employeeRepository: EmployeeRepository,
    private val authRepository: AuthRepository,
) {
    fun execute(username: String, command: CreateEmployeeCommand): GetEmployeeResponseCommand {
        val auth = authRepository.findByUsername(username) ?: throw EmployeeNotFoundException()
        if (auth.id != null && employeeRepository.findEmployeeByAuthId(auth.id!!) != null) {
            throw EmployeeAlreadyExistingException()
        }
        val now = Timestamp.from(Instant.now())
        val manager = command.manager?.resolveEmployee(employeeRepository)
        val employee = EmployeeEntity(
            auth = auth,
            department = command.department,
            position = command.position,
            status = command.status,
            workingType = command.workingType,
            isActive = command.isActive,
            manager = manager,
            createdAt = now,
            updatedAt = now,
            createdBy = command.createdBy,
            note = command.note,
        )
        return employeeRepository.createEmployee(employee).toGetEmployeeResponseCommand()
    }
}
