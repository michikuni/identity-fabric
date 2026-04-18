package com.mpcorp.identity.application.usecase.attendance

import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.domain.entity.AttendanceEntity
import com.mpcorp.identity.domain.repository.AttendanceRepository
import com.mpcorp.identity.domain.repository.EmployeeRepository
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class CheckInUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val employeeRepository: EmployeeRepository,
    private val ledgerBridge: FabricLedgerBridge,
) {
    fun execute(authId: UUID, location: String? = null, note: String? = null): AttendanceEntity {
        val employee = employeeRepository.findEmployeeByAuthId(authId) ?: throw EmployeeNotFoundException()
        val now = Timestamp.from(Instant.now())
        val today = LocalDate.now()

        val existing = attendanceRepository.findTodayByEmployeeId(employee.id!!)
        if (existing?.checkInTime != null) return existing  // Already checked in

        val attendance = AttendanceEntity(
            employee = employee,
            workDate = today,
            checkInTime = now,
            checkInLocation = location,
            status = "PRESENT",
            note = note,
            createdAt = now,
            updatedAt = now,
        )
        val saved = attendanceRepository.save(attendance)
        ledgerBridge.logAttendance(employee.id.toString(), "CHECK_IN", employee.auth.email)
        return saved
    }
}
