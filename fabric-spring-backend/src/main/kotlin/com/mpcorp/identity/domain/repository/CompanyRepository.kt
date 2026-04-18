package com.mpcorp.identity.domain.repository

import com.mpcorp.identity.domain.entity.CompanyEntity

interface CompanyRepository {
    fun save(company: CompanyEntity): CompanyEntity
    fun findFirst(): CompanyEntity?
    fun findById(id: Long): CompanyEntity?
    fun existsByTaxCode(taxCode: String): Boolean
}
