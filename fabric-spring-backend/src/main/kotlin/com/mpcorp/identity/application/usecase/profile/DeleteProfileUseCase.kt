package com.mpcorp.identity.application.usecase.profile

import com.mpcorp.identity.application.dto.profile.DeleteProfileCommand
import com.mpcorp.identity.application.support.requireEmployeeId
import com.mpcorp.identity.domain.repository.ProfileRepository
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import org.springframework.stereotype.Service

@Service
class DeleteProfileUseCase(
    private val profileRepository: ProfileRepository,
    private val fabricBridge: FabricLedgerBridge,
) {
    fun execute(command: DeleteProfileCommand) {
        val employeeId = command.employee.requireEmployeeId()
        profileRepository.deleteProfileById(employeeId)
        fabricBridge.deleteProfileRecord(employeeId.toString())
    }
}