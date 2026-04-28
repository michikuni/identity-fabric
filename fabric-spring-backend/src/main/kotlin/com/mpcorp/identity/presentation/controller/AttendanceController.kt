package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.application.usecase.attendance.CheckInUseCase
import com.mpcorp.identity.application.usecase.attendance.CheckOutUseCase
import com.mpcorp.identity.application.usecase.attendance.GetAttendanceUseCase
import com.mpcorp.identity.common.exception.EmployeeNotFoundException
import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.AttendanceJpaRepository
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import com.mpcorp.identity.infrastructures.persistence.mapper.toDomainEntity
import com.mpcorp.identity.presentation.security.BearerAuthIdResolver
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.*
import java.time.YearMonth

@RestController
@RequestMapping("/api/v1/attendance")
class AttendanceController(
    private val bearerAuthIdResolver: BearerAuthIdResolver,
    private val checkInUseCase: CheckInUseCase,
    private val checkOutUseCase: CheckOutUseCase,
    private val getAttendanceUseCase: GetAttendanceUseCase,
    private val employeeJpaRepository: EmployeeJpaRepository,
    private val attendanceJpaRepository: AttendanceJpaRepository,
) {
    data class CheckInRequest(val location: String? = null, val note: String? = null)
    data class CheckOutRequest(val location: String? = null)

    @PostMapping("/check-in")
    fun checkIn(httpRequest: HttpServletRequest, @RequestBody body: CheckInRequest): ApiResponse<Any> {
        val authId = bearerAuthIdResolver.resolveAuthId(httpRequest)
        val result = checkInUseCase.execute(authId, body.location, body.note)
        return ApiResponse(status = "200", message = "Check-in successful", data = result)
    }

    @PostMapping("/check-out")
    fun checkOut(httpRequest: HttpServletRequest, @RequestBody body: CheckOutRequest): ApiResponse<Any> {
        val authId = bearerAuthIdResolver.resolveAuthId(httpRequest)
        val result = checkOutUseCase.execute(authId, body.location)
        return ApiResponse(status = "200", message = "Check-out successful", data = result)
    }

    @GetMapping("/today")
    fun today(httpRequest: HttpServletRequest): ApiResponse<Any> {
        val authId = bearerAuthIdResolver.resolveAuthId(httpRequest)
        val result = getAttendanceUseCase.executeToday(authId)
        return ApiResponse(status = "200", message = "OK", data = result)
    }

    @GetMapping
    fun history(
        httpRequest: HttpServletRequest,
        @RequestParam(defaultValue = "0") year: Int,
        @RequestParam(defaultValue = "0") month: Int,
    ): ApiResponse<Any> {
        val authId = bearerAuthIdResolver.resolveAuthId(httpRequest)
        val ym = if (year == 0 || month == 0) YearMonth.now() else YearMonth.of(year, month)
        val result = getAttendanceUseCase.execute(authId, ym.year, ym.monthValue)
        return ApiResponse(status = "200", message = "OK", data = result)
    }

    @GetMapping("/team")
    fun teamTimesheet(
        httpRequest: HttpServletRequest,
        @RequestParam(defaultValue = "0") year: Int,
        @RequestParam(defaultValue = "0") month: Int,
    ): ApiResponse<Any> {
        val authId = bearerAuthIdResolver.resolveAuthId(httpRequest)
        val manager = employeeJpaRepository.findEmployeeByAuthId(authId) ?: throw EmployeeNotFoundException()
        val ym = if (year == 0 || month == 0) YearMonth.now() else YearMonth.of(year, month)
        val records = attendanceJpaRepository.findByManagerIdAndMonth(manager.id!!, ym.year, ym.monthValue)
        val grouped = records.groupBy { it.employee.id!! }.map { (_, list) ->
            val emp = list.first().employee
            mapOf(
                "employeeId" to emp.id,
                "employeeName" to (emp.profile?.name ?: emp.auth.email),
                "department" to emp.department,
                "records" to list.map { it.toDomainEntity() },
            )
        }
        return ApiResponse(status = "200", message = "OK", data = grouped)
    }
}
