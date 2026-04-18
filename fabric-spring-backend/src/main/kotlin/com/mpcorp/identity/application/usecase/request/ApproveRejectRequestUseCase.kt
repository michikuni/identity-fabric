package com.mpcorp.identity.application.usecase.request

import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.domain.entity.LeaveRequestEntity
import com.mpcorp.identity.domain.repository.EmployeeRepository
import com.mpcorp.identity.domain.repository.LeaveRequestRepository
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ApproveRejectRequestUseCase(
    private val leaveRequestRepository: LeaveRequestRepository,
    private val employeeRepository: EmployeeRepository,
    private val ledgerBridge: FabricLedgerBridge,
) {
    fun approve(authId: UUID, requestId: Long): LeaveRequestEntity {
        val manager = employeeRepository.findEmployeeByAuthId(authId) ?: throw EmployeeNotFoundException()
        val saved = leaveRequestRepository.updateStatus(requestId, "APPROVED")
        ledgerBridge.logRequest(
            saved.employee.id.toString(), saved.requestType, "APPROVED", manager.auth.email
        )
        return saved
    }

    fun reject(authId: UUID, requestId: Long, reason: String): LeaveRequestEntity {
        val manager = employeeRepository.findEmployeeByAuthId(authId) ?: throw EmployeeNotFoundException()
        val saved = leaveRequestRepository.updateStatus(requestId, "REJECTED", reason)
        ledgerBridge.logRequest(
            saved.employee.id.toString(), saved.requestType, "REJECTED", manager.auth.email
        )
        return saved
    }
}
