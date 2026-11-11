package no.nordicsemi.android.nrfmesh.feature.config.applicationkeys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.common.Sending
import no.nordicsemi.android.nrfmesh.core.common.unknownApplicationKeys
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyGet
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import kotlin.uuid.Uuid

@HiltViewModel(assistedFactory = ConfigAppKeysViewModel.Factory::class)
internal class ConfigAppKeysViewModel @AssistedInject internal constructor(
    private val repository: CoreDataRepository,
    @Assisted uuid: String,
) : ViewModel() {
    private lateinit var meshNetwork: MeshNetwork
    private lateinit var selectedNode: Node
    private val nodeUuid = Uuid.parse(uuidString = uuid)

    val uiState: StateFlow<ConfigAppKeysUiState>
        field = MutableStateFlow(ConfigAppKeysUiState())

    init {
        observeNetworkChanges()
    }

    private fun observeNetworkChanges() {
        repository.networkEvents
            .mapNotNull { repository.meshNetwork }
            .onEach { network ->
                this@ConfigAppKeysViewModel.selectedNode =
                    network.node(uuid = nodeUuid) ?: return@onEach
                uiState.update { state ->
                    state.copy(
                        isLocalProvisionerNode = selectedNode.isLocalProvisioner,
                        addedAppKeys = selectedNode.applicationKeys.toList(),
                        availableAppKeys = selectedNode.unknownApplicationKeys()
                    )
                }
                meshNetwork = network // update the local network instance
            }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Called when the user pulls down to refresh the node details.
     */
    internal fun onRefresh() {
        uiState.update { it.copy(isRefreshing = true) }
        readApplicationKeys()
    }

    internal fun send(message: AcknowledgedConfigMessage) {
        uiState.update { it.copy(messageState = Sending(message = message)) }
        viewModelScope.launch {
            try {
                repository.send(selectedNode, message)?.let { response ->
                    uiState.update {
                        it.copy(
                            messageState = Completed(
                                message = message,
                                response = response as ConfigResponse
                            ),
                            isRefreshing = false
                        )
                    }
                } ?: run {
                    uiState.update {
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
                uiState.update {
                    it.copy(
                        messageState = Failed(message = message, error = e),
                        isRefreshing = false
                    )
                }
            }
        }
    }

    internal fun send(model: Model, message: MeshMessage) {
        uiState.update { it.copy(messageState = Sending(message = message)) }
        viewModelScope.launch {
            runCatching {
                if (message is AcknowledgedMeshMessage) {
                    val response = repository.send(model = model, ackedMessage = message)
                    uiState.update {
                        it.copy(
                            messageState = Completed(
                                message = message,
                                response = response as? MeshResponse
                            )
                        )
                    }

                } else {
                    repository.send(
                        model = model,
                        unackedMessage = message as UnacknowledgedMeshMessage
                    )
                    uiState.update { it.copy(messageState = Completed(message = message)) }
                }
            }.getOrElse {
                uiState.update { state ->
                    state.copy(
                        messageState = Failed(message = message, error = it),
                        isRefreshing = false
                    )
                }
            }
        }
    }

    internal fun readApplicationKeys() {
        viewModelScope.launch {
            var message: ConfigAppKeyGet? = null
            try {
                selectedNode.networkKeys.forEach {
                    message = ConfigAppKeyGet(networkKeyIndex = it.index)
                    uiState.update { it.copy(messageState = Sending(message = message)) }
                    repository.send(selectedNode, message)?.let { response ->
                        uiState.update {
                            it.copy(
                                messageState = Completed(
                                    message = message,
                                    response = response as ConfigResponse
                                ),
                                isRefreshing = false
                            )
                        }
                    } ?: run {
                        uiState.update {
                            it.copy(
                                messageState = Failed(
                                    message = message,
                                    error = IllegalStateException("No response received")
                                ),
                                isRefreshing = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                uiState.update {
                    it.copy(
                        messageState = Failed(message = message, error = e),
                        isRefreshing = false
                    )
                }
            }
        }
    }

    internal fun isKeyInUse(key: ApplicationKey) = selectedNode
        .containsModelsBoundToApplicationKey(key = key)

    internal fun resetMessageState() {
        uiState.update { it.copy(messageState = NotStarted) }
    }

    internal fun addApplicationKey() = viewModelScope.launch {
        val _ = repository.addApplicationKey(
            boundNetworkKey = meshNetwork.networkKeys.first()
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(uuid: String): ConfigAppKeysViewModel
    }
}

internal data class ConfigAppKeysUiState(
    val isRefreshing: Boolean = false,
    val messageState: MessageState = NotStarted,
    val isLocalProvisionerNode: Boolean = false,
    val availableAppKeys: List<ApplicationKey> = emptyList(),
    val addedAppKeys: List<ApplicationKey> = emptyList(),
)