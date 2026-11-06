package no.nordicsemi.android.nrfmesh.feature.nodes.node.scenes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.VpnKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.common.Sending
import no.nordicsemi.android.nrfmesh.core.common.isSceneServer
import no.nordicsemi.android.nrfmesh.core.common.isSceneSetupServer
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.configurator.ConfigTask
import no.nordicsemi.android.nrfmesh.core.data.configurator.MeshTask
import no.nordicsemi.android.nrfmesh.core.data.configurator.Messenger
import no.nordicsemi.android.nrfmesh.core.data.name
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppBind
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppUnbind
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneDelete
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRecall
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRegisterGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRegisterStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStore
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.Scene
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber
import kotlin.uuid.Uuid

/**
 * View model of the screen listing the scenes stored on a node.
 *
 * To send messages to the Scene Server and the Scene Setup Server models on the node, both of them
 * need to have an application key bound. The status messages are processed by the Scene Client
 * model on the local node, which has to be bound to the same key. Otherwise, the scenes of the
 * network are not updated.
 */
@HiltViewModel(assistedFactory = NodeScenesViewModel.Factory::class)
internal class NodeScenesViewModel @AssistedInject internal constructor(
    private val repository: CoreDataRepository,
    @Assisted uuid: String,
) : ViewModel() {
    private val nodeUuid = Uuid.parse(uuidString = uuid)
    private var sceneServer: Model? = null
    private var sceneSetupServer: Model? = null
    private var localSceneClient: Model? = null
    private var localNode: Node? = null
    private var fixMessenger: Messenger? = null
    private var fixJob: Job? = null
    private var tasksJob: Job? = null

    val uiState: StateFlow<NodeScenesUiState>
        field = MutableStateFlow(NodeScenesUiState())

    init {
        repository.networkEvents
            .map { repository.meshNetwork }
            .filterNotNull()
            .onEach { updateState(network = it) }
            .launchIn(scope = viewModelScope)
    }

    private fun updateState(network: MeshNetwork) {
        val node = network.node(uuid = nodeUuid) ?: return
        val models = node.primaryElement.models
        val server = models.firstOrNull { it.isSceneServer() }
        val setupServer = models.firstOrNull { it.isSceneSetupServer() }
        val local = network.localProvisioner?.node
        val client = local?.primaryElement?.models?.firstOrNull { it.isSceneClient }
        sceneServer = server
        sceneSetupServer = setupServer
        localSceneClient = client
        localNode = local

        // Messages are sent using the first application key bound to the model.
        val serverKey = server?.boundApplicationKeys?.firstOrNull()
        val setupServerKey = setupServer?.boundApplicationKeys?.firstOrNull()
        val clientKeys = client?.boundApplicationKeys.orEmpty()
        val isSetUp = serverKey != null && clientKeys.any { it.index == serverKey.index } &&
                (setupServer == null || (setupServerKey != null &&
                        clientKeys.any { it.index == setupServerKey.index }))
        uiState.update { state ->
            state.copy(
                nodeName = node.name,
                localNodeName = local?.name.orEmpty(),
                networkScenes = network.scenes,
                scenes = network.scenes
                    .filter { node.primaryUnicastAddress in it.addresses }
                    .map { SceneInfo(name = it.name, number = it.number) },
                // Set up is available until the Scene Server and the Scene Client share a key and the
                // Scene Setup Server is bound to the key of the Scene Client.
                canFix = server != null && client != null && !isSetUp,
                canUniqueKeySetup = server != null && setupServer != null && client != null &&
                        !isSetUp,
                isSceneServerReady = serverKey != null,
                isSceneSetupServerReady = setupServerKey != null,
                // The client is ready, when it is bound to the key used by the server.
                isSceneClientReady = clientKeys.isNotEmpty() &&
                        (serverKey == null || clientKeys.any { it.index == serverKey.index }),
                isSceneClientReadyToSetup = clientKeys.isNotEmpty() &&
                        (setupServerKey == null || clientKeys.any { it.index == setupServerKey.index })
            )
        }
    }

    /**
     * Reads the Scene Register and the current scene from the node.
     */
    internal fun onRefresh() {
        val state = uiState.value
        if (!state.isSceneServerReady || !state.isSceneClientReady) return
        viewModelScope.launch {
            uiState.update { it.copy(isRefreshing = true) }
            val response = request(message = SceneRegisterGet(), model = sceneServer)
            // The Scene Register Status reports no current scene when a transition is in
            // progress, so the Scene Get is sent to find out the target scene.
            if ((response as? SceneRegisterStatus)?.takeIf { it.isSuccess }?.scenes?.isNotEmpty() == true) {
                request(message = SceneGet(), model = sceneServer)
            }
            uiState.update { it.copy(isRefreshing = false) }
        }
    }

    /**
     * Recalls the given scene on the node.
     */
    internal fun recall(scene: SceneNumber) {
        val state = uiState.value
        if (!state.isSceneServerReady || !state.isSceneClientReady) return
        viewModelScope.launch {
            request(message = SceneRecall(sceneNumber = scene), model = sceneServer)
        }
    }

    /**
     * Stores the current state of the node as the given scene in the Scene Register of the node.
     */
    internal fun store(scene: SceneNumber) {
        val state = uiState.value
        if (!state.isSceneSetupServerReady || !state.isSceneClientReadyToSetup) return
        viewModelScope.launch {
            request(message = SceneStore(sceneNumber = scene), model = sceneSetupServer)
        }
    }

    /**
     * Deletes the given scene from the Scene Register of the node.
     */
    internal fun delete(scene: SceneNumber) {
        val state = uiState.value
        if (!state.isSceneSetupServerReady || !state.isSceneClientReadyToSetup) return
        viewModelScope.launch {
            request(message = SceneDelete(sceneNumber = scene), model = sceneSetupServer)
        }
    }

    /**
     * Sets up the Scene Server and the Scene Setup Server on the node and the Scene Client on the
     * local node so that they can communicate.
     *
     * The Scene Server and the Scene Client have to be bound to the same application key. If one of
     * them is already bound to a key, the other one is bound to that key. The Scene Setup Server is
     * bound to the same key.
     *
     * The progress is published as [NodeScenesUiState.tasks].
     */
    internal fun fixClient() = runSetup { network, node, local, server, client ->
        SetupPlan().apply {
            val key = pairServerAndClient(network, node, local, server, client)
            sceneSetupServer?.let { bind(target = node, model = it, key = key) }
        }.tasks()
    }

    /**
     * Binds a unique application key to the Scene Setup Server on the node and to the Scene Client
     * on the local node, so that the Scene Server and the Scene Setup Server don't share a key.
     *
     * The setup starts with the Scene Server and the Scene Client, which are bound to the same key
     * as in [fixClient]. The Scene Setup Server is then compared to it and is bound to another
     * application key, which is the one it is already bound to, a key of the Scene Client that the
     * Scene Server doesn't use, the next application key of the network or a new one. The Scene
     * Client is bound to both of the keys. If the Scene Setup Server and the Scene Server are bound
     * to the other's key, the key is unbound.
     *
     * The progress is published as [NodeScenesUiState.tasks].
     */
    internal fun uniqueKeySetup() = runSetup { network, node, local, server, client ->
        val setupServer = sceneSetupServer ?: return@runSetup emptyList()
        SetupPlan().apply {
            val serverKey = pairServerAndClient(network, node, local, server, client)
            val setupKey = setupServer.boundApplicationKeys
                .firstOrNull { it.index != serverKey.index }
                // A key of the Scene Client that is not used by the Scene Server.
                ?: client.boundApplicationKeys.firstOrNull { it.index != serverKey.index }
                ?: network.applicationKeys.firstOrNull { it.index != serverKey.index }
                ?: repository.addApplicationKey(boundNetworkKey = serverKey.boundNetworkKey)
            bind(target = node, model = setupServer, key = setupKey)
            bind(target = local, model = client, key = setupKey)
            // The keys are not reused between the two models.
            unbind(target = node, model = setupServer, key = serverKey)
            unbind(target = node, model = server, key = setupKey)
        }.tasks()
    }

    /**
     * Binds the Scene Server on the node and the Scene Client on the local node to the same
     * application key and returns it. If one of them is already bound to a key, the other one is
     * bound to that key. The key of the Scene Server is preferred, as the status messages are
     * encrypted with it. If none of them is bound to a key, the first application key of the
     * network is used, or a new one is added if the network has none.
     */
    private fun SetupPlan.pairServerAndClient(
        network: MeshNetwork,
        node: Node,
        local: Node,
        server: Model,
        client: Model,
    ): ApplicationKey {
        val key = server.boundApplicationKeys.firstOrNull()
            ?: client.boundApplicationKeys.firstOrNull()
            ?: network.applicationKeys.firstOrNull()
            ?: repository.addApplicationKey(boundNetworkKey = network.networkKeys.first())
        bind(target = node, model = server, key = key)
        bind(target = local, model = client, key = key)
        return key
    }

    /**
     * Plans the tasks of the setup, adding the keys to the nodes that don't know them first, then
     * binding and finally unbinding the keys.
     */
    private class SetupPlan {
        private val added = mutableSetOf<Pair<Node, ApplicationKey>>()
        private val adds = mutableListOf<ConfigTask>()
        private val binds = mutableListOf<ConfigTask>()
        private val unbinds = mutableListOf<ConfigTask>()

        fun bind(target: Node, model: Model, key: ApplicationKey) {
            if (key.isBoundTo(model = model)) return
            if (!target.knows(key = key) && added.add(target to key)) {
                adds += ConfigTask(
                    icon = Icons.Outlined.VpnKey,
                    label = "Adding ${key.name} to ${target.name}",
                    message = ConfigAppKeyAdd(key = key),
                    target = target
                )
            }
            binds += ConfigTask(
                icon = Icons.Outlined.AddLink,
                label = "Binding ${key.name} to ${model.name()}",
                message = ConfigModelAppBind(model = model, applicationKey = key),
                target = target
            )
        }

        fun unbind(target: Node, model: Model, key: ApplicationKey) {
            if (!key.isBoundTo(model = model)) return
            unbinds += ConfigTask(
                icon = Icons.Outlined.LinkOff,
                label = "Unbinding ${key.name} from ${model.name()}",
                message = ConfigModelAppUnbind(model = model, applicationKey = key),
                target = target
            )
        }

        fun tasks() = adds + binds + unbinds
    }

    /**
     * Runs the tasks planned by the given function on a messenger that is not shared with the other
     * screens.
     */
    private fun runSetup(
        plan: suspend (MeshNetwork, Node, Node, Model, Model) -> List<ConfigTask>,
    ) {
        val network = repository.meshNetwork ?: return
        val node = network.node(uuid = nodeUuid) ?: return
        val local = localNode ?: return
        val server = sceneServer ?: return
        val client = localSceneClient ?: return
        fixJob?.cancel()
        fixJob = viewModelScope.launch {
            try {
                val tasks = plan(network, node, local, server, client)
                val messenger = repository.messengers.createDetached()
                fixMessenger = messenger
                tasks.forEach { messenger.enqueueTask(task = it) }
                tasksJob?.cancel()
                tasksJob = messenger.meshTaskFlow
                    .onEach { tasks -> uiState.update { it.copy(tasks = tasks.toList()) } }
                    .launchIn(scope = viewModelScope)
                messenger.execute()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState.update {
                    it.copy(messageState = Failed(message = SceneRegisterGet(), error = e))
                }
            }
            repository.meshNetwork?.let { updateState(network = it) }
        }
    }

    /**
     * Retries the tasks of the setup.
     */
    internal fun onRetryPressed() {
        val messenger = fixMessenger ?: return
        fixJob?.cancel()
        fixJob = viewModelScope.launch {
            messenger.retry()
            repository.meshNetwork?.let { updateState(network = it) }
        }
    }

    /**
     * Closes the progress of the setup, cancelling the tasks that are still running.
     */
    internal fun onTasksDismissed() {
        fixJob?.cancel()
        tasksJob?.cancel()
        fixMessenger?.clear()
        fixMessenger = null
        uiState.update { it.copy(tasks = emptyList()) }
        repository.meshNetwork?.let { updateState(network = it) }
    }

    internal fun onMessageHandled() {
        uiState.update { it.copy(messageState = NotStarted) }
    }

    /**
     * Sends the given message to the model and updates the current scene from the response.
     *
     * @return the response of the node, or null if the message could not be sent.
     */
    private suspend fun request(message: AcknowledgedMeshMessage, model: Model?): MeshMessage? {
        if (model == null) return null
        uiState.update { it.copy(messageState = Sending(message = message)) }
        return try {
            val response = repository.send(model = model, ackedMessage = message)
            // A status that reports an error is shown in a dialog by the screen, so the current
            // scene is updated only when the node reports success.
            when {
                response is SceneStatus && response.isSuccess ->
                    // While a transition is in progress, the target scene is reported.
                    setCurrentScene(response.targetScene ?: response.currentScene)

                response is SceneRegisterStatus && response.isSuccess ->
                    setCurrentScene(response.currentScene)
            }
            uiState.update {
                it.copy(messageState = Completed(message = message, response = response))
            }
            response
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            uiState.update { it.copy(messageState = Failed(message = message, error = e)) }
            null
        }
    }

    private fun setCurrentScene(scene: SceneNumber) {
        uiState.update { it.copy(currentScene = scene.takeIf { s -> Scene.isValid(sceneNumber = s) }) }
    }

    @AssistedFactory
    interface Factory {
        fun create(uuid: String): NodeScenesViewModel
    }
}

