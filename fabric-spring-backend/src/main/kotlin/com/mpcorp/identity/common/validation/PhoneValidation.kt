package com.mpcorp.identity.common.validation

class PhoneValidation {
    fun isValid(phone: String): Boolean {
        val value = phone.trim()
        return PHONE_REGEX.matches(value)
    }

    companion object {
        // Basic international phone validation: allows optional '+' and 9..15 digits.
        private val PHONE_REGEX = Regex("^\\+?\\d{9,15}$")
    }
}