package org.fabric.api.config

import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import mu.KotlinLogging
import org.hyperledger.fabric.client.Gateway
import org.hyperledger.fabric.client.identity.Identities
import org.hyperledger.fabric.client.identity.Signers
import org.hyperledger.fabric.client.identity.X509Identity
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Path
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Configuration
class FabricGatewayConfig(private val props: FabricProperties) {

    @Bean
    fun grpcChannel(): ManagedChannel {
        log.info { "Connecting to Fabric peer at ${props.peer.endpoint}" }
        val tlsCertPath = Path.of(props.peer.tlsCertPath)
        val sslContext = GrpcSslContexts.forClient()
            .trustManager(tlsCertPath.toFile())
            .build()

        return NettyChannelBuilder
            .forTarget(props.peer.endpoint)
            .sslContext(sslContext)
            .build()
    }

    @Bean
    fun fabricGateway(grpcChannel: ManagedChannel): Gateway {
        val certificate: X509Certificate = Identities.readX509Certificate(FileReader(props.gateway.certPath))
        val privateKey: PrivateKey = readPrivateKey(props.gateway.keyPath)

        val identity = X509Identity(props.mspId, certificate)
        val signer = Signers.newPrivateKeySigner(privateKey)

        return Gateway.newInstance()
            .identity(identity)
            .signer(signer)
            .connection(grpcChannel)
            .evaluateOptions { it.withDeadlineAfter(5, TimeUnit.SECONDS) }
            .endorseOptions { it.withDeadlineAfter(15, TimeUnit.SECONDS) }
            .submitOptions { it.withDeadlineAfter(5, TimeUnit.SECONDS) }
            .commitStatusOptions { it.withDeadlineAfter(60, TimeUnit.SECONDS) }
            .connect()
    }

    private fun readPrivateKey(keyDirPath: String): PrivateKey {
        val keyDir = Path.of(keyDirPath)
        val keyFile = Files.list(keyDir)
            .filter { it.toString().endsWith("_sk") || it.toString().endsWith(".pem") }
            .findFirst()
            .orElseThrow { IllegalStateException("No private key file found in $keyDirPath") }
        return Identities.readPrivateKey(FileReader(keyFile.toFile()))
    }
}
