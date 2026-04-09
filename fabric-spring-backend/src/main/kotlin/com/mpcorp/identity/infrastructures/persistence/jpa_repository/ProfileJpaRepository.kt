package com.mpcorp.identity.infrastructures.persistence.jpa_repository

import com.mpcorp.identity.infrastructures.persistence.jpa_entity.PayrollJpaEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.ProfileJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ProfileJpaRepository : JpaRepository<ProfileJpaEntity, Long>{
    @Query("SELECT * FROM profile WHERE employee_id = :id", nativeQuery = true)
    fun findProfileByEmployeeId(id: Long?): ProfileJpaEntity?

    @Modifying
    @Query("DELETE FROM profile WHERE employee_id = :id", nativeQuery = true)
    fun deleteProfileByEmployeeId(id: Long)
}