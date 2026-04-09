package com.mpcorp.identity.application.dto.employee

import com.mpcorp.identity.application.references.AuthRefModel
import com.mpcorp.identity.application.references.ContractRefModel
import com.mpcorp.identity.application.references.EmployeeRefModel
import com.mpcorp.identity.application.references.PayrollRefModel
import com.mpcorp.identity.application.references.ProfileRefModel
import java.sql.Timestamp

data class GetEmployeeResponseCommand(
    val id: Long,
    val auth: com.mpcorp.identity.application.references.AuthRefModel,
    val department: String,
    val position: String,
    val status: String,
    val workingType: String,
    val isActive: Boolean,
    val manager: com.mpcorp.identity.application.references.EmployeeRefModel?,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
    val createdBy: String,
    val note: String?,
    val profile: com.mpcorp.identity.application.references.ProfileRefModel? = null,
    val contract: com.mpcorp.identity.application.references.ContractRefModel? = null,
    val payroll: com.mpcorp.identity.application.references.PayrollRefModel? = null
)
