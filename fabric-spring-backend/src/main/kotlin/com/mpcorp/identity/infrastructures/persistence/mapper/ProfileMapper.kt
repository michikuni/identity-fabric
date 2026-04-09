package com.mpcorp.identity.infrastructures.persistence.mapper

import com.mpcorp.identity.domain.entity.ProfileEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.ProfileJpaEntity

fun ProfileJpaEntity.toDomainEntity(): ProfileEntity {
    return ProfileEntity(
        id = id,
        employee = employee.toDomainEntity(),
        name = name,
        email = email,
        phone = phone,
        major = major,
        gender = gender,
        health = health,
        expYears = expYears,
        skillSet = skillSet,
        married = married,
        avatarUrl = avatarUrl,
        certificate = certificate,
        dateOfBirth = dateOfBirth,
        identityType = identityType,
        nowResidence = nowResidence,
        emergencyName = emergencyName,
        educationLevel = educationLevel,
        emergencyPhone = emergencyPhone,
        identityNumber = identityNumber,
        identityIssueDate = identityIssueDate,
        identityIssuePlace = identityIssuePlace,
        permanentResidence = permanentResidence,
        emergencyRelationship = emergencyRelationship,
    )
}

fun ProfileEntity.toPersistentEntity(): ProfileJpaEntity {
    return ProfileJpaEntity(
        id = id,
        employee = employee.toPersistentEntity(),
        name = name,
        email = email,
        phone = phone,
        major = major,
        gender = gender,
        health = health,
        expYears = expYears,
        skillSet = skillSet,
        married = married,
        avatarUrl = avatarUrl,
        certificate = certificate,
        dateOfBirth = dateOfBirth,
        identityType = identityType,
        nowResidence = nowResidence,
        emergencyName = emergencyName,
        educationLevel = educationLevel,
        emergencyPhone = emergencyPhone,
        identityNumber = identityNumber,
        identityIssueDate = identityIssueDate,
        identityIssuePlace = identityIssuePlace,
        permanentResidence = permanentResidence,
        emergencyRelationship = emergencyRelationship,
    )
}
