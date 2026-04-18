package com.mpcorp.identity.infrastructures.persistence.repository

import com.mpcorp.identity.domain.entity.CompanyEntity
import com.mpcorp.identity.domain.repository.CompanyRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.CompanyJpaEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.CompanyJpaRepository
import com.mpcorp.identity.infrastructures.persistence.mapper.toDomainEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompanyRepositoryImpl(
    private val companyJpaRepository: CompanyJpaRepository,
) : CompanyRepository {

    @Transactional
    override fun save(company: CompanyEntity): CompanyEntity {
        val jpa = if (company.id != null) {
            companyJpaRepository.findById(company.id).orElseGet {
                CompanyJpaEntity(
                    taxCode = company.taxCode, companyName = company.companyName,
                    legalRepName = company.legalRepName, legalRepTitle = company.legalRepTitle,
                    legalRepIdNumber = company.legalRepIdNumber, address = company.address,
                    phone = company.phone, email = company.email, registeredAt = company.registeredAt,
                    createdAt = company.createdAt, updatedAt = company.updatedAt, createdBy = company.createdBy,
                )
            }.apply {
                taxCode = company.taxCode; companyName = company.companyName
                legalRepName = company.legalRepName; legalRepTitle = company.legalRepTitle
                legalRepIdNumber = company.legalRepIdNumber; address = company.address
                phone = company.phone; email = company.email; registeredAt = company.registeredAt
                updatedAt = company.updatedAt
            }
        } else {
            CompanyJpaEntity(
                taxCode = company.taxCode, companyName = company.companyName,
                legalRepName = company.legalRepName, legalRepTitle = company.legalRepTitle,
                legalRepIdNumber = company.legalRepIdNumber, address = company.address,
                phone = company.phone, email = company.email, registeredAt = company.registeredAt,
                createdAt = company.createdAt, updatedAt = company.updatedAt, createdBy = company.createdBy,
            )
        }
        return companyJpaRepository.save(jpa).toDomainEntity()
    }

    override fun findFirst(): CompanyEntity? = companyJpaRepository.findFirstByOrderByIdAsc()?.toDomainEntity()

    override fun findById(id: Long): CompanyEntity? =
        companyJpaRepository.findById(id).orElse(null)?.toDomainEntity()

    override fun existsByTaxCode(taxCode: String): Boolean = companyJpaRepository.existsByTaxCode(taxCode)
}
