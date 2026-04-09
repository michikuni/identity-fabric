package com.mpcorp.identity.domain.repository

import com.mpcorp.identity.domain.entity.ContractEntity

interface ContractRepository {
    fun createContract(contract: ContractEntity): ContractEntity
    fun findContractByEmployeeId(userId: Long): ContractEntity?
    fun updateContract(contract: ContractEntity): ContractEntity
    fun deleteContractByEmployeeId(userId: Long)
}