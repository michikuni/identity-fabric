package com.mpcorp.identity.domain.repository

import com.mpcorp.identity.domain.entity.EmployeeEntity
import java.util.UUID

interface EmployeeRepository {
    fun createEmployee(employee: EmployeeEntity): EmployeeEntity
    fun findEmployeeById(id: Long): EmployeeEntity?
    fun findEmployeeByAuthId(id: UUID): EmployeeEntity?
    fun updateEmployeeByAuthId(employee: EmployeeEntity): EmployeeEntity
    fun deleteEmployeeByAuthId(id: UUID)
}
