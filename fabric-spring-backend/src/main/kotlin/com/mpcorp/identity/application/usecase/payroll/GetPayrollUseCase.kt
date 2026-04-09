package com.mpcorp.identity.application.usecase.payroll

import com.mpcorp.identity.application.dto.payroll.GetPayrollRequestCommand
import com.mpcorp.identity.application.dto.payroll.GetPayrollResponseCommand
import com.mpcorp.identity.application.mapper.toGetPayrollResponseCommand
import com.mpcorp.identity.application.support.requireEmployeeId
import com.mpcorp.identity.domain.repository.PayrollRepository
import org.springframework.stereotype.Service

@Service
class GetPayrollUseCase(
    private val payrollRepository: PayrollRepository,
) {
    fun execute(command: GetPayrollRequestCommand): GetPayrollResponseCommand? {
        val payroll = payrollRepository.findPayrollById(command.employee.requireEmployeeId())
            ?: return null
        return payroll.toGetPayrollResponseCommand()
    }
}
