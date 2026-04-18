package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.application.usecase.company.GetCompanyUseCase
import com.mpcorp.identity.application.usecase.company.SaveCompanyCommand
import com.mpcorp.identity.application.usecase.company.SaveCompanyUseCase
import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.presentation.security.BearerAuthIdResolver
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/company")
class CompanyController(
    private val getCompanyUseCase: GetCompanyUseCase,
    private val saveCompanyUseCase: SaveCompanyUseCase,
    private val bearerAuthIdResolver: BearerAuthIdResolver,
) {
    data class CompanyBody(
        val id: Long? = null,
        val taxCode: String,
        val companyName: String,
        val legalRepName: String,
        val legalRepTitle: String,
        val legalRepIdNumber: String,
        val address: String,
        val phone: String,
        val email: String,
        val registeredAt: String,
    )

    @GetMapping
    fun get(): ApiResponse<Any> =
        ApiResponse(status = "200", message = "OK", data = getCompanyUseCase.execute())

    @PostMapping
    fun create(httpRequest: HttpServletRequest, @RequestBody body: CompanyBody): ApiResponse<Any> {
        val actorEmail = SecurityContextHolder.getContext().authentication?.name ?: "system"
        val result = saveCompanyUseCase.execute(
            SaveCompanyCommand(
                taxCode = body.taxCode, companyName = body.companyName,
                legalRepName = body.legalRepName, legalRepTitle = body.legalRepTitle,
                legalRepIdNumber = body.legalRepIdNumber, address = body.address,
                phone = body.phone, email = body.email,
                registeredAt = LocalDate.parse(body.registeredAt),
                actorEmail = actorEmail,
            )
        )
        return ApiResponse(status = "201", message = "Company registered", data = result)
    }

    @PutMapping
    fun update(httpRequest: HttpServletRequest, @RequestBody body: CompanyBody): ApiResponse<Any> {
        val actorEmail = SecurityContextHolder.getContext().authentication?.name ?: "system"
        val existing = getCompanyUseCase.execute()
        val result = saveCompanyUseCase.execute(
            SaveCompanyCommand(
                id = existing?.id ?: body.id,
                taxCode = body.taxCode, companyName = body.companyName,
                legalRepName = body.legalRepName, legalRepTitle = body.legalRepTitle,
                legalRepIdNumber = body.legalRepIdNumber, address = body.address,
                phone = body.phone, email = body.email,
                registeredAt = LocalDate.parse(body.registeredAt),
                actorEmail = actorEmail,
            )
        )
        return ApiResponse(status = "200", message = "Company updated", data = result)
    }
}
