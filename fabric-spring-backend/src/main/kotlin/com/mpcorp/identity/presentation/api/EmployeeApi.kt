package com.mpcorp.identity.presentation.api

import com.mpcorp.identity.presentation.request.employee.CreateEmployeeRequest
import com.mpcorp.identity.presentation.request.employee.UpdateEmployeeRequest
import com.mpcorp.identity.presentation.response.employee.EmployeeResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/employee")
interface EmployeeApi {
    @PostMapping
    fun create(httpRequest: HttpServletRequest, @RequestBody request: CreateEmployeeRequest): EmployeeResponse

    @GetMapping
    fun get(httpRequest: HttpServletRequest): EmployeeResponse

    @PutMapping
    fun update(httpRequest: HttpServletRequest, @RequestBody request: UpdateEmployeeRequest): EmployeeResponse

    @DeleteMapping
    fun delete(httpRequest: HttpServletRequest): EmployeeResponse
}

