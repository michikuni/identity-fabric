package org.fabric.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["org.fabric.api"])
class FabricApplication

fun main(args: Array<String>) {
    runApplication<FabricApplication>(*args)
}
