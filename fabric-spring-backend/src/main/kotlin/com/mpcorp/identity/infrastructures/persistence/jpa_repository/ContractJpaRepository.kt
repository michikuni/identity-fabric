package com.mpcorp.identity.infrastructures.persistence.jpa_repository

import com.mpcorp.identity.domain.entity.ContractEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.ContractJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ContractJpaRepository : JpaRepository<ContractJpaEntity, Long>{
    @Query("SELECT * FROM contract WHERE employee_id = :employeeId", nativeQuery = true)
    fun findContractByEmployeeId(employeeId: Long): ContractJpaEntity?

    @Modifying
    @Query("DELETE FROM contract WHERE employee_id = :employeeId", nativeQuery = true)
    fun deleteContractByEmployeeId(employeeId: Long)
}