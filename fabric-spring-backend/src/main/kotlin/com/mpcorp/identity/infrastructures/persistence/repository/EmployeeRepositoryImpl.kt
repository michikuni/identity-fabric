package com.mpcorp.identity.infrastructures.persistence.repository

import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.common.exception.UserNotFoundException
import com.mpcorp.identity.domain.entity.EmployeeEntity
import com.mpcorp.identity.domain.repository.EmployeeRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.EmployeeJpaEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.AuthJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import com.mpcorp.identity.infrastructures.persistence.mapper.toDomainEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class EmployeeRepositoryImpl(
    private val employeeJpaRepository: EmployeeJpaRepository,
    private val authJpaRepository: AuthJpaRepository,
) : EmployeeRepository {
    @Transactional
    override fun createEmployee(
        employee: EmployeeEntity
    ): EmployeeEntity {
        val authId = employee.auth.id ?: throw UserNotFoundException()
        val employeeJpaData = EmployeeJpaEntity(
            auth = authJpaRepository.findById(authId).orElseThrow(::UserNotFoundException),
            department = employee.department,
            position = employee.position,
            status = employee.status,
            workingType = employee.workingType,
            isActive = employee.isActive,
            manager = employee.manager?.id?.let { employeeJpaRepository.findById(it).orElse(null) },
            createdAt = employee.createdAt,
            updatedAt = employee.updatedAt,
            createdBy = employee.createdBy,
            note = employee.note,
            publicKey = employee.publicKey,
        )
        val dataSaveEmployee = employeeJpaRepository.save(employeeJpaData)
        return dataSaveEmployee.toDomainEntity()
    }

    override fun findEmployeeByAuthId(id: UUID): EmployeeEntity? {
        val employeeJpaData = employeeJpaRepository.findEmployeeByAuthId(id) ?: return null
        return employeeJpaData.toDomainEntity()
    }

    override fun findEmployeeById(id: Long): EmployeeEntity? {
        val employeeJpaData = employeeJpaRepository.findById(id).orElse(null) ?: return null
        return employeeJpaData.toDomainEntity()
    }

    override fun updateEmployeeByAuthId(
        employee: EmployeeEntity
    ): EmployeeEntity {
        val authId = employee.auth.id ?: throw UserNotFoundException()
        val existingEmployee = employeeJpaRepository.findEmployeeByAuthId(authId) ?: throw EmployeeNotFoundException()
        existingEmployee.apply {
            manager = employee.manager?.id?.let { employeeJpaRepository.findById(it).orElse(null) }
            department = employee.department
            position = employee.position
            status = employee.status
            workingType = employee.workingType
            isActive = employee.isActive
            updatedAt = employee.updatedAt
            note = employee.note
        }

        return employeeJpaRepository.save(existingEmployee).toDomainEntity()
    }

    @Transactional
    override fun deleteEmployeeByAuthId(id: UUID) {
        employeeJpaRepository.deleteEmployeeByAuthId(id)
    }
}
