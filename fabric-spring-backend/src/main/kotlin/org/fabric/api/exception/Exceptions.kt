package org.fabric.api.exception

import mu.KotlinLogging
import org.fabric.api.model.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

private val log = KotlinLogging.logger {}

// ── Domain exceptions ─────────────────────────────────────────────────────────

class IdentityRecordNotFoundException(employeeId: String, recordType: String) :
    RuntimeException("IdentityRecord '$recordType:$employeeId' does not exist on the ledger")

class FabricTransactionException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

// ── Global handler ────────────────────────────────────────────────────────────

@RestControllerAdvice
class FabricExceptionHandler {

    @ExceptionHandler(IdentityRecordNotFoundException::class)
    fun handleNotFound(ex: IdentityRecordNotFoundException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn { ex.message }
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse(success = false, message = ex.message ?: "Record not found"))
    }

    @ExceptionHandler(FabricTransactionException::class)
    fun handleFabricTx(ex: FabricTransactionException): ResponseEntity<ApiResponse<Nothing>> {
        log.error(ex) { "Fabric transaction error: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.BAD_GATEWAY)
            .body(ApiResponse(success = false, message = "Blockchain transaction failed: ${ex.message}"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Map<String, String>>> {
        val errors = ex.bindingResult.allErrors.associate { error ->
            val field = (error as? FieldError)?.field ?: error.objectName
            field to (error.defaultMessage ?: "Invalid value")
        }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse(success = false, message = "Validation failed", data = errors))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error(ex) { "Unhandled exception: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse(success = false, message = "Internal server error: ${ex.message}"))
    }
}