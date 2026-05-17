package com.mpcorp.identity.common.exception

class UserAlreadyExistingException(message: String = "User already exists! Create a new user.") : RuntimeException(message)