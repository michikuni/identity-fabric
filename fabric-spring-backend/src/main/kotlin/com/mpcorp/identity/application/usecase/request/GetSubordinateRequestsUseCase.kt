package com.mpcorp.identity.application.usecase.request

import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.domain.entity.LeaveRequestEntity
import com.mpcorp.identity.domain.repository.EmployeeRepository
import com.mpcorp.identity.domain.repository.LeaveRequestRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GetSubordinateRequestsUseCase(
    private val leaveRequestRepository: LeaveRequestRepository,
    private val employeeRepository: EmployeeRepository,
) {
    fun execute(authId: UUID, pendingOnly: Boolean = false): List<LeaveRequestEntity> {
        val manager = employeeRepository.findEmployeeByAuthId(authId) ?: throw EmployeeNotFoundException()
        return if (pendingOnly) {
            leaveRequestRepository.findPendingByApproverId(manager.id!!)
        } else {
            leaveRequestRepository.findByApproverId(manager.id!!)
        }
    }
}
