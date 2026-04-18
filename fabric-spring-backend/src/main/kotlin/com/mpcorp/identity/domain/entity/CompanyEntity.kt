package com.mpcorp.identity.domain.entity

import java.sql.Timestamp
import java.time.LocalDate

class CompanyEntity(
    val id: Long? = null,
    val taxCode: String,
    val companyName: String,
    val legalRepName: String,
    val legalRepTitle: String,
    val legalRepIdNumber: String,
    val address: String,
    val phone: String,
    val email: String,
    val registeredAt: LocalDate,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
    val createdBy: String,
)
