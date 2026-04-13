package com.mpcorp.identity.application.usecase.profile
 
import com.mpcorp.identity.application.dto.profile.GetProfileResponseCommand
import com.mpcorp.identity.application.dto.profile.UpdateProfileCommand
import com.mpcorp.identity.application.mapper.toGetProfileResponseCommand
import com.mpcorp.identity.application.support.resolveEmployee
import com.mpcorp.identity.application.support.toLongValue
import com.mpcorp.identity.domain.entity.ProfileEntity
import com.mpcorp.identity.domain.repository.EmployeeRepository
import com.mpcorp.identity.domain.repository.ProfileRepository
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import org.springframework.stereotype.Service
 
@Service
class UpdateProfileUseCase(
    private val profileRepository: ProfileRepository,
    private val employeeRepository: EmployeeRepository,
    private val fabricBridge: FabricLedgerBridge,
) {
    fun execute(command: UpdateProfileCommand): GetProfileResponseCommand {
        val employee = command.employee.resolveEmployee(employeeRepository)
        val profileEntity = ProfileEntity(
            expYears               = command.expYears,
            id                     = command.profile.id.toLongValue(),
            employee               = employee,
            name                   = command.name,
            gender                 = command.gender,
            identityType           = command.identityType,
            identityNumber         = command.identityNumber,
            identityIssueDate      = command.identityIssueDate,
            identityIssuePlace     = command.identityIssuePlace,
            email                  = command.email,
            phone                  = command.phone,
            emergencyName          = command.emergencyName,
            emergencyPhone         = command.emergencyPhone,
            emergencyRelationship  = command.emergencyRelationship,
            dateOfBirth            = command.dateOfBirth,
            health                 = command.health,
            married                = command.married,
            permanentResidence     = command.permanentResidence,
            nowResidence           = command.nowResidence,
            avatarUrl              = command.avatarUrl,
            educationLevel         = command.educationLevel,
            major                  = command.major,
            certificate            = command.certificate,
            skillSet               = command.skillSet,
        )
        val saved = profileRepository.updateProfileById(profileEntity)
        fabricBridge.upsertProfileRecord(saved, action = "UPDATE")
        return saved.toGetProfileResponseCommand()
    }
}