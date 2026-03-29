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
                .title("Hyperledger Fabric – Asset Transfer API")
                .description(
                    """
                    REST API for interacting with the Hyperledger Fabric 2-Org network.
                    Provides full CRUD and transfer operations on blockchain assets via the Fabric Gateway SDK.
                    """.trimIndent()
                )
                .version("1.0.0")
                .contact(Contact().name("Fabric Dev Team"))
                .license(License().name("Apache 2.0"))
        )
}
