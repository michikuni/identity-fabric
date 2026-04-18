package com.mpcorp.identity.infrastructures.persistence.jpa_repository

import com.mpcorp.identity.infrastructures.persistence.jpa_entity.CompanyJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CompanyJpaRepository : JpaRepository<CompanyJpaEntity, Long> {
    fun existsByTaxCode(taxCode: String): Boolean
    fun findFirstByOrderByIdAsc(): CompanyJpaEntity?
}
