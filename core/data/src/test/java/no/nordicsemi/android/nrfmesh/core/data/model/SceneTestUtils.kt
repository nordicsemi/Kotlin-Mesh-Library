package no.nordicsemi.android.nrfmesh.core.data.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nordicsemi.android.nrfmesh.core.data.storage.SceneRegisterStorage
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.SecurePropertiesStorage
import no.nordicsemi.kotlin.mesh.core.Storage
import no.nordicsemi.kotlin.mesh.core.StoredWithSceneModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.Location
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.uuid.Uuid

/**
 * Scene register kept in memory.
 */
class InMemorySceneRegisterStorage : SceneRegisterStorage {
    private val scenes = mutableMapOf<Uuid, MutableSet<SceneNumber>>()

    override suspend fun readScenes(uuid: Uuid) = scenes[uuid].orEmpty().sorted()

    override fun storeScene(uuid: Uuid, sceneNumber: SceneNumber) {
        scenes.getOrPut(uuid) { mutableSetOf() }.add(sceneNumber)
    }

    override fun deleteScene(uuid: Uuid, sceneNumber: SceneNumber) {
        scenes[uuid]?.remove(sceneNumber)
    }
}

/**
 * Model that is stored with a scene, which records the calls it receives from the Scene Server.
 */
class RecordingStoredWithSceneHandler : StoredWithSceneModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf()
    override val isSubscriptionSupported = false
    override val publicationMessageComposer: MessageComposer? = null

    val stored = mutableListOf<SceneNumber>()
    val recalled = mutableListOf<Triple<SceneNumber, TransitionTime?, UByte?>>()

    override fun store(scene: SceneNumber) {
        stored.add(scene)
    }

    override fun recall(scene: SceneNumber, transitionTime: TransitionTime?, delay: UByte?) {
        recalled.add(Triple(scene, transitionTime, delay))
    }

    override suspend fun handle(event: ModelEvent): MeshResponse? = null
}

/**
 * Local node with a Scene Server, a Scene Setup Server and a model that is stored with a scene,
 * set up in a network parsed from a Mesh Configuration Database JSON.
 */
class SceneTestNode(
    val storage: InMemorySceneRegisterStorage = InMemorySceneRegisterStorage(),
) {
    val sceneServer = SceneServer(
        ioDispatcher = Dispatchers.Unconfined,
        storage = storage,
        defaultTransitionTimeServer = GenericDefaultTransitionTimeServer()
    )
    val sceneSetupServer = SceneSetupServer(server = sceneServer)
    val storedWithScene = RecordingStoredWithSceneHandler()

    private val sceneServerModel = Model(
        modelId = SigModelId(modelIdentifier = Model.SCENE_SERVER_MODEL_ID),
        handler = sceneServer
    )
    private val sceneSetupServerModel = Model(
        modelId = SigModelId(modelIdentifier = Model.SCENE_SETUP_SERVER_MODEL_ID),
        handler = sceneSetupServer
    )
    private val sceneAwareModel = Model(
        modelId = SigModelId(modelIdentifier = Model.GENERIC_ON_OFF_SERVER_MODEL_ID),
        handler = storedWithScene
    )

    /**
     * UUID of the network the node was set up in.
     */
    val networkUuid: Uuid

    init {
        val json = checkNotNull(
            javaClass.classLoader?.getResourceAsStream("cdb_json.json")?.readAllBytes()
        )
        val manager = MeshNetworkManager(
            storage = NoStorage,
            secureProperties = NoSecureProperties,
            ioDispatcher = Dispatchers.Unconfined
        )
        networkUuid = runBlocking { manager.import(json) }.uuid
        manager.localElements = listOf(
            Element(
                _name = "Primary Element",
                location = Location.FIRST,
                _models = mutableListOf(sceneServerModel, sceneSetupServerModel, sceneAwareModel)
            )
        )
    }

    /**
     * Sends an acknowledged message to the Scene Server.
     */
    suspend fun sendToSceneServer(message: AcknowledgedMeshMessage) = sceneServer.handle(
        ModelEvent.AcknowledgedMessageReceived(
            model = sceneServerModel,
            request = message,
            source = SOURCE.address,
            destination = DESTINATION
        )
    )

    /**
     * Sends an unacknowledged message to the Scene Server.
     */
    suspend fun sendToSceneServer(message: UnacknowledgedMeshMessage) = sceneServer.handle(
        ModelEvent.UnacknowledgedMessageReceived(
            model = sceneServerModel,
            message = message,
            source = SOURCE.address,
            destination = DESTINATION
        )
    )

    /**
     * Sends an acknowledged message to the Scene Setup Server.
     */
    suspend fun sendToSceneSetupServer(message: AcknowledgedMeshMessage) =
        sceneSetupServer.handle(
            ModelEvent.AcknowledgedMessageReceived(
                model = sceneSetupServerModel,
                request = message,
                source = SOURCE.address,
                destination = DESTINATION
            )
        )

    /**
     * Sends an unacknowledged message to the Scene Setup Server.
     */
    suspend fun sendToSceneSetupServer(message: UnacknowledgedMeshMessage) =
        sceneSetupServer.handle(
            ModelEvent.UnacknowledgedMessageReceived(
                model = sceneSetupServerModel,
                message = message,
                source = SOURCE.address,
                destination = DESTINATION
            )
        )

    private companion object {
        val SOURCE = UnicastAddress(address = 0x0100)
        val DESTINATION = UnicastAddress(address = 0x0001)
    }
}

internal object NoStorage : Storage {
    override suspend fun load() = ByteArray(0)
    override suspend fun save(network: ByteArray) = Unit
}

internal object NoSecureProperties : SecurePropertiesStorage {
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
