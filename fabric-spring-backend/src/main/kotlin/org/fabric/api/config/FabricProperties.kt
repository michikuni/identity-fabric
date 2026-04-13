package org.fabric.api.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Maps fabric.* properties từ application.yml.
 *
 * fabric:
 *   msp-id: Org1MSP
 *   channel-name: mychannel
 *   chaincode-name: identity-ledger      ← chaincode IdentityLedger.java
 *   peer:
 *     endpoint: localhost:7051
 *     tls-cert-path: .../tls/ca.crt
 *   gateway:
 *     cert-path: .../signcerts/cert.pem
 *     key-path:  .../keystore/
 */
@Component
@ConfigurationProperties(prefix = "fabric")
data class FabricProperties(
    var mspId: String = "Org1MSP",
    var channelName: String = "mychannel",
    var chaincodeName: String = "identity-ledger",
    var peer: PeerProperties = PeerProperties(),
    var gateway: GatewayProperties = GatewayProperties(),
) {
    data class PeerProperties(
        var endpoint: String = "localhost:7051",
        var tlsCertPath: String = "",
    )

    data class GatewayProperties(
        var certPath: String = "",
        var keyPath: String = "",
    )
}