package com.mpcorp.identity.infrastructures.persistence.repository

import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.domain.entity.AttendanceEntity
import com.mpcorp.identity.domain.repository.AttendanceRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.AttendanceJpaEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.AttendanceJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import com.mpcorp.identity.infrastructures.persistence.mapper.toDomainEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

private val VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh")

@Service
class AttendanceRepositoryImpl(
    private val attendanceJpaRepository: AttendanceJpaRepository,
    private val employeeJpaRepository: EmployeeJpaRepository,
) : AttendanceRepository {

    @Transactional
    override fun save(attendance: AttendanceEntity): AttendanceEntity {
        val employeeId = attendance.employee.id ?: throw EmployeeNotFoundException()
        val existing = attendanceJpaRepository.findByEmployeeIdAndWorkDate(employeeId, attendance.workDate)

        val jpa = if (existing != null) {
            existing.apply {
                checkInTime = attendance.checkInTime ?: checkInTime
                checkOutTime = attendance.checkOutTime ?: checkOutTime
                checkInLocation = attendance.checkInLocation ?: checkInLocation
                checkOutLocation = attendance.checkOutLocation ?: checkOutLocation
                status = attendance.status
                note = attendance.note
                updatedAt = attendance.updatedAt
            }
        } else {
            val emp = employeeJpaRepository.findById(employeeId).orElseThrow(::EmployeeNotFoundException)
            AttendanceJpaEntity(
                employee = emp,
                workDate = attendance.workDate,
                checkInTime = attendance.checkInTime,
                checkOutTime = attendance.checkOutTime,
                checkInLocation = attendance.checkInLocation,
                checkOutLocation = attendance.checkOutLocation,
                status = attendance.status,
                note = attendance.note,
                createdAt = attendance.createdAt,
                updatedAt = attendance.updatedAt,
            )
        }
        return attendanceJpaRepository.save(jpa).toDomainEntity()
    }

    override fun findByEmployeeIdAndWorkDate(employeeId: Long, workDate: LocalDate): AttendanceEntity? =
        attendanceJpaRepository.findByEmployeeIdAndWorkDate(employeeId, workDate)?.toDomainEntity()

    override fun findByEmployeeIdAndMonth(employeeId: Long, year: Int, month: Int): List<AttendanceEntity> =
        attendanceJpaRepository.findByEmployeeIdAndMonth(employeeId, year, month).map { it.toDomainEntity() }

    override fun findTodayByEmployeeId(employeeId: Long): AttendanceEntity? =
        attendanceJpaRepository.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now(VN_ZONE))?.toDomainEntity()
}
