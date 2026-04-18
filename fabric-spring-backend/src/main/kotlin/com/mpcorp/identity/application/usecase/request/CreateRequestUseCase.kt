package com.mpcorp.identity.application.usecase.request

import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.domain.entity.LeaveRequestEntity
import com.mpcorp.identity.domain.repository.EmployeeRepository
import com.mpcorp.identity.domain.repository.LeaveRequestRepository
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateRequestCommand(
    val requestType: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val session: String? = null,
    val reason: String,
    val photoUrl: String? = null,
    val approverId: Long? = null,
)

@Service
class CreateRequestUseCase(
    private val leaveRequestRepository: LeaveRequestRepository,
    private val employeeRepository: EmployeeRepository,
    private val ledgerBridge: FabricLedgerBridge,
) {
    fun execute(authId: UUID, command: CreateRequestCommand): LeaveRequestEntity {
        val employee = employeeRepository.findEmployeeByAuthId(authId) ?: throw EmployeeNotFoundException()
        val approver = command.approverId?.let {
            employeeRepository.findEmployeeById(it) ?: throw EmployeeNotFoundException()
        }
        val now = Timestamp.from(Instant.now())
        val request = LeaveRequestEntity(
            employee = employee,
            requestType = command.requestType,
            status = "PENDING",
            startDate = command.startDate,
            endDate = command.endDate,
            session = command.session,
            reason = command.reason,
            photoUrl = command.photoUrl,
            approver = approver,
            createdAt = now,
            updatedAt = now,
        )
        val saved = leaveRequestRepository.save(request)
        ledgerBridge.logRequest(employee.id.toString(), command.requestType, "CREATE", employee.auth.email)
        return saved
    }
}
