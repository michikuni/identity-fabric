package com.mpcorp.identity.presentation.controller

import com.mpcorp.identity.common.response.ApiResponse
import com.mpcorp.identity.infrastructures.persistence.jpa_repository.EmployeeJpaRepository
import com.mpcorp.identity.infrastructures.vc.VcIssuerService
import org.fabric.api.model.DIDDocument
import org.fabric.api.service.IdentityLedgerService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * IdentityController — public endpoints cho DID resolution và VC retrieval.
 *
 * Không yêu cầu auth — Verifier (ngân hàng, nhà tuyển dụng) có thể
 * query mà không cần tài khoản trong hệ thống.
 */
@RestController
@RequestMapping("/api/v1/identity")
class IdentityController(
    private val ledgerService: IdentityLedgerService,
    private val employeeJpaRepository: EmployeeJpaRepository,
    private val vcIssuerService: VcIssuerService,
) {

    /**
     * Resolve DID Document từ Fabric ledger.
     * GET /api/v1/identity/did/{did}
     */
    @GetMapping("/did/{did}")
    fun resolveDid(@PathVariable did: String): ApiResponse<DIDDocument> {
        val doc = ledgerService.resolveDID(did)
        return ApiResponse(status = "200", message = "OK", data = doc)
    }

    /**
     * Lấy EmploymentVC của nhân viên (nhân viên tự fetch về wallet).
     * GET /api/v1/identity/vc/employment/{employeeId}
     *
     * Trả về VC JSON string đã ký. Flutter lưu vào SecureStorage.
     */
    @GetMapping("/vc/employment/{employeeId}")
    fun getEmploymentVC(@PathVariable employeeId: Long): ApiResponse<Map<String, String>> {
        val employee = employeeJpaRepository.findById(employeeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found")
        }
        val vc = employee.employmentVc
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "VC not issued yet — account may not be approved")
        return ApiResponse(status = "200", message = "OK", data = mapOf("vc" to vc))
    }

    /**
     * Lấy TerminationVC của nhân viên đã bị terminate.
     * GET /api/v1/identity/vc/termination/{employeeId}
     */
    @GetMapping("/vc/termination/{employeeId}")
    fun getTerminationVC(@PathVariable employeeId: Long): ApiResponse<Map<String, String>> {
        val employee = employeeJpaRepository.findById(employeeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found")
        }
        val vc = employee.terminationVc
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Termination VC not issued — employee may not be terminated")
        return ApiResponse(status = "200", message = "OK", data = mapOf("vc" to vc))
    }

    /**
     * GET /api/v1/identity/vc/salary/{employeeId}
     */
    @GetMapping("/vc/salary/{employeeId}")
    fun getSalaryRangeVC(@PathVariable employeeId: Long): ApiResponse<Map<String, String>> {
        val employee = employeeJpaRepository.findById(employeeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found")
        }
        val vc = employee.salaryRangeVc
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "SalaryRangeVC not issued yet")
        return ApiResponse(status = "200", message = "OK", data = mapOf("vc" to vc))
    }

    /**
     * GET /api/v1/identity/vc/promotion/{employeeId}
     */
    @GetMapping("/vc/promotion/{employeeId}")
    fun getPromotionVC(@PathVariable employeeId: Long): ApiResponse<Map<String, String>> {
        val employee = employeeJpaRepository.findById(employeeId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found")
        }
        val vc = employee.promotionVc
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "PromotionVC not issued yet")
        return ApiResponse(status = "200", message = "OK", data = mapOf("vc" to vc))
    }

    /**
     * Verify một VC — Verifier gửi VC JSON lên để check chữ ký và expiry.
     * POST /api/v1/identity/vc/verify
     */
    @org.springframework.web.bind.annotation.PostMapping("/vc/verify")
    fun verifyVC(@org.springframework.web.bind.annotation.RequestBody body: Map<String, String>): ApiResponse<Map<String, Any>> {
        val vcJson = body["vc"] ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing 'vc' field")
        val result = vcIssuerService.verifyVC(vcJson)
        return ApiResponse(
            status = "200", message = "OK",
            data = mapOf("valid" to result.valid, "reason" to result.reason),
        )
    }
}
