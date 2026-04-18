package com.mpcorp.identity.infrastructures.persistence.mapper

import com.mpcorp.identity.domain.entity.LeaveRequestEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.LeaveRequestJpaEntity

fun LeaveRequestJpaEntity.toDomainEntity(): LeaveRequestEntity = LeaveRequestEntity(
    id = id,
    employee = employee.toDomainEntity(),
    requestType = requestType,
    status = status,
    startDate = startDate,
    endDate = endDate,
    session = session,
    reason = reason,
    photoUrl = photoUrl,
    approver = approver?.toDomainEntity(),
    approvedAt = approvedAt,
    rejectedReason = rejectedReason,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
