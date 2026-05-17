package com.mpcorp.identity.infrastructures.persistence.jpa_repository

import com.mpcorp.identity.infrastructures.persistence.jpa_entity.EmployeeJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface EmployeeJpaRepository : JpaRepository<EmployeeJpaEntity, Long>{
    @Query("SELECT * FROM employee WHERE auth_id = :id", nativeQuery = true)
    fun findEmployeeByAuthId(id: UUID): EmployeeJpaEntity?

    @Query("SELECT e.* FROM employee e JOIN auth a ON e.auth_id = a.id WHERE a.email = :email", nativeQuery = true)
    fun findEmployeeByAuthEmail(email: String): EmployeeJpaEntity?

    @Modifying
    @Query("DELETE FROM employee WHERE auth_id = :id", nativeQuery = true)
    fun deleteEmployeeByAuthId(id: UUID)
}