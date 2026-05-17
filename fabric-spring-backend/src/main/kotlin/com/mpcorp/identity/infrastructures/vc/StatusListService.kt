package com.mpcorp.identity.infrastructures.vc

import com.fasterxml.jackson.databind.ObjectMapper
import com.mpcorp.identity.infrastructures.fabric.FabricLedgerBridge
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.withLock

/**
 * StatusListService — W3C Status List 2021 implementation.
 *
 * Spec: https://www.w3.org/TR/vc-status-list/
 *
 * ## Flow
 *
 * 1. Issuer issues a VC with `credentialStatus.statusListIndex = <i>`
 *    and `statusListCredential = /api/v1/status-list/{listId}`.
 * 2. To revoke: flip bit `i` to 1, gzip + base64url the bitstring,
 *    re-sign the Status List VC envelope, push the new encoded list on-chain.
 * 3. Verifier fetches the Status List VC, decodes the bitstring,
 *    checks bit `i`. 0 = active, 1 = revoked.
 *
 * ## Storage
 *
 * - On-chain (Fabric): full encoded list + size + last updated index.
 *   This preserves immutability and gives an audit history (`GetRecordHistory`
 *   could be added later to walk versions).
 * - Off-chain (this service): an in-memory cache of decoded bits, rebuilt
 *   lazily from the ledger to avoid hammering Fabric on every revoke.
 *
 * Capacity: 131_072 bits (16 KiB) per list — enough for the demo and well
 * below the 16K minimum recommended by the spec.
 *
 * The signing scheme follows VcIssuerService (HMAC-SHA256). Production should
 * migrate to Ed25519 / ECDSA P-256.
 */
