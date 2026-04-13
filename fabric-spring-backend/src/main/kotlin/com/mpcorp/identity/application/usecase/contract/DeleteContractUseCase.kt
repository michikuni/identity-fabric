package com.mpcorp.identity.application.usecase.contract
 
import com.mpcorp.identity.application.dto.contract.DeleteContractCommand
import com.mpcorp.identity.application.support.requireEmployeeId
import com.mpcorp.identity.domain.repository.ContractRepository
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import org.springframework.stereotype.Service
 
@Service
class DeleteContractUseCase(
    private val contractRepository: ContractRepository,
    private val fabricBridge: FabricLedgerBridge,
) {
    fun execute(command: DeleteContractCommand) {
        val employeeId = command.employee.requireEmployeeId()
        contractRepository.deleteContractByEmployeeId(employeeId)
        fabricBridge.deleteContractRecord(employeeId.toString())
    }
}