package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import org.springframework.web.bind.annotation.*

data class DirectoryItem(
    val id: Long,
    val name: String,
    val email: String,
    val department: String,
    val position: String,
    val role: String,
    val status: String,
)

@RestController
@RequestMapping("/api/v1/directory")
class DirectoryController(
    private val employeeJpaRepository: EmployeeJpaRepository,
) {
    @GetMapping
    fun list(): ApiResponse<Any> {
        val items = employeeJpaRepository.findAll().map { emp ->
            DirectoryItem(
                id = emp.id ?: 0,
                name = emp.profile?.name ?: emp.auth.email,
                email = emp.auth.email,
                department = emp.department,
                position = emp.position,
                role = emp.auth.role.name,
                status = emp.status,
            )
        }
        return ApiResponse(status = "200", message = "OK", data = items)
    }

    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long): ApiResponse<Any> {
        val emp = employeeJpaRepository.findById(id).orElseThrow { Exception("Not found") }
        val item = DirectoryItem(
            id = emp.id ?: 0,
            name = emp.profile?.name ?: emp.auth.email,
            email = emp.auth.email,
            department = emp.department,
            position = emp.position,
            role = emp.auth.role.name,
            status = emp.status,
        )
        return ApiResponse(status = "200", message = "OK", data = item)
    }
}
