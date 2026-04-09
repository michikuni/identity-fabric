package com.mpcorp.identity.application.dto.contract

import com.mpcorp.identity.application.references.EmployeeRefModel
import com.mpcorp.identity.application.references.IdentifierModel
import java.sql.Timestamp

data class GetContractResponseCommand(
    val id: com.mpcorp.identity.application.references.IdentifierModel,
    val employee: com.mpcorp.identity.application.references.EmployeeRefModel,
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
