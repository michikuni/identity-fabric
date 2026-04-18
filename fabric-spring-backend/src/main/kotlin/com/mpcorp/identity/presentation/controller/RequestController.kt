package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.application.usecase.request.ApproveRejectRequestUseCase
import com.mpcorp.identity.application.usecase.request.CreateRequestCommand
import com.mpcorp.identity.application.usecase.request.CreateRequestUseCase
import com.mpcorp.identity.application.usecase.request.GetRequestsUseCase
import com.mpcorp.identity.application.usecase.request.GetSubordinateRequestsUseCase
import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.presentation.security.BearerAuthIdResolver
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/requests")
class RequestController(
    private val bearerAuthIdResolver: BearerAuthIdResolver,
    private val createRequestUseCase: CreateRequestUseCase,
    private val getRequestsUseCase: GetRequestsUseCase,
    private val approveRejectUseCase: ApproveRejectRequestUseCase,
) {
    data class CreateRequestBody(
        val requestType: String,
        val startDate: String,
        val endDate: String,
        val session: String? = null,
        val reason: String,
        val photoUrl: String? = null,
        val approverId: Long? = null,
    )

    data class RejectBody(val reason: String)

    @GetMapping
    fun list(httpRequest: HttpServletRequest): ApiResponse<Any> {
        val authId = bearerAuthIdResolver.resolveAuthId(httpRequest)
        return ApiResponse(status = "200", message = "OK", data = getRequestsUseCase.execute(authId))
    }

    @PostMapping
    fun create(httpRequest: HttpServletRequest, @RequestBody body: CreateRequestBody): ApiResponse<Any> {
        val authId = bearerAuthIdResolver.resolveAuthId(httpRequest)
        val result = createRequestUseCase.execute(
            authId, CreateRequestCommand(
                requestType = body.requestType,
                startDate = LocalDate.parse(body.startDate),
                endDate = LocalDate.parse(body.endDate),
                session = body.session,
                reason = body.reason,
                photoUrl = body.photoUrl,
                approverId = body.approverId,
            )
        )
        return ApiResponse(status = "201", message = "Request created", data = result)
    }
}

@RestController
@RequestMapping("/api/v1/manager/requests")
class ManagerRequestController(
    private val bearerAuthIdResolver: BearerAuthIdResolver,
    private val approveRejectUseCase: ApproveRejectRequestUseCase,
    private val getSubordinateRequestsUseCase: GetSubordinateRequestsUseCase,
) {
    data class RejectBody(val reason: String)

    @GetMapping
    fun list(
        httpRequest: HttpServletRequest,
        @RequestParam(defaultValue = "false") pendingOnly: Boolean,
    ): ApiResponse<Any> {
        val authId = bearerAuthIdResolver.resolveAuthId(httpRequest)
        return ApiResponse(status = "200", message = "OK", data = getSubordinateRequestsUseCase.execute(authId, pendingOnly))
    }

    @PutMapping("/{id}/approve")
    fun approve(httpRequest: HttpServletRequest, @PathVariable id: Long): ApiResponse<Any> {
        val authId = bearerAuthIdResolver.resolveAuthId(httpRequest)
        return ApiResponse(status = "200", message = "Approved", data = approveRejectUseCase.approve(authId, id))
    }

    @PutMapping("/{id}/reject")
    fun reject(httpRequest: HttpServletRequest, @PathVariable id: Long, @RequestBody body: RejectBody): ApiResponse<Any> {
        val authId = bearerAuthIdResolver.resolveAuthId(httpRequest)
        return ApiResponse(status = "200", message = "Rejected", data = approveRejectUseCase.reject(authId, id, body.reason))
    }
}
