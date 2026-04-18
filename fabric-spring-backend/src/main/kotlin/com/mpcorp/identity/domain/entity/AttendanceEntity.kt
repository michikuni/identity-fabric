package com.mpcorp.identity.domain.entity

import java.sql.Timestamp
import java.time.LocalDate

class AttendanceEntity(
    val id: Long? = null,
    val employee: EmployeeEntity,
    val workDate: LocalDate,
    val checkInTime: Timestamp? = null,
    val checkOutTime: Timestamp? = null,
    val checkInLocation: String? = null,
    val checkOutLocation: String? = null,
    val status: String = "PRESENT",
    val note: String? = null,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)
