package com.mpcorp.identity.application.usecase.profile

import com.mpcorp.identity.application.dto.profile.GetProfileRequestCommand
import com.mpcorp.identity.application.dto.profile.GetProfileResponseCommand
import com.mpcorp.identity.application.mapper.toGetProfileResponseCommand
import com.mpcorp.identity.application.support.requireEmployeeId
import com.mpcorp.identity.domain.repository.ProfileRepository
import org.springframework.stereotype.Service

@Service
class GetProfileUseCase(
    private val profileRepository: ProfileRepository,
) {
    fun execute(command: GetProfileRequestCommand): GetProfileResponseCommand? {
        val profile = profileRepository.findProfileById(command.employee.requireEmployeeId())
            ?: return null
        return profile.toGetProfileResponseCommand()
    }
}
