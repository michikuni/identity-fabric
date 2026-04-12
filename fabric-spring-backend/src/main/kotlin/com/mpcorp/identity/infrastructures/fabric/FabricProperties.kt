package com.mpcorp.identity.infrastructures.fabric

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Maps fabric.* entries from application.yml into a typed config object.
 *
 * fabric:
 *   msp-id: Org1MSP
 *   channel-name: mychannel
 *   chaincode-name: asset-transfer
 *   peer:
 *     endpoint: localhost:7051
 *     tls-cert-path: .../tls/ca.crt
 *   gateway:
 *     cert-path: .../signcerts/cert.pem
 *     key-path:  .../keystore/
 */
@ConfigurationProperties(prefix = "fabric")
data class FabricProperties(
    val mspId: String = "Org1MSP",
    val channelName: String = "mychannel",
    val chaincodeName: String = "asset-transfer",
    val peer: PeerProperties = PeerProperties(),
    val gateway: GatewayProperties = GatewayProperties(),
) {
    data class PeerProperties(
        val endpoint: String = "localhost:7051",
        val tlsCertPath: String = "",
    )

    data class GatewayProperties(
        val certPath: String = "",
        val keyPath: String = "",
    )
}
