package com.mpcorp.identity.infrastructures.persistence.mapper

import com.mpcorp.identity.domain.entity.AttendanceEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.AttendanceJpaEntity

fun AttendanceJpaEntity.toDomainEntity(): AttendanceEntity = AttendanceEntity(
    id = id,
    employee = employee.toDomainEntity(),
    workDate = workDate,
    checkInTime = checkInTime,
    checkOutTime = checkOutTime,
    checkInLocation = checkInLocation,
    checkOutLocation = checkOutLocation,
    status = status,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
