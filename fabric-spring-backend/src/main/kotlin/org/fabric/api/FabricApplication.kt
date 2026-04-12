package org.fabric.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [
        DataSourceAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,
        SecurityAutoConfiguration::class,
    ],
    scanBasePackages = ["org.fabric.api"]
)
class FabricApplication

fun main(args: Array<String>) {
    runApplication<FabricApplication>(*args)
}
