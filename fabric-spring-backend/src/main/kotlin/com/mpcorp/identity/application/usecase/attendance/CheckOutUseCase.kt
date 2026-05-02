package com.mpcorp.identity.application.usecase.attendance

import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.domain.entity.AttendanceEntity
import com.mpcorp.identity.domain.repository.AttendanceRepository
import com.mpcorp.identity.domain.repository.EmployeeRepository
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Service
class CheckOutUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val employeeRepository: EmployeeRepository,
    private val ledgerBridge: FabricLedgerBridge,
) {
    fun execute(authId: UUID, location: String? = null): AttendanceEntity {
        val employee = employeeRepository.findEmployeeByAuthId(authId) ?: throw EmployeeNotFoundException()
        val now = Timestamp.from(Instant.now())

        val today = attendanceRepository.findTodayByEmployeeId(employee.id!!)
            ?: throw Exception("No check-in record for today")

        val updated = AttendanceEntity(
            id = today.id,
            employee = today.employee,
            workDate = today.workDate,
            checkInTime = today.checkInTime,
            checkOutTime = now,
            checkInLocation = today.checkInLocation,
            checkOutLocation = location ?: today.checkOutLocation,
            status = today.status,
            note = today.note,
            createdAt = today.createdAt,
            updatedAt = now,
        )
        val saved = attendanceRepository.save(updated)
        ledgerBridge.logAttendance(employee.id.toString(), "CHECK_OUT", employee.auth.email)
        return saved
    }
}
