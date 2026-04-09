package com.mpcorp.identity.application.usecase.employee

import com.mpcorp.identity.domain.repository.EmployeeRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DeleteCurrentEmployeeUseCase(
    private val employeeRepository: EmployeeRepository,
) {
    fun execute(authId: UUID) {
        employeeRepository.deleteEmployeeByAuthId(authId)
    }
}

