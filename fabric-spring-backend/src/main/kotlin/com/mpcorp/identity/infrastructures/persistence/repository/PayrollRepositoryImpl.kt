package com.mpcorp.identity.infrastructures.persistence.repository

import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.common.exception.PayrollNotFoundException
import com.mpcorp.identity.domain.entity.PayrollEntity
import com.mpcorp.identity.domain.repository.PayrollRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_entity.PayrollJpaEntity
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.PayrollJpaRepository
import com.mpcorp.identity.infrastructures.persistence.mapper.toDomainEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PayrollRepositoryImpl(
    private val payrollJpaRepository: PayrollJpaRepository,
    private val employeeJpaRepository: EmployeeJpaRepository,
) : PayrollRepository {
    @Transactional
    override fun createPayrollById(
        payroll: PayrollEntity
    ): PayrollEntity {
        val employeeId = payroll.employee.id ?: throw EmployeeNotFoundException()
        val payrollJpaData = PayrollJpaEntity(
            employee = employeeJpaRepository.findById(employeeId).orElseThrow(::EmployeeNotFoundException),
            salaryType = payroll.salaryType,
            baseSalary = payroll.baseSalary,
            bonusSalary = payroll.bonusSalary,
            overTimeRate = payroll.overTimeRate,
            totalIncome = payroll.totalIncome,
            currency = payroll.currency,
            payDay = payroll.payDay,
            bankAccountNumber = payroll.bankAccountNumber,
            bankAccountName = payroll.bankAccountName,
            bankName = payroll.bankName,
            bankBranch = payroll.bankBranch,
        )
        val dataSavePayroll = payrollJpaRepository.save(payrollJpaData)
        return dataSavePayroll.toDomainEntity()
    }

    override fun findPayrollById(userId: Long): PayrollEntity? {
        val payrollJpaData = payrollJpaRepository.findPayrollByEmployeeId(userId) ?: return null
        return payrollJpaData.toDomainEntity()
    }

    override fun updatePayrollById(
        payroll: PayrollEntity
    ): PayrollEntity {
        val existingPayroll = payrollJpaRepository.findPayrollByEmployeeId(payroll.employee.id)
            ?: throw PayrollNotFoundException()
        existingPayroll.apply {
            val employeeId = payroll.employee.id ?: throw EmployeeNotFoundException()
            employee = employeeJpaRepository.findById(employeeId).orElseThrow(::EmployeeNotFoundException)
            salaryType = payroll.salaryType
            baseSalary = payroll.baseSalary
            bonusSalary = payroll.bonusSalary
            overTimeRate = payroll.overTimeRate
            totalIncome = payroll.totalIncome
            currency = payroll.currency
            payDay = payroll.payDay
            bankAccountNumber = payroll.bankAccountNumber
            bankAccountName = payroll.bankAccountName
            bankName = payroll.bankName
            bankBranch = payroll.bankBranch
        }

        return payrollJpaRepository.save(existingPayroll).toDomainEntity()
    }

    @Transactional
    override fun deletePayrollById(userId: Long) {
        payrollJpaRepository.deletePayrollByEmployeeId(userId)
    }
}
