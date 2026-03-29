package org.fabric.api.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.fabric.api.websocket.FabricEventPublisher
import io.mockk.*
import org.fabric.api.config.FabricProperties
import org.fabric.api.exception.AssetAlreadyExistsException
import org.fabric.api.exception.AssetNotFoundException
import org.fabric.api.model.Asset
import org.hyperledger.fabric.client.Contract
import org.hyperledger.fabric.client.Gateway
import org.hyperledger.fabric.client.Network
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssetServiceTest {

    private val gateway = mockk<Gateway>()
    private val network = mockk<Network>()
    private val contract = mockk<Contract>()
    private val eventPublisher = mockk<FabricEventPublisher>(relaxed = true)
    private val props = FabricProperties(channelName = "mychannel", chaincodeName = "asset-transfer")
    private val objectMapper = ObjectMapper().registerKotlinModule()

    private lateinit var service: AssetService

    private val sampleAsset = Asset("asset1", "blue", 5, "Tomoko", 300)
    private val sampleJson = """{"ID":"asset1","Color":"blue","Size":5,"Owner":"Tomoko","AppraisedValue":300}"""

    @BeforeEach
    fun setUp() {
        every { gateway.getNetwork("mychannel") } returns network
        every { network.getContract("asset-transfer") } returns contract
        service = AssetService(gateway, props, objectMapper, eventPublisher)
    }

    @Test
    fun `getAllAssets returns list of assets`() {
        val json = """[{"ID":"asset1","Color":"blue","Size":5,"Owner":"Tomoko","AppraisedValue":300}]"""
        every { contract.evaluateTransaction("GetAllAssets") } returns json.toByteArray()

        val result = service.getAllAssets()

        assertEquals(1, result.size)
        assertEquals("asset1", result[0].id)
    }

    @Test
    fun `getAsset returns asset when found`() {
        every { contract.evaluateTransaction("ReadAsset", "asset1") } returns sampleJson.toByteArray()

        val result = service.getAsset("asset1")

        assertEquals(sampleAsset, result)
    }

    @Test
    fun `getAsset throws AssetNotFoundException when not found`() {
        every { contract.evaluateTransaction("ReadAsset", "missing") } throws
                RuntimeException("Asset 'missing' does not exist")

        assertThrows<AssetNotFoundException> { service.getAsset("missing") }
    }

    @Test
    fun `assetExists returns true when asset found`() {
        every { contract.evaluateTransaction("AssetExists", "asset1") } returns "true".toByteArray()

        assertTrue(service.assetExists("asset1"))
    }

    @Test
    fun `assetExists returns false when not found`() {
        every { contract.evaluateTransaction("AssetExists", "missing") } returns "false".toByteArray()

        assertFalse(service.assetExists("missing"))
    }

    @Test
    fun `createAsset throws AlreadyExistsException when asset exists`() {
        every { contract.evaluateTransaction("AssetExists", "asset1") } returns "true".toByteArray()

        assertThrows<AssetAlreadyExistsException> {
            service.createAsset("asset1", "blue", 5, "Alice", 500)
        }
    }

    @Test
    fun `createAsset creates and returns new asset`() {
        every { contract.evaluateTransaction("AssetExists", "asset7") } returns "false".toByteArray()
        every { contract.submitTransaction("CreateAsset", "asset7", "purple", "10", "Alice", "900") } returns ByteArray(0)
        every { contract.evaluateTransaction("ReadAsset", "asset7") } returns
                """{"ID":"asset7","Color":"purple","Size":10,"Owner":"Alice","AppraisedValue":900}""".toByteArray()

        val result = service.createAsset("asset7", "purple", 10, "Alice", 900)

        assertEquals("asset7", result.id)
        assertEquals("Alice", result.owner)
    }

    @Test
    fun `deleteAsset throws NotFoundException when missing`() {
        every { contract.evaluateTransaction("AssetExists", "ghost") } returns "false".toByteArray()

        assertThrows<AssetNotFoundException> { service.deleteAsset("ghost") }
    }

    @Test
    fun `transferAsset returns previous owner`() {
        every { contract.evaluateTransaction("AssetExists", "asset1") } returns "true".toByteArray()
        every { contract.submitTransaction("TransferAsset", "asset1", "Bob") } returns "Tomoko".toByteArray()

        val prevOwner = service.transferAsset("asset1", "Bob")

        assertEquals("Tomoko", prevOwner)
    }
}
