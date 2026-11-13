package no.nordicsemi.kotlin.mesh.core.layers.foundation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.SecurePropertiesStorage
import no.nordicsemi.kotlin.mesh.core.Storage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationVirtualAddressSet
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.Location
import no.nordicsemi.kotlin.mesh.core.model.MasterSecurity
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Publish
import no.nordicsemi.kotlin.mesh.core.model.PublishPeriod
import no.nordicsemi.kotlin.mesh.core.model.Retransmit
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class ConfigurationClientHandlerPublicationTest {
    private val network: MeshNetwork
    private val localModel: Model
    private val handler: ConfigurationClientHandler
    private val nodeAddress = UnicastAddress(address = 0x0004)
    private val label = VirtualAddress(uuid = Uuid.parse("0073e7e4-d8b9-440f-af84-15df4c56c0e1"))

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
        localModel = manager.localElements.flatMap { it.models }.first { it.isConfigurationClient }
        handler = localModel.eventHandler as ConfigurationClientHandler
    }

    private val targetModel: Model
        get() = checkNotNull(network.node(address = nodeAddress.address))
            .elements.first().models.first()

    private fun status(hash: UShort) = checkNotNull(
        ConfigModelPublicationStatus.init(
            byteArrayOf(0x00) +
                    byteArrayOf(
                        targetModel.parentElement!!.unicastAddress.address.toByte(),
                        (targetModel.parentElement!!.unicastAddress.address.toInt() shr 8).toByte()
                    ) +
                    byteArrayOf(hash.toByte(), (hash.toInt() shr 8).toByte()) +
                    byteArrayOf(0x00, 0x00, 0xFF.toByte(), 0x00, 0x00) +
                    byteArrayOf(
                        targetModel.modelIdentifierBytes()[0],
                        targetModel.modelIdentifierBytes()[1]
                    )
        )
    )

    private fun Model.modelIdentifierBytes(): ByteArray {
        val id = ConfigModelPublicationGet(this).modelIdentifier.toInt()
        return byteArrayOf(id.toByte(), (id shr 8).toByte())
    }

    private suspend fun receive(response: MeshResponse, request: AcknowledgedMeshMessage) =
        handler.handle(
            ModelEvent.ResponseReceived(
                model = localModel,
                response = response,
                request = request,
                source = nodeAddress.address
            )
        )

    @Test
    fun testGetStoresTheLabelOfTheLocalGroupMatchingTheHash() = runBlocking {
        network.add(Group(_name = "Virtual", address = label))
        val hash = label.address

        receive(response = status(hash), request = ConfigModelPublicationGet(targetModel))

        assertEquals(label, targetModel.publish?.address)
    }

    @Test
    fun testGetIgnoresAHashWithoutALocalGroup() = runBlocking {
        receive(
            response = status(hash = 0xA02Bu),
            request = ConfigModelPublicationGet(targetModel)
        )

        assertNull(targetModel.publish)
    }

    @Test
    fun testVirtualSetStoresThePublishOfTheRequest() = runBlocking {
        val publish = Publish(
            address = label,
            index = 0u,
            ttl = 0xFFu,
            period = PublishPeriod.disabled,
            credentials = MasterSecurity,
            retransmit = Retransmit.disabled
        )
        val request = ConfigModelPublicationVirtualAddressSet(publish = publish, model = targetModel)

        receive(response = status(hash = label.address), request = request)

        assertEquals(label, targetModel.publish?.address)
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
