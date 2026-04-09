package com.mpcorp.identity.application.usecase.contract

import com.mpcorp.identity.application.dto.contract.GetContractRequestCommand
import com.mpcorp.identity.application.dto.contract.GetContractResponseCommand
import com.mpcorp.identity.application.mapper.toResponseCommand
import com.mpcorp.identity.application.support.requireEmployeeId
import com.mpcorp.identity.domain.repository.ContractRepository
import org.springframework.stereotype.Service

@Service
class GetContractUseCase(
    private val contractRepository: ContractRepository,
) {
    fun execute(command: GetContractRequestCommand): GetContractResponseCommand? {
        return contractRepository.findContractByEmployeeId(command.employee.requireEmployeeId())?.toResponseCommand()
    }
}
