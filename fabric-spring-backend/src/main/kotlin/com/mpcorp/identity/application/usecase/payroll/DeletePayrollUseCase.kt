package com.mpcorp.identity.application.usecase.payroll

import com.mpcorp.identity.application.dto.payroll.DeletePayrollCommand
import com.mpcorp.identity.application.support.requireEmployeeId
import com.mpcorp.identity.domain.repository.PayrollRepository
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import org.springframework.stereotype.Service

@Service
class DeletePayrollUseCase(
    private val payrollRepository: PayrollRepository,
    private val fabricBridge: FabricLedgerBridge,
) {
    fun execute(command: DeletePayrollCommand) {
        val employeeId = command.employee.requireEmployeeId()
        payrollRepository.deletePayrollById(employeeId)
        fabricBridge.deletePayrollRecord(employeeId.toString())
    }
}   