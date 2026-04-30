package no.nordicsemi.android.feature.config.networkkeys.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NodeIdentityStatus
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.common.Sending
import no.nordicsemi.android.nrfmesh.core.common.unknownNetworkKeys
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNodeIdentityGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNodeIdentityStatus
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.Node
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@HiltViewModel(assistedFactory = ConfigNetKeysViewModel.Factory::class)
internal class ConfigNetKeysViewModel @AssistedInject internal constructor(
    private val repository: CoreDataRepository,
    @Assisted uuid: String,
) : ViewModel() {
    private lateinit var meshNetwork: MeshNetwork
    private lateinit var selectedNode: Node
    private val nodeUuid = Uuid.parse(uuidString = uuid)

    private val _uiState = MutableStateFlow(ConfigNetKeysScreenUi())
    val uiState: StateFlow<ConfigNetKeysScreenUi> = _uiState.asStateFlow()

    init {
        observeNetworkChanges()
    }

    private fun observeNetworkChanges() {
        repository.networkEvents
            .map { repository.meshNetwork }
            .filterNotNull()
            .onEach { network ->
                selectedNode = network.node(uuid = nodeUuid) ?: return@onEach
                _uiState.update { state ->
                    state.copy(
                        isLocalProvisionerNode = selectedNode.isLocalProvisioner,
                        addedNetworkKeys = selectedNode.networkKeys.toList(),
                        availableNetworkKeys = selectedNode.unknownNetworkKeys()
                    )
                }
                meshNetwork = network // update the local network instance
            }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Returns if the NodeIdentityState for this should be updated/refreshed.
     *
     * @return true if the NodeIdentityState should be updated, false otherwise.
     */
    private fun shouldUpdateNodeIdentityState(): Boolean =
        _uiState.value.nodeIdentityStates.isEmpty()

    /**
     * Creates a list of NodeIdentityStatus objects for each network key.
     *
     * @return List of NodeIdentityStatus objects.
     */
    private fun createNodeIdentityStates(model: Model) =
        model.parentElement?.parentNode?.networkKeys
            ?.map { key ->
                NodeIdentityStatus(
                    networkKey = key,
                    nodeIdentityState = null
                )
            } ?: emptyList()

    /**
     * Called when the user pulls down to refresh the node details.
     */
    internal fun onRefresh() {
        _uiState.value = uiState.value.copy(isRefreshing = true)
        send(message = ConfigNetKeyGet())
    }

    internal fun send(message: AcknowledgedConfigMessage) {
        _uiState.update {
            it.copy(
                messageState = Sending(message = message),
                isRefreshing = if (message is ConfigNetKeyGet) true else it.isRefreshing
            )
        }
        viewModelScope.launch {
            try {
                repository.send(selectedNode, message)?.let { response ->
                    _uiState.value = _uiState.value.copy(
                        messageState = Completed(
                            message = message,
                            response = response as ConfigResponse
                        ),
                        isRefreshing = false
                    )
                } ?: run {
                    _uiState.value = _uiState.value.copy(
                        messageState = Failed(
                            message = message,
                            error = IllegalStateException("No response received")
                        ),
                        isRefreshing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    messageState = Failed(message = message, error = e),
                    isRefreshing = false
                )
            }
        }
    }

    internal fun send(model: Model, message: MeshMessage) {
        _uiState.value = _uiState.value.copy(messageState = Sending(message = message))
        viewModelScope.launch {
            runCatching {
                if (message is AcknowledgedMeshMessage) {
                    val response = repository.send(model = model, ackedMessage = message)
                    _uiState.value = _uiState.value.copy(
                        messageState = Completed(
                            message = message,
                            response = response as? MeshResponse
                        )
                    )

                } else {
                    repository.send(
                        model = model,
                        unackedMessage = message as UnacknowledgedMeshMessage
                    )
                    _uiState.value =
                        _uiState.value.copy(messageState = Completed(message = message))
                }
            }.getOrElse {
                _uiState.value = _uiState.value.copy(
                    messageState = Failed(message = message, error = it),
                    isRefreshing = false
                )
            }
        }
    }

    internal fun readApplicationKeys() {
        viewModelScope.launch {
            var message: ConfigAppKeyGet? = null
            try {
                selectedNode.networkKeys.forEach {
                    message = ConfigAppKeyGet(networkKeyIndex = it.index)
                    _uiState.value = _uiState.value.copy(messageState = Sending(message = message))
                    repository.send(selectedNode, message)?.let { response ->
                        _uiState.value = _uiState.value.copy(
                            messageState = Completed(
                                message = message,
                                response = response as ConfigResponse
                            ),
                            isRefreshing = false
                        )
                    } ?: run {
                        _uiState.value = _uiState.value.copy(
                            messageState = Failed(
                                message = message,
                                error = IllegalStateException("No response received")
                            ),
                            isRefreshing = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    messageState = Failed(message = message, error = e),
                    isRefreshing = false
                )
            }
        }
    }

    internal fun requestNodeIdentityStates(model: Model) {
        viewModelScope.launch {
            val element = model.parentElement ?: throw IllegalStateException("Element not found")
            if (shouldUpdateNodeIdentityState()) {
                _uiState.value = _uiState.value.copy(
                    nodeIdentityStates = createNodeIdentityStates(model = model)
                )
            }
            val uiState = _uiState.value
            val nodeIdentityStates = uiState.nodeIdentityStates.toMutableList()
            val keys = element.parentNode?.networkKeys ?: emptyList()

            var message: ConfigNodeIdentityGet? = null
            var response: ConfigNodeIdentityStatus? = null
            try {
                keys.forEach { key ->
                    message = ConfigNodeIdentityGet(networkKeyIndex = key.index)
                    _uiState.value = _uiState.value.copy(messageState = Sending(message = message))
                    response = repository.send(
                        node = element.parentNode!!,
                        message = message
                    ) as ConfigNodeIdentityStatus

                    response.let { status ->
                        val index = nodeIdentityStates.indexOfFirst { state ->
                            state.networkKey.index == status.networkKeyIndex
                        }
                        nodeIdentityStates[index] = nodeIdentityStates[index]
                            .copy(nodeIdentityState = status.identity)
                    }
                }
                _uiState.value = _uiState.value.copy(
                    messageState = Completed(
                        message = ConfigNodeIdentityGet(networkKeyIndex = keys.first().index),
                        response = response as ConfigNodeIdentityStatus
                    ),
                    nodeIdentityStates = nodeIdentityStates.toList()
                )
            } catch (ex: Exception) {
                _uiState.value = _uiState.value.copy(
                    messageState = Failed(message = message, error = ex),
                    isRefreshing = false,
                )
            }
        }
    }

    internal fun resetMessageState() {
        _uiState.value = _uiState.value.copy(messageState = NotStarted)
    }

    internal fun addNetworkKey() = repository.addNetworkKey()

    internal fun isKeyInUse(key: NetworkKey) = selectedNode
        .containsApplicationKeyBoundToNetworkKey(key = key)

    fun save() {
        viewModelScope.launch {
            repository.save()
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(uuid: String): ConfigNetKeysViewModel
    }
}

internal data class ConfigNetKeysScreenUi(
    val isRefreshing: Boolean = false,
    val messageState: MessageState = NotStarted,
    val isLocalProvisionerNode: Boolean = false,
    val nodeIdentityStates: List<NodeIdentityStatus> = emptyList(),
    val availableNetworkKeys: List<NetworkKey> = emptyList(),
    val addedNetworkKeys: List<NetworkKey> = emptyList(),
)