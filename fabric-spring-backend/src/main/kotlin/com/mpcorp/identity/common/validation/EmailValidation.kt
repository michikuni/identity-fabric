package com.mpcorp.identity.common.validation

class EmailValidation {
    fun isValid(email: String): Boolean {
        val value = email.trim()
        return EMAIL_REGEX.matches(value)
    }

    companion object {
        // Basic email format validation (intentionally simple)
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    }
}