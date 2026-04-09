package com.mpcorp.identity.domain.repository

import com.mpcorp.identity.domain.entity.PayrollEntity

interface PayrollRepository {
    fun createPayrollById(payroll: PayrollEntity): PayrollEntity
    fun findPayrollById(userId: Long): PayrollEntity?
    fun updatePayrollById(payroll: PayrollEntity): PayrollEntity
    fun deletePayrollById(userId: Long)
}