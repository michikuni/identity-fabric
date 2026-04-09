package com.mpcorp.identity

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IdentityApplication

fun main(args: Array<String>) {
	runApplication<com.mpcorp.identity.IdentityApplication>(*args)
}
