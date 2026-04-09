package com.mpcorp.identity.presentation.api

import com.mpcorp.identity.presentation.request.profile.CreateProfileRequest
import com.mpcorp.identity.presentation.request.profile.UpdateProfileRequest
import com.mpcorp.identity.presentation.response.profile.ProfileResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/profile")
interface ProfileApi {
    @PostMapping
    fun create(httpRequest: HttpServletRequest, @RequestBody request: CreateProfileRequest): ProfileResponse

    @PutMapping
    fun update(httpRequest: HttpServletRequest, @RequestBody request: UpdateProfileRequest): ProfileResponse

    @GetMapping
    fun get(httpRequest: HttpServletRequest): ProfileResponse

    @DeleteMapping
    fun delete(httpRequest: HttpServletRequest): ProfileResponse
}

