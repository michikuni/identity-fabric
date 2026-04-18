package com.mpcorp.identity.application.usecase.company

import com.mpcorp.identity.domain.entity.CompanyEntity
import com.mpcorp.identity.domain.repository.CompanyRepository
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate

data class SaveCompanyCommand(
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
    val actorEmail: String,
)

@Service
class GetCompanyUseCase(private val companyRepository: CompanyRepository) {
    fun execute(): CompanyEntity? = companyRepository.findFirst()
}

@Service
class SaveCompanyUseCase(
    private val companyRepository: CompanyRepository,
    private val ledgerBridge: FabricLedgerBridge,
) {
    fun execute(command: SaveCompanyCommand): CompanyEntity {
        val now = Timestamp.from(Instant.now())
        val existing = companyRepository.findFirst()
        val isNew = existing == null

        val entity = CompanyEntity(
            id = command.id ?: existing?.id,
            taxCode = command.taxCode,
            companyName = command.companyName,
            legalRepName = command.legalRepName,
            legalRepTitle = command.legalRepTitle,
            legalRepIdNumber = command.legalRepIdNumber,
            address = command.address,
            phone = command.phone,
            email = command.email,
            registeredAt = command.registeredAt,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            createdBy = existing?.createdBy ?: command.actorEmail,
        )
        val saved = companyRepository.save(entity)
        ledgerBridge.logCompany(saved.id.toString(), if (isNew) "CREATE" else "UPDATE", command.actorEmail)
        return saved
    }
}
