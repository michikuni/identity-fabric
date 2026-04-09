package com.mpcorp.identity.application.usecase.profile

import com.mpcorp.identity.application.dto.profile.DeleteProfileCommand
import com.mpcorp.identity.application.support.requireEmployeeId
import com.mpcorp.identity.domain.repository.ProfileRepository
import org.springframework.stereotype.Service

@Service
class DeleteProfileUseCase(
    private val profileRepository: ProfileRepository,
) {
    fun execute(command: DeleteProfileCommand) {
        profileRepository.deleteProfileById(command.employee.requireEmployeeId())
    }
}
