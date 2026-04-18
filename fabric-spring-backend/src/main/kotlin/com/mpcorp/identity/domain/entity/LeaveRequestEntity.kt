package com.mpcorp.identity.domain.entity

import java.sql.Timestamp
import java.time.LocalDate

class LeaveRequestEntity(
    val id: Long? = null,
    val employee: EmployeeEntity,
    val requestType: String,
    val status: String = "PENDING",
    val startDate: LocalDate,
    val endDate: LocalDate,
    val session: String? = null,
    val reason: String,
    val photoUrl: String? = null,
    val approver: EmployeeEntity? = null,
    val approvedAt: Timestamp? = null,
    val rejectedReason: String? = null,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
)