@Service
class StatusListService(
    private val objectMapper: ObjectMapper,
    private val fabricBridge: FabricLedgerBridge,
    @Value("\${vc.secret}") private val vcSecret: String,
    @Value("\${vc.status-list.id:employment-status-list-1}") private val defaultListId: String,
    @Value("\${vc.status-list.size:131072}") private val listSize: Int,
    @Value("\${vc.status-list.base-url:http://localhost:8080/api/v1/status-list}") private val baseUrl: String,
    @Value("\${vc.issuer-did:did:fabric:trustid:org1}") private val issuerDid: String,
) {

    private val log = LoggerFactory.getLogger(StatusListService::class.java)

    /** In-memory bitmap cache per listId, hydrated lazily from Fabric. */
    private val bitmaps = ConcurrentHashMap<String, ByteArray>()
    private val locks   = ConcurrentHashMap<String, ReentrantLock>()

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns the configured default listId for newly-issued VCs. */
    fun defaultListId(): String = defaultListId

    /**
     * Build the `credentialStatus` block embedded into every VC at issuance.
     * The Verifier follows `statusListCredential` to fetch the bitstring and
     * checks `statusListIndex` against it.
     */
    fun buildCredentialStatus(statusListIndex: Long, listId: String = defaultListId): Map<String, Any> {
        return mapOf(
            "id"                   to "$baseUrl/$listId#$statusListIndex",
            "type"                 to "StatusList2021Entry",
            "statusPurpose"        to "revocation",
            "statusListIndex"      to statusListIndex.toString(),
            "statusListCredential" to "$baseUrl/$listId",
        )
    }

    /**
     * Mark a single entry as revoked (bit = 1) on the given list. Recomputes
     * the encoded bitstring and pushes the new snapshot to Fabric.
     */
    fun revoke(statusListIndex: Long, listId: String = defaultListId, revokedBy: String = "system") {
        toggle(statusListIndex, listId, revoked = true, updatedBy = revokedBy)
    }

    /**
     * Reactivate an entry (bit = 0) — used when issuing a fresh VC for the
     * same index (e.g. ensures a newly-approved account starts ACTIVE even
     * if the list already had a higher-water mark).
     */
    fun activate(statusListIndex: Long, listId: String = defaultListId, updatedBy: String = "system") {
        toggle(statusListIndex, listId, revoked = false, updatedBy = updatedBy)
    }

    /**
     * Quick lookup: is the bit at [statusListIndex] currently set (revoked)?
     * Used by VC verification path.
     */
    fun isRevoked(statusListIndex: Long, listId: String = defaultListId): Boolean {
        val bits = loadBitmap(listId)
        return getBit(bits, statusListIndex)
    }

    /**
     * Returns the signed Status List 2021 VC JSON for this listId.
     * Verifiers GET this at `/api/v1/status-list/{listId}`.
     */
    fun getStatusListCredential(listId: String = defaultListId): String {
        val bits        = loadBitmap(listId)
        val encodedList = encodeBitstring(bits)
        return buildStatusListCredential(listId, encodedList).also {
            log.debug("[StatusList] Served list={} size={} bits", listId, listSize)
        }
    }

    // ── Internal: bitmap operations ───────────────────────────────────────────

    private fun toggle(statusListIndex: Long, listId: String, revoked: Boolean, updatedBy: String) {
        require(statusListIndex in 0 until listSize) {
            "statusListIndex $statusListIndex out of range [0, $listSize)"
        }
        val lock = locks.computeIfAbsent(listId) { ReentrantLock() }
        lock.withLock {
            val bits = loadBitmap(listId)
            val current = getBit(bits, statusListIndex)
            if (current == revoked) {
                log.debug("[StatusList] noop — list={} index={} already revoked={}", listId, statusListIndex, revoked)
                return
            }
            setBit(bits, statusListIndex, revoked)
            bitmaps[listId] = bits

            val encoded = encodeBitstring(bits)
            fabricBridge.updateStatusListEntry(
                listId       = listId,
                encodedList  = encoded,
                size         = listSize.toLong(),
                updatedIndex = statusListIndex,
                revoked      = revoked,
                updatedBy    = updatedBy,
            )
            log.info("[StatusList] list={} index={} revoked={} updatedBy={}", listId, statusListIndex, revoked, updatedBy)
        }
    }

    /**
     * Load the bitmap for [listId] — from the in-memory cache if present,
     * otherwise rehydrate from Fabric, otherwise start fresh.
     */
    private fun loadBitmap(listId: String): ByteArray {
        bitmaps[listId]?.let { return it }
        val lock = locks.computeIfAbsent(listId) { ReentrantLock() }
        return lock.withLock {
            bitmaps[listId]?.let { return@withLock it }
            val record = fabricBridge.getStatusList(listId)
            val bytes = if (record != null) {
                try {
                    decodeBitstring(record.encodedList, listSize)
                } catch (e: Exception) {
                    log.warn("[StatusList] failed to decode on-chain bitstring, starting fresh — {}", e.message)
                    ByteArray(listSize / 8)
                }
            } else {
                ByteArray(listSize / 8)
            }
            bitmaps[listId] = bytes
            bytes
        }
    }

    private fun getBit(bits: ByteArray, index: Long): Boolean {
        val byteIdx = (index ushr 3).toInt()
        val bitIdx  = (index and 7L).toInt()
        return (bits[byteIdx].toInt() and (1 shl (7 - bitIdx))) != 0
    }

    private fun setBit(bits: ByteArray, index: Long, value: Boolean) {
        val byteIdx = (index ushr 3).toInt()
        val bitIdx  = (index and 7L).toInt()
        val mask    = (1 shl (7 - bitIdx)).toByte()
        bits[byteIdx] = if (value) {
            (bits[byteIdx].toInt() or mask.toInt()).toByte()
        } else {
            (bits[byteIdx].toInt() and mask.toInt().inv()).toByte()
        }
    }

    // ── Encoding: gzip + base64url ────────────────────────────────────────────

    private fun encodeBitstring(bits: ByteArray): String {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(bits) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
    }

    private fun decodeBitstring(encoded: String, expectedSize: Int): ByteArray {
        val gzipped = Base64.getUrlDecoder().decode(encoded)
        val out = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(gzipped)).use { it.copyTo(out) }
        val decoded = out.toByteArray()
        val expectedBytes = expectedSize / 8
        return when {
            decoded.size == expectedBytes -> decoded
            decoded.size <  expectedBytes -> decoded + ByteArray(expectedBytes - decoded.size)
            else                          -> decoded.copyOfRange(0, expectedBytes)
        }
    }

    // ── Status List VC envelope ───────────────────────────────────────────────

    /**
     * Wrap the encoded bitstring in a signed W3C Verifiable Credential of
     * type `StatusList2021Credential`. Verifiers receive this VC verbatim.
     */
    private fun buildStatusListCredential(listId: String, encodedList: String): String {
        val now = Instant.now()
        val credentialSubject = mapOf(
            "id"            to "$baseUrl/$listId#list",
            "type"          to "StatusList2021",
            "statusPurpose" to "revocation",
            "encodedList"   to encodedList,
        )
        val vcBody = mapOf(
            "@context" to listOf(
                "https://www.w3.org/2018/credentials/v1",
                "https://w3id.org/vc/status-list/2021/v1",
            ),
            "id"                to "$baseUrl/$listId",
            "type"              to listOf("VerifiableCredential", "StatusList2021Credential"),
            "issuer"            to issuerDid,
            "issuanceDate"      to now.toString(),
            "credentialSubject" to credentialSubject,
        )
        val vcBodyJson = objectMapper.writeValueAsString(vcBody)
        val proofValue = hmacSha256(vcBodyJson)
        val vcWithProof = vcBody + mapOf(
            "proof" to mapOf(
                "type"               to "HMAC-SHA256",
                "created"            to now.toString(),
                "verificationMethod" to "$issuerDid#key-1",
                "proofValue"         to proofValue,
            )
        )
        return objectMapper.writeValueAsString(vcWithProof)
    }

    private fun hmacSha256(data: String): String {
        val key = SecretKeySpec(vcSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256").apply { init(key) }
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
