package com.mpcorp.identity.infrastructures.fabric

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Maps fabric.* properties from application.yml.
 */
@Component
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
