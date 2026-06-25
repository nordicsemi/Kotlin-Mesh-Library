package no.nordicsemi.android.nrfmesh.feature.model

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.runtime.mcumgr.McuMgrTransport
import io.runtime.mcumgr.ble.McuMgrBleTransport
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
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNodeIdentityGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNodeIdentityStatus
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.LogLevel
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@HiltViewModel(assistedFactory = ModelViewModel.Factory::class)
internal class ModelViewModel @AssistedInject internal constructor(
    private val repository: CoreDataRepository,
    @Assisted("address") address: Int,
    @Assisted("modelId") modelId: Int,
) : ViewModel(), McuMgrTransport.ConnectionCallback {
    private val unicastAddress = address.toUShort()
    private val modelIdentifier = modelId.toUInt()
    private lateinit var meshNetwork: MeshNetwork
    private lateinit var selectedNode: Node
    private lateinit var selectedModel: Model
    private var transport: McuMgrBleTransport? = null

    private val _uiState = MutableStateFlow(ModelScreenUiState())
    val uiState: StateFlow<ModelScreenUiState> = _uiState.asStateFlow()

    init {
        observeNetworkChanges()
    }

    override fun onConnected() {
        repository.logger.log(
            message = { "McuManagerTransport connected" },
            category = LogCategory.MODEL,
            level = LogLevel.APPLICATION
        )
    }

    override fun onDeferred() {
        repository.logger.log(
            message = { "McuManagerTransport deferred" },
            category = LogCategory.MODEL,
            level = LogLevel.APPLICATION
        )
    }

    override fun onError(throwable: Throwable) {
        repository.logger.log(
            message = { "Error in McuManagerTransport in LePairingResponser: ${throwable.message}" },
            category = LogCategory.MODEL,
            level = LogLevel.APPLICATION
        )
    }

    private fun observeNetworkChanges() {
        repository.networkEvents
            .mapNotNull { repository.meshNetwork }
            .onEach { network ->
                selectedModel = network.element(elementAddress = unicastAddress)
                    ?.model(modelId = modelIdentifier)
                    ?: throw IllegalStateException("Model not found")
                selectedNode = selectedModel.parentElement?.parentNode
                    ?: throw IllegalStateException("Node not found")
                _uiState.update { state ->
                    state.copy(
                        modelState = ModelState.Success(
                            model = selectedModel
                        )
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

    internal fun send(message: AcknowledgedConfigMessage) {
        _uiState.value = _uiState.value.copy(messageState = Sending(message = message))
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

    internal fun sendApplicationMessage(model: Model, message: MeshMessage) {
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

    internal suspend fun send(
        model: Model,
        message: AcknowledgedMeshMessage,
    ) = repository.send(model = model, ackedMessage = message)

    internal fun startPairing(context: Context) {
        val identifier = repository.identifier ?: return
        val manager = ContextCompat.getSystemService(
            context,
            BluetoothManager::class.java
        ) as BluetoothManager
        val device = manager.adapter.getRemoteDevice(identifier)
        if (transport == null || transport?.isConnected == false) {
            val transport = McuMgrBleTransport(context, device)
                .also { transport = it }
            transport.connect(this)
        }
    }

    internal fun resetMessageState() {
        _uiState.value = _uiState.value.copy(messageState = NotStarted)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("address") address: Int,
            @Assisted("modelId") modelId: Int,
        ): ModelViewModel
    }
}

internal sealed interface ModelState {

    data object Loading : ModelState

    data class Success(val model: Model) : ModelState

    data class Error(val throwable: Throwable) : ModelState
}

internal data class ModelScreenUiState(
    val modelState: ModelState = ModelState.Loading,
    val messageState: MessageState = NotStarted,
    val isRefreshing: Boolean = false,
    val nodeIdentityStates: List<NodeIdentityStatus> = emptyList(),
    val wasNetworkRemoved: Boolean = false,
)