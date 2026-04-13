package com.mpcorp.identity.infrastructures.fabric

import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import org.hyperledger.fabric.client.Gateway
import org.hyperledger.fabric.client.identity.Identities
import org.hyperledger.fabric.client.identity.Signers
import org.hyperledger.fabric.client.identity.X509Identity
import org.slf4j.LoggerFactory
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Path
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit

/**
 * Standalone Fabric gateway factory — chỉ dùng khi chạy IdentityApplication riêng lẻ.
 *
 * Khi chạy FabricApplication (unified app), org.fabric.api.config.FabricGatewayConfig
 * sẽ tạo các bean Gateway/ManagedChannel → class này KHÔNG được đăng ký là @Configuration
 * để tránh conflict bean name 'fabricGatewayConfig'.
 */
class FabricGatewayConfig(private val props: FabricProperties) {

    private val log = LoggerFactory.getLogger(FabricGatewayConfig::class.java)

    fun grpcChannel(): ManagedChannel {
        log.info("Connecting to Fabric peer at ${props.peer.endpoint}")
        val tlsCert = Path.of(props.peer.tlsCertPath)
        val sslContext = GrpcSslContexts.forClient()
            .trustManager(tlsCert.toFile())
            .build()
        return NettyChannelBuilder
            .forTarget(props.peer.endpoint)
            .sslContext(sslContext)
            .build()
    }

    fun fabricGateway(grpcChannel: ManagedChannel): Gateway {
        val certificate: X509Certificate =
            Identities.readX509Certificate(FileReader(props.gateway.certPath))
        val privateKey: PrivateKey = readPrivateKey(props.gateway.keyPath)

        return Gateway.newInstance()
            .identity(X509Identity(props.mspId, certificate))
            .signer(Signers.newPrivateKeySigner(privateKey))
            .connection(grpcChannel)
            .evaluateOptions { it.withDeadlineAfter(5, TimeUnit.SECONDS) }
            .endorseOptions  { it.withDeadlineAfter(15, TimeUnit.SECONDS) }
            .submitOptions  { it.withDeadlineAfter(5, TimeUnit.SECONDS) }
            .commitStatusOptions { it.withDeadlineAfter(60, TimeUnit.SECONDS) }
            .connect()
    }

    private fun readPrivateKey(keyDirPath: String): PrivateKey {
        val keyDir = Path.of(keyDirPath)
        val keyFile = Files.list(keyDir)
            .filter { f -> f.toString().endsWith("_sk") || f.toString().endsWith(".pem") }
            .findFirst()
            .orElseThrow { IllegalStateException("No private key file in $keyDirPath") }
        return Identities.readPrivateKey(FileReader(keyFile.toFile()))
    }
}