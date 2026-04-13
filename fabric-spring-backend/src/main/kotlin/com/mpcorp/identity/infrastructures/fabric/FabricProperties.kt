package com.mpcorp.identity.infrastructures.fabric

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Standalone Fabric properties — chỉ dùng khi chạy IdentityApplication riêng lẻ.
 *
 * Khi chạy FabricApplication (unified app), org.fabric.api.config.FabricProperties
 * (cũng bind prefix "fabric") sẽ được dùng → class này KHÔNG có @Component
 * để tránh duplicate ConfigurationProperties binding và bean name conflict.
 */
@ConfigurationProperties(prefix = "fabric")
data class FabricProperties(
    var mspId: String = "Org1MSP",
    var channelName: String = "mychannel",
    var chaincodeName: String = "identity-ledger",
    var peer: PeerProperties = PeerProperties(),
    var gateway: GatewayProperties = GatewayProperties()
) {
    data class PeerProperties(
        var endpoint: String = "localhost:7051",
        var tlsCertPath: String = ""
    )

    data class GatewayProperties(
        var certPath: String = "",
        var keyPath: String = ""
    )
}