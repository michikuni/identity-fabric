package com.mpcorp.identity.common.exception

import com.mpcorp.identity.common.constant.ErrorMessage
import com.mpcorp.identity.common.constant.StatusMessage
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.*
import org.slf4j.LoggerFactory

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(UserNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleUserNotFound(
        ex: UserNotFoundException
    ): ErrorResponse {

        return ErrorResponse(
            message = ex.message ?: ErrorMessage.USER_NOT_FOUND,
            code = HttpStatus.NOT_FOUND.value(),
            status = StatusMessage.FAILURE,
            data = HttpStatus.NOT_FOUND.reasonPhrase,
        )
    }

    @ExceptionHandler(InvalidPasswordException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleInvalidPassword(
        ex: InvalidPasswordException
    ): ErrorResponse {

        return ErrorResponse(
            message = ex.message ?: ErrorMessage.INVALID_PASSWORD,
            code = HttpStatus.UNAUTHORIZED.value(),
            status = StatusMessage.FAILURE,
            data = HttpStatus.UNAUTHORIZED.reasonPhrase,
        )
    }

    @ExceptionHandler(UserAlreadyExistingException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleUserAlreadyExisting(ex: UserAlreadyExistingException): ErrorResponse {
        return ErrorResponse(
            message = ex.message ?: ErrorMessage.USER_ALREADY_EXISTS,
            code = HttpStatus.CONFLICT.value(),
            status = StatusMessage.FAILURE,
            data = HttpStatus.CONFLICT.reasonPhrase,
        )
    }

    @ExceptionHandler(EmployeeNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleEmployeeNotFound(ex: EmployeeNotFoundException): ErrorResponse {
        return ErrorResponse(
            message = ex.message ?: ErrorMessage.EMPLOYEE_NOT_FOUND,
            code = HttpStatus.NOT_FOUND.value(),
            status = StatusMessage.FAILURE,
            data = HttpStatus.NOT_FOUND.reasonPhrase,
        )
    }

    @ExceptionHandler(EmployeeAlreadyExistingException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleEmployeeAlreadyExisting(ex: EmployeeAlreadyExistingException): ErrorResponse {
        return ErrorResponse(
            message = ex.message ?: ErrorMessage.EMPLOYEE_ALREADY_EXISTS,
            code = HttpStatus.CONFLICT.value(),
            status = StatusMessage.FAILURE,
            data = HttpStatus.CONFLICT.reasonPhrase,
        )
    }

    @ExceptionHandler(ContractNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleContractNotFound(ex: ContractNotFoundException): ErrorResponse {
        return ErrorResponse(
            message = ex.message ?: ErrorMessage.CONTRACT_NOT_FOUND,
            code = HttpStatus.NOT_FOUND.value(),
            status = StatusMessage.FAILURE,
            data = HttpStatus.NOT_FOUND.reasonPhrase,
        )
    }

    @ExceptionHandler(ProfileNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleProfileNotFound(ex: ProfileNotFoundException): ErrorResponse {
        return ErrorResponse(
            message = ex.message ?: ErrorMessage.PROFILE_NOT_FOUND,
            code = HttpStatus.NOT_FOUND.value(),
            status = StatusMessage.FAILURE,
            data = HttpStatus.NOT_FOUND.reasonPhrase,
        )
    }

    @ExceptionHandler(PayrollNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handlePayrollNotFound(ex: PayrollNotFoundException): ErrorResponse {
        return ErrorResponse(
            message = ex.message ?: ErrorMessage.PAYROLL_NOT_FOUND,
            code = HttpStatus.NOT_FOUND.value(),
            status = StatusMessage.FAILURE,
            data = HttpStatus.NOT_FOUND.reasonPhrase,
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBadRequest(ex: HttpMessageNotReadableException): ErrorResponse {
        logger.warn("Invalid request body", ex)
        return ErrorResponse(
            message = ErrorMessage.BAD_REQUEST,
            code = HttpStatus.BAD_REQUEST.value(),
            status = StatusMessage.FAILURE,
            data = HttpStatus.BAD_REQUEST.reasonPhrase,
        )
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleDataIntegrityViolation(ex: DataIntegrityViolationException): ErrorResponse {
        logger.warn("Data conflict", ex)
        return ErrorResponse(
            message = ErrorMessage.DATA_CONFLICT,
            code = HttpStatus.CONFLICT.value(),
            status = StatusMessage.FAILURE,
            data = HttpStatus.CONFLICT.reasonPhrase,
        )
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleException(ex: Exception): ErrorResponse {
        logger.error("Unhandled exception", ex)

        return ErrorResponse(
            message = ErrorMessage.SERVER_ERROR,
            code = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            status = StatusMessage.FAILURE,
            data = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
        )
    }
}
