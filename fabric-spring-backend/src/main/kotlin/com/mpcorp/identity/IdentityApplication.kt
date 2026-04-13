package com.mpcorp.identity

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

/**
 * Standalone entry point — chỉ dùng khi chạy com.mpcorp.identity riêng lẻ (không có Fabric).
 * Entry point chính của unified app là org.fabric.api.FabricApplication.
 *
 * @SpringBootApplication(scanBasePackages = ["com.mpcorp.identity"]) tường minh
 * để tránh scan lại org.fabric.api khi chạy standalone.
 */
@SpringBootApplication(scanBasePackages = ["com.mpcorp.identity"])
@EnableAsync
class IdentityApplication

fun main(args: Array<String>) {
    runApplication<IdentityApplication>(*args)
}
