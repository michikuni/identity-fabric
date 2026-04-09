package com.mpcorp.identity.presentation.request.contract

import com.mpcorp.identity.presentation.model.ContractRefPayload
import java.sql.Timestamp

data class UpdateContractRequest(
    val contract: ContractRefPayload,
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
