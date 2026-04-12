package com.mpcorp.identity

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class IdentityApplication

fun main(args: Array<String>) {
	runApplication<com.mpcorp.identity.IdentityApplication>(*args)
}
