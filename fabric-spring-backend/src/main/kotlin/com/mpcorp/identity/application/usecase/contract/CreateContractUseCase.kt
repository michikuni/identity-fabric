package com.mpcorp.identity.application.usecase.contract
 
import com.mpcorp.identity.application.dto.contract.CreateContractCommand
import com.mpcorp.identity.application.dto.contract.GetContractResponseCommand
import com.mpcorp.identity.application.mapper.toDomainEntity
import com.mpcorp.identity.application.mapper.toResponseCommand
import com.mpcorp.identity.application.support.resolveEmployee
import com.mpcorp.identity.domain.repository.ContractRepository
import com.mpcorp.identity.domain.repository.EmployeeRepository
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import org.springframework.stereotype.Service
 
@Service
class CreateContractUseCase(
    private val contractRepository: ContractRepository,
    private val employeeRepository: EmployeeRepository,
    private val fabricBridge: FabricLedgerBridge,
) {
    fun execute(command: CreateContractCommand): GetContractResponseCommand {
        val employee        = command.employee.resolveEmployee(employeeRepository)
        val contractCreated = contractRepository.createContract(command.toDomainEntity(employee))
        fabricBridge.upsertContractRecord(contractCreated, action = "CREATE")
        return contractCreated.toResponseCommand()
    }
}