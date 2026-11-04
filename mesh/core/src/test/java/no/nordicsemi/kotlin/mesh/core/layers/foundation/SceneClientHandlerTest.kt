package no.nordicsemi.kotlin.mesh.core.layers.foundation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.SecurePropertiesStorage
import no.nordicsemi.kotlin.mesh.core.Storage
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRegisterGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRegisterStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStatus
import no.nordicsemi.kotlin.mesh.core.messages.SceneMessageStatus
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.Location
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class SceneClientHandlerTest {
    private val network: MeshNetwork

    // The Scene Client is added to the primary element of the local node by the library.
    private val model: Model
    private val handler: SceneClientHandler

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
        model = manager.localElements.flatMap { it.models }.first { it.isSceneClient }
        handler = model.eventHandler as SceneClientHandler
    }

    private val node = UnicastAddress(address = 0x0100)
    private val otherNode = UnicastAddress(address = 0x0200)

    private suspend fun receive(response: MeshResponse, request: SceneRegisterGet) = handler.handle(
        ModelEvent.ResponseReceived(
            model = model,
            response = response,
            request = request,
            source = node.address
        )
    )

    @Test
    fun testSceneRegisterStatusAddsNodeToScenes() = runBlocking {
        network.add(name = "Existing", number = 0x0010u)

        val result = receive(
            response = SceneRegisterStatus(
                status = SceneMessageStatus.SUCCESS,
                currentScene = 0u,
                scenes = listOf(0x0010u, 0x0011u)
            ),
            request = SceneRegisterGet()
        )

        assertNull(result)
        assertEquals(listOf(node), network.scene(0x0010u)?.addresses)
        // A scene that is not known to the network is added.
        assertEquals(listOf(node), assertNotNull(network.scene(0x0011u)).addresses)
    }

    @Test
    fun testSceneRegisterStatusRemovesNodeFromScenesNotStored() = runBlocking {
        network.add(name = "Stored", number = 0x0010u)
        network.add(name = "Deleted", number = 0x0011u)
        receive(
            response = SceneRegisterStatus(SceneMessageStatus.SUCCESS, 0u, listOf(0x0010u, 0x0011u)),
            request = SceneRegisterGet()
        )

        receive(
            response = SceneRegisterStatus(SceneMessageStatus.SUCCESS, 0u, listOf(0x0010u)),
            request = SceneRegisterGet()
        )

        assertEquals(listOf(node), network.scene(0x0010u)?.addresses)
        assertEquals(emptyList(), network.scene(0x0011u)?.addresses)
    }

    @Test
    fun testSceneRegisterStatusDoesNotAffectOtherNodes() = runBlocking {
        network.add(name = "Shared", number = 0x0010u)
        handler.handle(
            ModelEvent.ResponseReceived(
                model = model,
                response = SceneRegisterStatus(SceneMessageStatus.SUCCESS, 0u, listOf(0x0010u)),
                request = SceneRegisterGet(),
                source = otherNode.address
            )
        )

        receive(
            response = SceneRegisterStatus(SceneMessageStatus.SUCCESS, 0u, emptyList()),
            request = SceneRegisterGet()
        )

        assertEquals(listOf(otherNode), network.scene(0x0010u)?.addresses)
    }

    @Test
    fun testSceneStatusAddsNodeToCurrentAndTargetScenes() = runBlocking {
        handler.handle(
            ModelEvent.ResponseReceived(
                model = model,
                response = SceneStatus(
                    status = SceneMessageStatus.SUCCESS,
                    currentScene = 0x0020u,
                    targetScene = 0x0021u,
                    remainingTime = null
                ),
                request = SceneGet(),
                source = node.address
            )
        )

        assertEquals(listOf(node), assertNotNull(network.scene(0x0020u)).addresses)
        assertEquals(listOf(node), assertNotNull(network.scene(0x0021u)).addresses)
    }

    @Test
    fun testNoSceneIsNotAddedToTheNetwork() = runBlocking {
        val before = network.scenes.size

        handler.handle(
            ModelEvent.ResponseReceived(
                model = model,
                response = SceneStatus(SceneMessageStatus.SUCCESS, currentScene = 0u),
                request = SceneGet(),
                source = node.address
            )
        )

        assertEquals(before, network.scenes.size)
    }

    @Test
    fun testAcknowledgedMessagesAreNotSupported() {
        assertFailsWith<no.nordicsemi.kotlin.mesh.core.ModelError.InvalidMessage> {
            runBlocking {
                handler.handle(
                    ModelEvent.AcknowledgedMessageReceived(
                        model = model,
                        request = SceneGet(),
                        source = node.address,
                        destination = UnicastAddress(address = 0x0001)
                    )
                )
            }
        }
    }
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
    override fun storeLastSeqAuthValue(uuid: Uuid, source: UnicastAddress, lastSeqAuth: ULong) =
        Unit

    override suspend fun previousSeqAuthValue(uuid: Uuid, source: UnicastAddress): ULong? = null
    override fun storePreviousSeqAuthValue(uuid: Uuid, source: UnicastAddress, seqAuth: ULong) =
        Unit

    override suspend fun storeLocalProvisioner(uuid: Uuid, localProvisionerUuid: Uuid) = Unit
    override suspend fun localProvisioner(uuid: Uuid): String? = null
}
