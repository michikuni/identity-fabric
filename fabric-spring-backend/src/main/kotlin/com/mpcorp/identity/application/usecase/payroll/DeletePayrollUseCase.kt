package com.mpcorp.identity.application.usecase.payroll

import com.mpcorp.identity.application.dto.payroll.DeletePayrollCommand
import com.mpcorp.identity.application.support.requireEmployeeId
import com.mpcorp.identity.domain.repository.PayrollRepository
import org.springframework.stereotype.Service

@Service
class DeletePayrollUseCase(
    private val payrollRepository: PayrollRepository,
) {
    fun execute(command: DeletePayrollCommand) {
        payrollRepository.deletePayrollById(command.employee.requireEmployeeId())
    }
}
