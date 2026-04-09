package com.mpcorp.identity.infrastructures.persistence.jpa_repository

import com.mpcorp.identity.infrastructures.persistence.jpa_entity.PayrollJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface PayrollJpaRepository : JpaRepository<PayrollJpaEntity, Long>{
    @Query("SELECT * FROM payroll WHERE employee_id = :id", nativeQuery = true)
    fun findPayrollByEmployeeId(id: Long?): PayrollJpaEntity?

    @Modifying
    @Query("DELETE FROM payroll WHERE employee_id = :id", nativeQuery = true)
    fun deletePayrollByEmployeeId(id: Long)
}