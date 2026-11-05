package no.nordicsemi.android.nrfmesh.feature.nodes.node.scenes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VpnKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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
import no.nordicsemi.android.nrfmesh.core.data.configurator.Messenger
import no.nordicsemi.android.nrfmesh.core.data.configurator.TaskStatus
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppBind
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneDelete
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRecall
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRegisterGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRegisterStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStatus
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

    private val _uiState = MutableStateFlow(NodeScenesUiState())
    val uiState: StateFlow<NodeScenesUiState> = _uiState.asStateFlow()

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
        _uiState.update { state ->
            state.copy(
                scenes = network.scenes
                    .filter { node.primaryUnicastAddress in it.addresses }
                    .map { SceneInfo(name = it.name, number = it.number) },
                canFix = server != null && client != null,
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
        val state = _uiState.value
        if (!state.isSceneServerReady || !state.isSceneClientReady) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val response = request(message = SceneRegisterGet(), model = sceneServer)
            // The Scene Register Status reports no current scene when a transition is in
            // progress, so the Scene Get is sent to find out the target scene.
            if ((response as? SceneRegisterStatus)?.takeIf { it.isSuccess }?.scenes?.isNotEmpty() == true) {
                request(message = SceneGet(), model = sceneServer)
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    /**
     * Recalls the given scene on the node.
     */
    internal fun recall(scene: SceneNumber) {
        val state = _uiState.value
        if (!state.isSceneServerReady || !state.isSceneClientReady) return
        viewModelScope.launch {
            request(message = SceneRecall(sceneNumber = scene), model = sceneServer)
        }
    }

    /**
     * Deletes the given scene from the Scene Register of the node.
     */
    internal fun delete(scene: SceneNumber) {
        val state = _uiState.value
        if (!state.isSceneSetupServerReady || !state.isSceneClientReadyToSetup) return
        viewModelScope.launch {
            request(message = SceneDelete(sceneNumber = scene), model = sceneSetupServer)
        }
    }

    /**
     * Sets up the models required to work with scenes. The first application key of the network is
     * used, or a new one is added if the network has none. The key is added to the node and to the
     * local node if they don't know it and is then bound to the Scene Server and Scene Setup Server
     * on the node and to the Scene Client on the local node.
     */
    internal fun fixClient() {
        val network = repository.meshNetwork ?: return
        val node = network.node(uuid = nodeUuid) ?: return
        val local = localNode ?: return
        val client = localSceneClient ?: return
        viewModelScope.launch {
            try {
                val key = network.applicationKeys.firstOrNull()
                    ?: repository.addApplicationKey(boundNetworkKey = network.networkKeys.first())
                // The tasks are run by a messenger that is not shared with the other screens.
                val messenger = repository.messengers.createDetached()
                listOf(node, local)
                    .filterNot { it.knows(key = key) }
                    .forEach { messenger.enqueue(target = it, message = ConfigAppKeyAdd(key = key)) }
                listOfNotNull(sceneServer, sceneSetupServer)
                    .filterNot { key.isBoundTo(model = it) }
                    .forEach {
                        messenger.enqueue(
                            target = node,
                            message = ConfigModelAppBind(model = it, applicationKey = key)
                        )
                    }
                if (!key.isBoundTo(model = client)) {
                    messenger.enqueue(
                        target = local,
                        message = ConfigModelAppBind(model = client, applicationKey = key)
                    )
                }
                messenger.execute()
                // The messenger stops at the first failed task of a node and reports it.
                messenger.meshTaskFlow.first()
                    .firstNotNullOfOrNull { it.status as? TaskStatus.Error }
                    ?.let { error ->
                        _uiState.update {
                            it.copy(
                                messageState = Failed(
                                    message = SceneRegisterGet(),
                                    error = IllegalStateException(error.error)
                                )
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(messageState = Failed(message = SceneRegisterGet(), error = e))
                }
            }
            repository.meshNetwork?.let { updateState(network = it) }
        }
    }

    private fun Messenger.enqueue(target: Node, message: AcknowledgedConfigMessage) {
        enqueueTask(
            task = ConfigTask(
                icon = Icons.Outlined.VpnKey,
                label = message.toString(),
                message = message,
                target = target
            )
        )
    }

    internal fun onMessageHandled() {
        _uiState.update { it.copy(messageState = NotStarted) }
    }

    /**
     * Sends the given message to the model and updates the current scene from the response.
     *
     * @return the response of the node, or null if the message could not be sent.
     */
    private suspend fun request(message: AcknowledgedMeshMessage, model: Model?): MeshMessage? {
        if (model == null) return null
        _uiState.update { it.copy(messageState = Sending(message = message)) }
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
            _uiState.update {
                it.copy(messageState = Completed(message = message, response = response))
            }
            response
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update { it.copy(messageState = Failed(message = message, error = e)) }
            null
        }
    }

    private fun setCurrentScene(scene: SceneNumber) {
        _uiState.update { it.copy(currentScene = scene.takeIf { s -> Scene.isValid(sceneNumber = s) }) }
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
    val scenes: List<SceneInfo> = emptyList(),
    /**
     * The current scene is not stored in the network database. It has to be read from the node.
     */
    val currentScene: SceneNumber? = null,
    val isSceneServerReady: Boolean = false,
    val isSceneSetupServerReady: Boolean = false,
    val isSceneClientReady: Boolean = false,
    val isSceneClientReadyToSetup: Boolean = false,
    /**
     * The setup can be fixed when the node has a Scene Server and the local node a Scene Client.
     */
    val canFix: Boolean = false,
    val isRefreshing: Boolean = false,
    val messageState: MessageState = NotStarted,
)
