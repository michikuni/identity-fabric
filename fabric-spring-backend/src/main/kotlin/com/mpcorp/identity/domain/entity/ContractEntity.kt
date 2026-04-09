package com.mpcorp.identity.domain.entity

import java.sql.Timestamp

data class ContractEntity(
    val id: Long? = null,
    val employee: EmployeeEntity,
    val typeContract: String,
    val startDate: Timestamp,
    val endDate: Timestamp?,
    val contractExpire: Timestamp?,
    val probationStartDate: Timestamp?,
    val probationEndDate: Timestamp?,
    val taxCode: String,
    val socialInsuranceNumber: String?,
    val healthInsuranceNumber: String?,
)