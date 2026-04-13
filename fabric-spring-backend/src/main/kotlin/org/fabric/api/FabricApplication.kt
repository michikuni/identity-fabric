package org.fabric.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["org.fabric.api", "com.mpcorp.identity"])
@EnableAsync
@EnableScheduling
class FabricApplication

fun main(args: Array<String>) {
    runApplication<FabricApplication>(*args)
}
