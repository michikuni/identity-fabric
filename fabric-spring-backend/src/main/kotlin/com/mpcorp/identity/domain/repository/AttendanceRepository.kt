package com.mpcorp.identity.domain.repository

import com.mpcorp.identity.domain.entity.AttendanceEntity
import java.time.LocalDate

interface AttendanceRepository {
    fun save(attendance: AttendanceEntity): AttendanceEntity
    fun findByEmployeeIdAndWorkDate(employeeId: Long, workDate: LocalDate): AttendanceEntity?
    fun findByEmployeeIdAndMonth(employeeId: Long, year: Int, month: Int): List<AttendanceEntity>
    fun findTodayByEmployeeId(employeeId: Long): AttendanceEntity?
}
