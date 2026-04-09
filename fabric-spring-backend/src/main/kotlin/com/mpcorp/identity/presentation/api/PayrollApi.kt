package com.mpcorp.identity.presentation.api

import com.mpcorp.identity.presentation.request.payroll.CreatePayrollRequest
import com.mpcorp.identity.presentation.request.payroll.UpdatePayrollRequest
import com.mpcorp.identity.presentation.response.payroll.PayrollResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/payroll")
interface PayrollApi {
    @PostMapping
    fun create(httpRequest: HttpServletRequest, @RequestBody request: CreatePayrollRequest): PayrollResponse

    @PutMapping
    fun update(httpRequest: HttpServletRequest, @RequestBody request: UpdatePayrollRequest): PayrollResponse

    @GetMapping
    fun get(httpRequest: HttpServletRequest): PayrollResponse

    @DeleteMapping
    fun delete(httpRequest: HttpServletRequest): PayrollResponse
}

