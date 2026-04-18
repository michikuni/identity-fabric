package com.mpcorp.identity.infrastructures.persistence.repository

import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.domain.entity.LeaveRequestEntity
import com.mpcorp.identity.domain.repository.LeaveRequestRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.LeaveRequestJpaEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.LeaveRequestJpaRepository
import com.mpcorp.identity.infrastructures.persistence.mapper.toDomainEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant

@Service
class LeaveRequestRepositoryImpl(
    private val leaveRequestJpaRepository: LeaveRequestJpaRepository,
    private val employeeJpaRepository: EmployeeJpaRepository,
) : LeaveRequestRepository {

    @Transactional
    override fun save(request: LeaveRequestEntity): LeaveRequestEntity {
        val employeeId = request.employee.id ?: throw EmployeeNotFoundException()
        val emp = employeeJpaRepository.findById(employeeId).orElseThrow(::EmployeeNotFoundException)
        val approverJpa = request.approver?.id?.let {
            employeeJpaRepository.findById(it).orElse(null)
        }
        val jpa = LeaveRequestJpaEntity(
            id = request.id,
            employee = emp,
            requestType = request.requestType,
            status = request.status,
            startDate = request.startDate,
            endDate = request.endDate,
            session = request.session,
            reason = request.reason,
            photoUrl = request.photoUrl,
            approver = approverJpa,
            approvedAt = request.approvedAt,
            rejectedReason = request.rejectedReason,
            createdAt = request.createdAt,
            updatedAt = request.updatedAt,
        )
        return leaveRequestJpaRepository.save(jpa).toDomainEntity()
    }

    override fun findById(id: Long): LeaveRequestEntity? =
        leaveRequestJpaRepository.findById(id).orElse(null)?.toDomainEntity()

    override fun findByEmployeeId(employeeId: Long): List<LeaveRequestEntity> =
        leaveRequestJpaRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId).map { it.toDomainEntity() }

    override fun findByApproverId(approverId: Long): List<LeaveRequestEntity> =
        leaveRequestJpaRepository.findByApproverIdOrderByCreatedAtDesc(approverId).map { it.toDomainEntity() }

    override fun findPendingByApproverId(approverId: Long): List<LeaveRequestEntity> =
        leaveRequestJpaRepository.findByApproverIdAndStatusOrderByCreatedAtDesc(approverId, "PENDING")
            .map { it.toDomainEntity() }

    @Transactional
    override fun updateStatus(id: Long, status: String, rejectedReason: String?): LeaveRequestEntity {
        val jpa = leaveRequestJpaRepository.findById(id).orElseThrow { Exception("Request not found") }
        jpa.status = status
        jpa.rejectedReason = rejectedReason
        if (status == "APPROVED") jpa.approvedAt = Timestamp.from(Instant.now())
        jpa.updatedAt = Timestamp.from(Instant.now())
        return leaveRequestJpaRepository.save(jpa).toDomainEntity()
    }
}
