package org.fabric.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.hyperledger.fabric.client.*;
import org.hyperledger.fabric.client.identity.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Fabric Gateway SDK v1.x client application.
 *
 * Connects to peer0.org1, submits and evaluates transactions
 * against the asset-transfer chaincode on mychannel.
 */
public final class App {

    // ─── Config ───────────────────────────────────────────────────────────────
    private static final String MSP_ID        = "Org1MSP";
    private static final String CHANNEL_NAME  = "mychannel";
    private static final String CHAINCODE_NAME = "asset-transfer";

    // Crypto paths (adjust if crypto-config output differs)
    private static final Path CRYPTO_PATH =
            Paths.get("..", "organizations", "org1");
    private static final Path CERT_DIR_PATH =
            CRYPTO_PATH.resolve(Paths.get("users", "User1@org1.example.com", "msp", "signcerts"));
    private static final Path KEY_DIR_PATH =
            CRYPTO_PATH.resolve(Paths.get("users", "User1@org1.example.com", "msp", "keystore"));
    private static final Path TLS_CERT_PATH =
            CRYPTO_PATH.resolve(Paths.get("peers", "peer0", "tls", "ca.crt"));

    private static final String PEER_ENDPOINT     = "localhost:7051";
    private static final String OVERRIDE_AUTH     = "peer0.org1.example.com";

    private final Contract contract;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // ─── Bootstrap ────────────────────────────────────────────────────────────

    public static void main(final String[] args) throws Exception {
        // gRPC channel to the peer
        ManagedChannel grpcChannel = newGrpcConnection();

        Gateway.Builder builder = Gateway.newInstance()
                .identity(newIdentity())
                .signer(newSigner())
                .connection(grpcChannel)
                .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));

        try (Gateway gateway = builder.connect()) {
            new App(gateway).run();
        } finally {
            grpcChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public App(final Gateway gateway) {
        Network network = gateway.getNetwork(CHANNEL_NAME);
        this.contract = network.getContract(CHAINCODE_NAME);
    }

    // ─── Demo workflow ────────────────────────────────────────────────────────

    private void run() throws GatewayException, CommitException {
        System.out.println("─────────────────────────────────────────");
        System.out.println("  Asset Transfer Java Client");
        System.out.println("─────────────────────────────────────────");

        initLedger();
        getAllAssets();
        createAsset();
        transferAssetAsync();
        getAllAssets();
    }

    private void initLedger() throws EndorseException, SubmitException,
            CommitStatusException, CommitException {
        System.out.println("\n→ InitLedger");
        contract.submitTransaction("InitLedger");
        System.out.println("  Ledger initialized with sample assets.");
    }

    private void getAllAssets() throws GatewayException {
        System.out.println("\n→ GetAllAssets");
        byte[] result = contract.evaluateTransaction("GetAllAssets");
        System.out.println("  Result: " + prettyJson(result));
    }

    private void createAsset() throws EndorseException, SubmitException,
            CommitStatusException, CommitException {
        String assetId = "asset" + Instant.now().toEpochMilli();
        System.out.println("\n→ CreateAsset: " + assetId);
        contract.submitTransaction(
                "CreateAsset", assetId, "yellow", "5", "Tom", "1300");
        System.out.println("  Created asset: " + assetId);
    }

    private void transferAssetAsync() throws EndorseException, SubmitException,
            CommitStatusException, CommitException {
        System.out.println("\n→ TransferAsset (async): asset1 → Saptha");

        SubmittedTransaction commit = contract.newProposal("TransferAsset")
                .addArguments("asset1", "Saptha")
                .build()
                .endorse()
                .submitAsync();

        byte[] result = commit.getResult();
        System.out.println("  Old owner: " + new String(result, StandardCharsets.UTF_8));

        System.out.println("  Waiting for commit…");
        Status status = commit.getStatus();
        if (!status.isSuccessful()) {
            throw new RuntimeException("Transaction " + status.getTransactionId()
                    + " failed with status code " + status.getCode());
        }
        System.out.println("  Transaction committed in block " + status.getBlockNumber());
    }

    // ─── Identity helpers ─────────────────────────────────────────────────────

    private static ManagedChannel newGrpcConnection() throws IOException, CertificateException {
        var credentials = GrpcSslContexts.forClient()
                .trustManager(TLS_CERT_PATH.toFile())
                .build();
        return NettyChannelBuilder.forTarget(PEER_ENDPOINT)
                .sslContext(credentials)
                .overrideAuthority(OVERRIDE_AUTH)
                .build();
    }

    private static Identity newIdentity() throws IOException, CertificateException {
        try (var certReader = Files.newBufferedReader(getFirstFilePath(CERT_DIR_PATH))) {
            var certificate = Identities.readX509Certificate(certReader);
            return new X509Identity(MSP_ID, certificate);
        }
    }

    private static Signer newSigner() throws IOException, InvalidKeyException {
        try (var keyReader = Files.newBufferedReader(getFirstFilePath(KEY_DIR_PATH))) {
            var privateKey = Identities.readPrivateKey(keyReader);
            return Signers.newPrivateKeySigner(privateKey);
        }
    }

    private static Path getFirstFilePath(final Path dir) throws IOException {
        try (var keyFiles = Files.list(dir)) {
            return keyFiles.findFirst()
                    .orElseThrow(() -> new IOException("No file found in: " + dir));
        }
    }

    private String prettyJson(final byte[] json) {
        return gson.toJson(JsonParser.parseString(new String(json, StandardCharsets.UTF_8)));
    }
}
