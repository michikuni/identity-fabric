package com.mpcorp.identity.application.dto.contract

import com.mpcorp.identity.application.references.ContractRefModel
import com.mpcorp.identity.application.references.EmployeeRefModel
import java.sql.Timestamp

data class UpdateContractCommand(
    val contract: com.mpcorp.identity.application.references.ContractRefModel,
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
