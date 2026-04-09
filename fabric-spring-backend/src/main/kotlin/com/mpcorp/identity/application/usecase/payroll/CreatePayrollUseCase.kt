package com.mpcorp.identity.application.usecase.payroll

import com.mpcorp.identity.application.dto.payroll.CreatePayrollCommand
import com.mpcorp.identity.application.dto.payroll.GetPayrollResponseCommand
import com.mpcorp.identity.application.mapper.toGetPayrollResponseCommand
import com.mpcorp.identity.application.support.resolveEmployee
import com.mpcorp.identity.domain.entity.PayrollEntity
import com.mpcorp.identity.domain.repository.EmployeeRepository
import com.mpcorp.identity.domain.repository.PayrollRepository
import org.springframework.stereotype.Service

@Service
class CreatePayrollUseCase(
    private val payrollRepository: PayrollRepository,
    private val employeeRepository: EmployeeRepository,
) {
    fun execute(command: CreatePayrollCommand): GetPayrollResponseCommand {
        val employee = command.employee.resolveEmployee(employeeRepository)
        val payroll = PayrollEntity(
            employee = employee,
            salaryType = command.salaryType,
            baseSalary = command.baseSalary,
            bonusSalary = command.bonusSalary,
            overTimeRate = command.overTimeRate,
            totalIncome = command.totalIncome,
            currency = command.currency,
            payDay = command.payDay,
            bankAccountNumber = command.bankAccountNumber,
            bankAccountName = command.bankAccountName,
            bankName = command.bankName,
            bankBranch = command.bankBranch,
        )
        return payrollRepository.createPayrollById(payroll).toGetPayrollResponseCommand()
    }
}
