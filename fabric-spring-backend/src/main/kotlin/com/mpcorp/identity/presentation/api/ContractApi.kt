package com.mpcorp.identity.presentation.api

import com.mpcorp.identity.presentation.request.contract.CreateContractRequest
import com.mpcorp.identity.presentation.request.contract.UpdateContractRequest
import com.mpcorp.identity.presentation.response.contract.ContractResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/v1/contracts")
interface ContractApi {
    @PostMapping
    fun createContract(httpRequest: HttpServletRequest, @RequestBody request: CreateContractRequest): ContractResponse

    @PutMapping
    fun updateContract(httpRequest: HttpServletRequest, @RequestBody request: UpdateContractRequest): ContractResponse

    @GetMapping
    fun getContract(httpRequest: HttpServletRequest): ContractResponse

    @DeleteMapping
    fun deleteContract(httpRequest: HttpServletRequest): ContractResponse
}

