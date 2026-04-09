package com.mpcorp.identity.infrastructures.persistence.repository

import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.common.exception.ProfileNotFoundException
import com.mpcorp.identity.domain.entity.ProfileEntity
import com.mpcorp.identity.domain.repository.ProfileRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.ProfileJpaEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.ProfileJpaRepository
import com.mpcorp.identity.infrastructures.persistence.mapper.toDomainEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProfileRepositoryImpl(
    private val profileJpaRepository: ProfileJpaRepository,
    private val employeeJpaRepository: EmployeeJpaRepository,
) : ProfileRepository {
    @Transactional
    override fun createProfileById(
        profile: ProfileEntity
    ): ProfileEntity {
        val employeeId = profile.employee.id ?: throw EmployeeNotFoundException()
        val profileJpaData = ProfileJpaEntity(
            employee = employeeJpaRepository.findById(employeeId).orElseThrow(::EmployeeNotFoundException),
            name = profile.name,
            gender = profile.gender,
            identityType = profile.identityType,
            identityNumber = profile.identityNumber,
            identityIssueDate = profile.identityIssueDate,
            identityIssuePlace = profile.identityIssuePlace,
            email = profile.email,
            phone = profile.phone,
            emergencyName = profile.emergencyName,
            emergencyPhone = profile.emergencyPhone,
            emergencyRelationship = profile.emergencyRelationship,
            dateOfBirth = profile.dateOfBirth,
            health = profile.health,
            married = profile.married,
            permanentResidence = profile.permanentResidence,
            nowResidence = profile.nowResidence,
            avatarUrl = profile.avatarUrl,
            educationLevel = profile.educationLevel,
            major = profile.major,
            certificate = profile.certificate,
            skillSet = profile.skillSet,
            expYears = profile.expYears,
        )
        val dataSaveProfile = profileJpaRepository.save(profileJpaData)
        return dataSaveProfile.toDomainEntity()
    }

    override fun findProfileById(userId: Long): ProfileEntity {
        val profileJpaData = profileJpaRepository.findProfileByEmployeeId(userId) ?: throw ProfileNotFoundException()
        return profileJpaData.toDomainEntity()
    }

    override fun updateProfileById(
        profile: ProfileEntity
    ): ProfileEntity {
        val existingProfile = profileJpaRepository.findProfileByEmployeeId(profile.employee.id) ?: throw ProfileNotFoundException()
        existingProfile.apply {
            val employeeId = profile.employee.id ?: throw EmployeeNotFoundException()
            employee = employeeJpaRepository.findById(employeeId).orElseThrow(::EmployeeNotFoundException)
            name = profile.name
            gender = profile.gender
            identityType = profile.identityType
            identityNumber = profile.identityNumber
            identityIssueDate = profile.identityIssueDate
            identityIssuePlace = profile.identityIssuePlace
            email = profile.email
            phone = profile.phone
            emergencyName = profile.emergencyName
            emergencyPhone = profile.emergencyPhone
            emergencyRelationship = profile.emergencyRelationship
            dateOfBirth = profile.dateOfBirth
            health = profile.health
            married = profile.married
            permanentResidence = profile.permanentResidence
            nowResidence = profile.nowResidence
            avatarUrl = profile.avatarUrl
            educationLevel = profile.educationLevel
            major = profile.major
            certificate = profile.certificate
            skillSet = profile.skillSet
            expYears = profile.expYears
        }

        return profileJpaRepository.save(existingProfile).toDomainEntity()
    }

    @Transactional
    override fun deleteProfileById(userId: Long) {
        profileJpaRepository.deleteProfileByEmployeeId(userId)
    }
}
