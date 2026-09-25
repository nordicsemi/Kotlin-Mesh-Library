package no.nordicsemi.android.nrfmesh.feature.nodes.node

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NodeIdentityStatus
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.common.Sending
import no.nordicsemi.android.nrfmesh.core.common.unknownApplicationKeys
import no.nordicsemi.android.nrfmesh.core.common.unknownNetworkKeys
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.NetworkConnectionState
import no.nordicsemi.android.nrfmesh.core.data.configurator.MeshTask
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigCompositionDataGet
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.Node
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@HiltViewModel(assistedFactory = NodeViewModel.Factory::class)
internal class NodeViewModel @AssistedInject internal constructor(
    private val repository: CoreDataRepository,
    @Assisted uuid: String,
) : ViewModel() {
    private lateinit var meshNetwork: MeshNetwork
    private lateinit var selectedNode: Node
    private val nodeUuid = Uuid.parse(uuidString = uuid)
    private val messenger = repository.messengers.messenger(uuid = nodeUuid)

    private val _uiState = MutableStateFlow(NodeScreenUiState())
    val uiState: StateFlow<NodeScreenUiState> = _uiState.asStateFlow()

    init {
        observeNetworkChanges()
        observeMessenger()
        executeTasks()
    }

    override fun onCleared() {
        messenger?.clear()
    }

    private fun observeNetworkChanges() {
        repository.networkEvents
            .mapNotNull { repository.meshNetwork }
            .onEach {
                val nodeState = it.node(uuid = nodeUuid)?.let { node ->
                    this@NodeViewModel.selectedNode = node
                    NodeState.Success(
                        node = node,
                        nodeInfoListData = NodeInfoListData(node = node)
                    )
                } ?: NodeState.Error(Throwable("Node not found"))
                _uiState.update { state ->
                    state.copy(
                        nodeState = nodeState,
                        availableNetworkKeys = selectedNode.unknownNetworkKeys(),
                        availableAppKeys = selectedNode.unknownApplicationKeys()
                    )
                }
                meshNetwork = it // update the local network instance
            }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Observes messenger to handle incoming messages from the repository.
     */
    private fun observeMessenger() {
        messenger?.meshTaskFlow
            ?.onEach { tasks -> _uiState.update { it.copy(tasks = tasks.toList()) } }
            ?.launchIn(scope = viewModelScope)
    }

    /**
     * Requests the composition data for the selected node when the network is connected.
     */
    private fun executeTasks() {
        repository.proxyConnectionStateFlow
            .onEach {
                if (it.connectionState is NetworkConnectionState.Connected) {
                    // Add a small delay to ensure proxy filter is set up before sending the message.
                    if (!selectedNode.isCompositionDataReceived) {
                        delay(timeMillis = 1000)
                        messenger?.execute(meshNetwork = meshNetwork, newNode = selectedNode)
                    }
                }
            }
            .launchIn(scope = viewModelScope)
    }

    internal fun onReconfigCompletePressed() {
        messenger?.clear()
    }

    internal fun onCancelPressed() {
        messenger?.clear()
    }

    internal fun onRetryPressed() {
        messenger?.retry(node = selectedNode)
    }

    /**
     * Called when the user pulls down to refresh the node details.
     */
    internal fun onRefresh() {
        _uiState.value = uiState.value.copy(isRefreshing = true)
        send(message = ConfigCompositionDataGet(page = 0x00u))
    }

    /**
     * Called when the user clicks on the excluded elements.
     *
     * @param exclude True to exclude the node, false to not exclude from the network.
     */
    internal fun onExcluded(exclude: Boolean) {
        selectedNode.excluded = exclude
        repository.save()
    }

    internal fun onItemSelected(item: ClickableNodeInfoItem) {
        _uiState.update { it.copy(selectedNodeInfoItem = item) }
    }

    internal fun send(message: AcknowledgedConfigMessage) {
        _uiState.update { it.copy(messageState = Sending(message = message)) }
        viewModelScope.launch {
            try {
                repository.send(selectedNode, message)?.let { response ->
                    _uiState.update {
                        it.copy(
                            messageState = Completed(
                                message = message,
                                response = response as ConfigResponse
                            ),
                            isRefreshing = false
                        )
                    }
                } ?: run {
                    _uiState.update {
                        it.copy(
                            messageState = Failed(
                                message = message,
                                error = IllegalStateException("No response received")
                            ),
                            isRefreshing = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        messageState = Failed(message = message, error = e),
                        isRefreshing = false
                    )
                }
            }
        }
    }

    internal fun removeNode() {
        meshNetwork.remove(node = selectedNode)
        save()
    }

    fun save() {
        repository.save()
    }

    @AssistedFactory
    interface Factory {
        fun create(uuid: String): NodeViewModel
    }
}

internal sealed interface NodeState {

    data object Loading : NodeState

    data class Success(
        val node: Node,
        val nodeInfoListData: NodeInfoListData,
    ) : NodeState

    data class Error(val throwable: Throwable) : NodeState
}

internal data class NodeScreenUiState(
    val nodeState: NodeState = NodeState.Loading,
    val isRefreshing: Boolean = false,
    val messageState: MessageState = NotStarted,
    val selectedNodeInfoItem: ClickableNodeInfoItem? = null,
    val nodeIdentityStates: List<NodeIdentityStatus> = emptyList(),
    val availableNetworkKeys: List<NetworkKey> = emptyList(),
    val availableAppKeys: List<ApplicationKey> = emptyList(),
    val tasks: List<MeshTask> = emptyList(),
    val wasNetworkRemoved: Boolean = false,
)