package com.mpcorp.identity.domain.repository

import com.mpcorp.identity.domain.entity.LeaveRequestEntity

interface LeaveRequestRepository {
    fun save(request: LeaveRequestEntity): LeaveRequestEntity
    fun findById(id: Long): LeaveRequestEntity?
    fun findByEmployeeId(employeeId: Long): List<LeaveRequestEntity>
    fun findByApproverId(approverId: Long): List<LeaveRequestEntity>
    fun findPendingByApproverId(approverId: Long): List<LeaveRequestEntity>
    fun updateStatus(id: Long, status: String, rejectedReason: String? = null): LeaveRequestEntity
}
