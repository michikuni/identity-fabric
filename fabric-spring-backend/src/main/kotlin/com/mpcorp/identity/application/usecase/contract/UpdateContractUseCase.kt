package com.mpcorp.identity.application.usecase.contract
 
import com.mpcorp.identity.application.dto.contract.GetContractResponseCommand
import com.mpcorp.identity.application.dto.contract.UpdateContractCommand
import com.mpcorp.identity.application.mapper.toDomainEntity
import com.mpcorp.identity.application.mapper.toResponseCommand
import com.mpcorp.identity.application.support.resolveEmployee
import com.mpcorp.identity.domain.repository.ContractRepository
import com.mpcorp.identity.domain.repository.EmployeeRepository
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import org.springframework.stereotype.Service
 
@Service
class UpdateContractUseCase(
    private val contractRepository: ContractRepository,
    private val employeeRepository: EmployeeRepository,
    private val fabricBridge: FabricLedgerBridge,
) {
    fun execute(command: UpdateContractCommand): GetContractResponseCommand {
        val employee = command.employee.resolveEmployee(employeeRepository)
        val saved    = contractRepository.updateContract(command.toDomainEntity(employee))
        fabricBridge.upsertContractRecord(saved, action = "UPDATE")
        return saved.toResponseCommand()
    }
}