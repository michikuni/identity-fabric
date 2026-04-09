package com.mpcorp.identity.application.mapper

import com.mpcorp.identity.application.dto.profile.GetProfileResponseCommand
import com.mpcorp.identity.application.mapper.toRefModel
import com.mpcorp.identity.application.references.IdentifierModel
import com.mpcorp.identity.domain.entity.ProfileEntity

fun com.mpcorp.identity.domain.entity.ProfileEntity.toGetProfileResponseCommand(): com.mpcorp.identity.application.dto.profile.GetProfileResponseCommand =
    _root_ide_package_.com.mpcorp.identity.application.dto.profile.GetProfileResponseCommand(
        id = _root_ide_package_.com.mpcorp.identity.application.references.IdentifierModel(requireNotNull(id) { "profile id missing" }.toString()),
        employee = employee.toRefModel(),
        name = name,
        gender = gender,
        identityType = identityType,
        identityNumber = identityNumber,
        identityIssueDate = identityIssueDate,
        identityIssuePlace = identityIssuePlace,
        email = email,
        phone = phone,
        emergencyName = emergencyName,
        emergencyPhone = emergencyPhone,
        emergencyRelationship = emergencyRelationship,
        dateOfBirth = dateOfBirth,
        health = health,
        married = married,
        permanentResidence = permanentResidence,
        nowResidence = nowResidence,
        avatarUrl = avatarUrl,
        educationLevel = educationLevel,
        major = major,
        certificate = certificate,
        skillSet = skillSet,
        expYears = expYears,
    )
