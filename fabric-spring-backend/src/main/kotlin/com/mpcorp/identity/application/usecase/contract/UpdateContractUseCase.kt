package com.mpcorp.identity.application.usecase.contract

import com.mpcorp.identity.application.dto.contract.GetContractResponseCommand
import com.mpcorp.identity.application.dto.contract.UpdateContractCommand
import com.mpcorp.identity.application.mapper.toDomainEntity
import com.mpcorp.identity.application.mapper.toResponseCommand
import com.mpcorp.identity.application.support.resolveEmployee
import com.mpcorp.identity.domain.repository.ContractRepository
import com.mpcorp.identity.domain.repository.EmployeeRepository
import org.springframework.stereotype.Service

@Service
class UpdateContractUseCase(
    private val contractRepository: ContractRepository,
    private val employeeRepository: EmployeeRepository,
) {
    fun execute(command: UpdateContractCommand): GetContractResponseCommand {
        val employee = command.employee.resolveEmployee(employeeRepository)
        return contractRepository.updateContract(command.toDomainEntity(employee)).toResponseCommand()
    }
}
