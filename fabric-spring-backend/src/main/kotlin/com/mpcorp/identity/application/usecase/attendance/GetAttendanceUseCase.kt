package com.mpcorp.identity.application.usecase.attendance

import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.domain.entity.AttendanceEntity
import com.mpcorp.identity.domain.repository.AttendanceRepository
import com.mpcorp.identity.domain.repository.EmployeeRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GetAttendanceUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val employeeRepository: EmployeeRepository,
) {
    fun execute(authId: UUID, year: Int, month: Int): List<AttendanceEntity> {
        val employee = employeeRepository.findEmployeeByAuthId(authId) ?: throw EmployeeNotFoundException()
        return attendanceRepository.findByEmployeeIdAndMonth(employee.id!!, year, month)
    }

    fun executeToday(authId: UUID): AttendanceEntity? {
        val employee = employeeRepository.findEmployeeByAuthId(authId) ?: throw EmployeeNotFoundException()
        return attendanceRepository.findTodayByEmployeeId(employee.id!!)
    }
}
