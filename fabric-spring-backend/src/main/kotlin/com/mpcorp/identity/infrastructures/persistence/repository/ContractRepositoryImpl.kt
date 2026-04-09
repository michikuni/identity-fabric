package com.mpcorp.identity.infrastructures.persistence.repository

import com.mpcorp.identity.common.exception.ContractNotFoundException
import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.domain.entity.ContractEntity
import com.mpcorp.identity.domain.repository.ContractRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.ContractJpaEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.ContractJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import com.mpcorp.identity.infrastructures.persistence.mapper.toDomainEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ContractRepositoryImpl (
    private val contractJpaRepository: ContractJpaRepository,
    private val employeeJpaRepository: EmployeeJpaRepository,
) : ContractRepository {
    @Transactional
    override fun createContract(contract: ContractEntity): ContractEntity {
        val employeeId = contract.employee.id ?: throw EmployeeNotFoundException()
        val contractJpaData = ContractJpaEntity(
            employee = employeeJpaRepository.findById(employeeId).orElseThrow(::EmployeeNotFoundException),
            typeContract = contract.typeContract,
            startDate = contract.startDate,
            endDate = contract.endDate,
            contractExpire = contract.contractExpire,
            probationStartDate = contract.probationStartDate,
            probationEndDate = contract.probationEndDate,
            taxCode = contract.taxCode,
            socialInsuranceNumber = contract.socialInsuranceNumber,
            healthInsuranceNumber = contract.healthInsuranceNumber,
        )
        val dataSaveContract = contractJpaRepository.save(contractJpaData)
        return dataSaveContract.toDomainEntity()
    }

    override fun findContractByEmployeeId(userId: Long): ContractEntity? {
        val contractJpaData = contractJpaRepository.findContractByEmployeeId(userId) ?: return null
        return contractJpaData.toDomainEntity()
    }

    override fun updateContract(contract: ContractEntity): ContractEntity {
        val employeeId = contract.employee.id ?: throw ContractNotFoundException()
        val existingContract = contractJpaRepository.findContractByEmployeeId(employeeId) ?: throw ContractNotFoundException()
        existingContract.apply {
            employee = employeeJpaRepository.findById(employeeId).orElseThrow(::EmployeeNotFoundException)
            typeContract = contract.typeContract
            startDate = contract.startDate
            endDate = contract.endDate
            contractExpire = contract.contractExpire
            probationStartDate = contract.probationStartDate
            probationEndDate = contract.probationEndDate
            taxCode = contract.taxCode
            socialInsuranceNumber = contract.socialInsuranceNumber
            healthInsuranceNumber = contract.healthInsuranceNumber
        }

        return contractJpaRepository.save(existingContract).toDomainEntity()
    }

    @Transactional
    override fun deleteContractByEmployeeId(userId: Long) {
        contractJpaRepository.deleteContractByEmployeeId(userId)
    }

}
