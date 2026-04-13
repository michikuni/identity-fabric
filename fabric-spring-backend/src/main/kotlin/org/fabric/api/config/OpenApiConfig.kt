package org.fabric.api.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Identity Ledger - Fabric API")
                .description(
                    """
                    REST API ghi và truy vấn identity audit records trên Hyperledger Fabric.
                    Nhận dữ liệu từ com.mpcorp.identity (Profile, Contract, Payroll) và ghi vào blockchain.
                    """.trimIndent()
                )
                .version("1.0.0")
                .contact(Contact().name("MpCorp Dev Team"))
                .license(License().name("Apache 2.0"))
        )
}
