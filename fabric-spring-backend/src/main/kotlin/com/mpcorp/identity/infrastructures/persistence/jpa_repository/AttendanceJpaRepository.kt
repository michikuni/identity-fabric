package com.mpcorp.identity.infrastructures.persistence.jpa_repository

import com.mpcorp.identity.infrastructures.persistence.jpa_entity.AttendanceJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface AttendanceJpaRepository : JpaRepository<AttendanceJpaEntity, Long> {

    fun findByEmployeeIdAndWorkDate(employeeId: Long, workDate: LocalDate): AttendanceJpaEntity?

    @Query("""
        SELECT a FROM AttendanceJpaEntity a
        WHERE a.employee.id = :employeeId
          AND YEAR(a.workDate) = :year
          AND MONTH(a.workDate) = :month
        ORDER BY a.workDate ASC
    """)
    fun findByEmployeeIdAndMonth(
        @Param("employeeId") employeeId: Long,
        @Param("year") year: Int,
        @Param("month") month: Int,
    ): List<AttendanceJpaEntity>

    @Query("""
        SELECT a FROM AttendanceJpaEntity a
        WHERE a.employee.manager.id = :managerId
          AND YEAR(a.workDate) = :year
          AND MONTH(a.workDate) = :month
        ORDER BY a.employee.id ASC, a.workDate ASC
    """)
    fun findByManagerIdAndMonth(
        @Param("managerId") managerId: Long,
        @Param("year") year: Int,
        @Param("month") month: Int,
    ): List<AttendanceJpaEntity>
}
