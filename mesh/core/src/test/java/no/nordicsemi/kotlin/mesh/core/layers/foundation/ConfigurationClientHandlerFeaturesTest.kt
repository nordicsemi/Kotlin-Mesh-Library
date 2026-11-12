package no.nordicsemi.kotlin.mesh.core.layers.foundation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.SecurePropertiesStorage
import no.nordicsemi.kotlin.mesh.core.Storage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigFriendGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigFriendStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigGattProxyGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigGattProxyStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigRelayGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigRelayStatus
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.FeatureState
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.Location
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.uuid.Uuid

class ConfigurationClientHandlerFeaturesTest {
    private val network: MeshNetwork
    private val model: Model
    private val handler: ConfigurationClientHandler

    // Bedroom Light has Relay and Friend enabled and Proxy unsupported in cdb_json.json.
    private val nodeAddress = UnicastAddress(address = 0x0004)

    init {
        val json = checkNotNull(
            javaClass.classLoader?.getResourceAsStream("cdb_json.json")?.readAllBytes()
        )
        val manager = MeshNetworkManager(
            storage = NoStorage,
            secureProperties = NoSecureProperties,
            ioDispatcher = Dispatchers.Unconfined
        )
        network = runBlocking { manager.import(json) }
        manager.localElements = listOf(Element(location = Location.FIRST))
        // The Configuration Client is added to the primary element of the local node by the library.
        model = manager.localElements.flatMap { it.models }.first { it.isConfigurationClient }
        handler = model.eventHandler as ConfigurationClientHandler
    }

    private fun node() = checkNotNull(network.node(address = nodeAddress.address))

    private suspend fun receive(
        response: MeshResponse,
        request: AcknowledgedMeshMessage,
    ) = handler.handle(
        ModelEvent.ResponseReceived(
            model = model,
            response = response,
            request = request,
            source = nodeAddress.address
        )
    )

    @Test
    fun testRelayStatusReplacesFeatures() = runBlocking {
        val before = node().features
        assertEquals(FeatureState.Enabled, before.relay?.state)

        receive(
            response = ConfigRelayStatus(FeatureState.Disabled, count = 0u, steps = 0u),
            request = ConfigRelayGet()
        )

        val after = node().features
        assertNotSame(before, after)
        assertEquals(FeatureState.Disabled, after.relay?.state)
        // The previous snapshot must not change, so that state holders can detect the change.
        assertEquals(FeatureState.Enabled, before.relay?.state)
    }

    @Test
    fun testProxyStatusReplacesFeatures() = runBlocking {
        val before = node().features
        assertEquals(FeatureState.Unsupported, before.proxy?.state)

        receive(
            response = ConfigGattProxyStatus(FeatureState.Enabled),
            request = ConfigGattProxyGet()
        )

        val after = node().features
        assertNotSame(before, after)
        assertEquals(FeatureState.Enabled, after.proxy?.state)
        assertEquals(FeatureState.Unsupported, before.proxy?.state)
    }

    @Test
    fun testFriendStatusReplacesFeatures() = runBlocking {
        val before = node().features
        assertEquals(FeatureState.Enabled, before.friend?.state)

        receive(
            response = ConfigFriendStatus(FeatureState.Disabled),
            request = ConfigFriendGet()
        )

        val after = node().features
        assertNotSame(before, after)
        assertEquals(FeatureState.Disabled, after.friend?.state)
        assertEquals(FeatureState.Enabled, before.friend?.state)
    }

    private object NoStorage : Storage {
        override suspend fun load() = ByteArray(0)
        override suspend fun save(network: ByteArray) = Unit
    }

    private object NoSecureProperties : SecurePropertiesStorage {
        override suspend fun ivIndex(uuid: Uuid): IvIndex = TODO("Not needed by the tests")
        override suspend fun storeIvIndex(uuid: Uuid, ivIndex: IvIndex) = Unit
        override suspend fun nextSequenceNumber(uuid: Uuid, address: UnicastAddress): UInt = 0u
        override suspend fun storeNextSequenceNumber(
            uuid: Uuid,
            address: UnicastAddress,
            sequenceNumber: UInt,
        ) = Unit

        override suspend fun resetSequenceNumber(uuid: Uuid, address: UnicastAddress) = Unit
        override suspend fun lastSeqAuthValue(uuid: Uuid, source: UnicastAddress): ULong? = null
        override fun storeLastSeqAuthValue(
            uuid: Uuid,
            source: UnicastAddress,
            lastSeqAuth: ULong,
        ) = Unit

        override suspend fun previousSeqAuthValue(uuid: Uuid, source: UnicastAddress): ULong? = null
        override fun storePreviousSeqAuthValue(
            uuid: Uuid,
            source: UnicastAddress,
            seqAuth: ULong,
        ) = Unit

        override suspend fun storeLocalProvisioner(uuid: Uuid, localProvisionerUuid: Uuid) = Unit
        override suspend fun localProvisioner(uuid: Uuid): String? = null
    }
}