/**
 * A scene stored on the node.
 */
internal data class SceneInfo(val name: String, val number: SceneNumber)

internal data class NodeScenesUiState(
    /**
     * Name of the node and of the local node, which are shown when the setup is required.
     */
    val nodeName: String = "",
    val localNodeName: String = "",
    val scenes: List<SceneInfo> = emptyList(),
    /**
     * All the scenes of the network, which can be stored on the node.
     */
    val networkScenes: List<Scene> = emptyList(),
    /**
     * The current scene is not stored in the network database. It has to be read from the node.
     */
    val currentScene: SceneNumber? = null,
    val isSceneServerReady: Boolean = false,
    val isSceneSetupServerReady: Boolean = false,
    val isSceneClientReady: Boolean = false,
    val isSceneClientReadyToSetup: Boolean = false,
    /**
     * The setup is needed when the Scene Server on the node and the Scene Client on the local node
     * are not bound to the same application key.
     */
    val canFix: Boolean = false,
    /**
     * A unique key can be bound to the Scene Setup Server when the setup is needed.
     */
    val canUniqueKeySetup: Boolean = false,
    /**
     * Tasks of the setup of the scene models.
     */
    val tasks: List<MeshTask> = emptyList(),
    val isRefreshing: Boolean = false,
    val messageState: MessageState = NotStarted,
)
