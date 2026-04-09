package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.application.dto.profile.CreateProfileCommand
import com.mpcorp.identity.application.dto.profile.DeleteProfileCommand
import com.mpcorp.identity.application.dto.profile.GetProfileRequestCommand
import com.mpcorp.identity.application.dto.profile.UpdateProfileCommand
import com.mpcorp.identity.application.usecase.employee.ResolveCurrentEmployeeRefUseCase
import com.mpcorp.identity.application.usecase.profile.CreateProfileUseCase
import com.mpcorp.identity.application.usecase.profile.DeleteProfileUseCase
import com.mpcorp.identity.application.usecase.profile.GetProfileUseCase
import com.mpcorp.identity.application.usecase.profile.UpdateProfileUseCase
import com.mpcorp.identity.common.constant.ErrorCodes
import com.mpcorp.identity.common.constant.StatusMessage
import com.mpcorp.identity.common.exception.ProfileNotFoundException
import com.mpcorp.identity.presentation.api.ProfileApi
import com.mpcorp.identity.presentation.mapper.toDto
import com.mpcorp.identity.presentation.mapper.toModel
import com.mpcorp.identity.presentation.request.profile.CreateProfileRequest
import com.mpcorp.identity.presentation.request.profile.UpdateProfileRequest
import com.mpcorp.identity.presentation.response.profile.ProfileResponse
import com.mpcorp.identity.presentation.security.BearerAuthIdResolver
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.RestController

@RestController
class ProfileController(
    private val bearerAuthIdResolver: BearerAuthIdResolver,
    private val resolveCurrentEmployeeRefUseCase: ResolveCurrentEmployeeRefUseCase,
    private val createProfileUseCase: CreateProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
) : ProfileApi {
    override fun create(httpRequest: HttpServletRequest, request: CreateProfileRequest): ProfileResponse {
        val profile = createProfileUseCase.execute(
            command = CreateProfileCommand(
                employee = currentEmployeeRef(httpRequest),
                name = request.name,
                gender = request.gender,
                identityType = request.identityType,
                identityNumber = request.identityNumber,
                identityIssueDate = request.identityIssueDate,
                identityIssuePlace = request.identityIssuePlace,
                email = request.email,
                phone = request.phone,
                emergencyName = request.emergencyName,
                emergencyPhone = request.emergencyPhone,
                emergencyRelationship = request.emergencyRelationship,
                dateOfBirth = request.dateOfBirth,
                health = request.health,
                married = request.married,
                permanentResidence = request.permanentResidence,
                nowResidence = request.nowResidence,
                avatarUrl = request.avatarUrl,
                educationLevel = request.educationLevel,
                major = request.major,
                certificate = request.certificate,
                skillSet = request.skillSet,
                expYears = request.expYears,
            )
        )

        return ProfileResponse(
            status = ErrorCodes.CREATE_SUCCESS,
            message = StatusMessage.SUCCESS,
            data = profile.toDto(),
        )
    }

    override fun update(httpRequest: HttpServletRequest, request: UpdateProfileRequest): ProfileResponse {
        val profile = updateProfileUseCase.execute(
            command = UpdateProfileCommand(
                profile = request.profile.toModel(),
                employee = currentEmployeeRef(httpRequest),
                name = request.name,
                gender = request.gender,
                identityType = request.identityType,
                identityNumber = request.identityNumber,
                identityIssueDate = request.identityIssueDate,
                identityIssuePlace = request.identityIssuePlace,
                email = request.email,
                phone = request.phone,
                emergencyName = request.emergencyName,
                emergencyPhone = request.emergencyPhone,
                emergencyRelationship = request.emergencyRelationship,
                dateOfBirth = request.dateOfBirth,
                health = request.health,
                married = request.married,
                permanentResidence = request.permanentResidence,
                nowResidence = request.nowResidence,
                avatarUrl = request.avatarUrl,
                educationLevel = request.educationLevel,
                major = request.major,
                certificate = request.certificate,
                skillSet = request.skillSet,
                expYears = request.expYears,
            )
        )

        return ProfileResponse(
            status = ErrorCodes.UPDATE_SUCCESS,
            message = StatusMessage.SUCCESS,
            data = profile.toDto(),
        )
    }

    override fun get(httpRequest: HttpServletRequest): ProfileResponse {
        val profile = getProfileUseCase.execute(GetProfileRequestCommand(employee = currentEmployeeRef(httpRequest)))
            ?: throw ProfileNotFoundException()

        return ProfileResponse(
            status = ErrorCodes.SUCCESS,
            message = StatusMessage.SUCCESS,
            data = profile.toDto(),
        )
    }

    override fun delete(httpRequest: HttpServletRequest): ProfileResponse {
        deleteProfileUseCase.execute(DeleteProfileCommand(employee = currentEmployeeRef(httpRequest)))

        return ProfileResponse(
            status = ErrorCodes.DELETE_SUCCESS,
            message = StatusMessage.SUCCESS,
            data = null,
        )
    }

    private fun currentEmployeeRef(httpRequest: HttpServletRequest) =
        resolveCurrentEmployeeRefUseCase.execute(bearerAuthIdResolver.resolveAuthId(httpRequest))
}
