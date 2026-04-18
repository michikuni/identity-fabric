package com.mpcorp.identity.common.constant

object ErrorMessage {
    const val BAD_REQUEST = "Request body is invalid"
    const val DATA_CONFLICT = "Request conflicts with existing data"
    const val USER_NOT_FOUND = "User not found"
    const val EMPLOYEE_NOT_FOUND = "Employee not found"
    const val EMPLOYEE_ALREADY_EXISTS = "Employee already exists"
    const val CONTRACT_NOT_FOUND = "Contract not found"
    const val PROFILE_NOT_FOUND = "Profile not found"
    const val PAYROLL_NOT_FOUND = "Payroll not found"
    const val INVALID_PASSWORD = "Password invalid"
    const val USER_ALREADY_EXISTS = "User already exists"
    const val SERVER_ERROR = "Internal server error"
    const val ACCOUNT_PENDING = "Account is pending approval by admin"
    const val ACCOUNT_REJECTED = "Account has been rejected"
}
