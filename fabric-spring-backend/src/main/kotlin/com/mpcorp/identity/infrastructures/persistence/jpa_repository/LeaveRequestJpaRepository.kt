package com.mpcorp.identity.infrastructures.persistence.jpa_repository

import com.mpcorp.identity.infrastructures.persistence.jpa_entity.LeaveRequestJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LeaveRequestJpaRepository : JpaRepository<LeaveRequestJpaEntity, Long> {
    fun findByEmployeeIdOrderByCreatedAtDesc(employeeId: Long): List<LeaveRequestJpaEntity>
    fun findByApproverIdOrderByCreatedAtDesc(approverId: Long): List<LeaveRequestJpaEntity>
    fun findByApproverIdAndStatusOrderByCreatedAtDesc(approverId: Long, status: String): List<LeaveRequestJpaEntity>
}
