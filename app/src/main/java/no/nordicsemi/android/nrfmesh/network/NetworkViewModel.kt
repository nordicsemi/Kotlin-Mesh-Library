package no.nordicsemi.android.nrfmesh.network

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.storage.MeshSecurePropertiesStorage
import no.nordicsemi.android.nrfmesh.network.util.ImportState
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppBind
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val repository: CoreDataRepository,
    private val storage: MeshSecurePropertiesStorage,
) : ViewModel() {
    private lateinit var meshNetwork: MeshNetwork
    private val _uiState = MutableStateFlow(value = NetworkScreenUiState())
    internal val uiState: StateFlow<NetworkScreenUiState> = _uiState.asStateFlow()

    init {
        observeNetworkChanges()
        loadNetwork()
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }

    // Observes the mesh network for any changes i.e. network reset etc.
    private fun observeNetworkChanges() {
        repository.networkEvents
            .map {
                if(repository.meshNetwork == null) {
                    _uiState.update { state ->
                        state.copy(networkState = MeshNetworkState.NoNetwork)
                    }
                }
                repository.meshNetwork
            }
            .filterNotNull()
            .onEach {
                meshNetwork = it
                _uiState.update { state ->
                    state.copy(
                        networkState = MeshNetworkState.Success(network = it),
                        id = state.id + 1
                    )
                }
                promptProvisionerSelection(meshNetwork = it)
            }.launchIn(scope = viewModelScope)
    }

    internal fun loadNetwork() {
        viewModelScope.launch {
            // Check if a network can be loaded and if not update the state
            if (!repository.load()) {
                _uiState.update { state ->
                    state.copy(networkState = MeshNetworkState.NoNetwork)
                }
            } else {
                repository.onBluetoothEnabled()
            }
        }
    }

    /**
     * Imports a network from a given Uri.
     *
     * @param uri                  URI of the file.
     * @param contentResolver      Content resolver.
     */
    @OptIn(ExperimentalUuidApi::class)
    internal fun importNetwork(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(importState = ImportState.Importing) }
                repository.importNetwork(uri = uri, contentResolver = contentResolver)
                _uiState.update { it.copy(importState = ImportState.Completed()) }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(importState = ImportState.Completed(error = Error(it)))
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun promptProvisionerSelection(meshNetwork: MeshNetwork) {
        if (!meshNetwork.restoreLocalProvisioner(storage = storage)) {
            meshNetwork
                .takeIf { it.provisioners.size > 1 }
                ?.let { _uiState.value = _uiState.value.copy(shouldSelectProvisioner = true) }
                ?: run {
                    storage.storeLocalProvisioner(
                        uuid = meshNetwork.uuid,
                        localProvisionerUuid = meshNetwork.provisioners.first().uuid
                    )
                }
        }
    }

    internal fun resetNetwork() {
        viewModelScope.launch { repository.resetNetwork() }
    }

    internal fun resetMeshNetworkUiState() {
        _uiState.update {
            it.copy(networkState = MeshNetworkState.Loading)
        }
    }

    internal fun onImportErrorAcknowledged() {
        _uiState.update { it.copy(importState = ImportState.Unknown) }
    }

    /**
     * This method sends a [HealthAttentionSetUnacknowledged] message to the health server model
     * of the given node to start the attention timer for 3 seconds.
     *
     * If the model is not bound to any App Key, a first found on that node will be bound to it.
     *
     * If the node does not have any App Keys, a key with Key Index 4095 will be created
     * (if such does not exist) and bound to the node.
     *
     * This new key ensures that no unintended key will be sent to this device just to make it blink.
     *
     * @param node The target node.
     */
    internal fun startAttentionTimer(node: Node) = node.primaryElement
        .model(SigModelId(Model.HEALTH_SERVER_MODEL_ID))
        ?.let { healthServerModel ->
            viewModelScope.launch {
                // Is there any App Key bound to the Health Server model?
                if (healthServerModel.boundApplicationKeys.isEmpty()) {
                    // Does the node know any App Key?
                    var firstAppKey = node.applicationKeys.firstOrNull()
                    if (firstAppKey == null) {
                        // Usually, the keys are numbered from 0, so it's unlikely that 4095 exists.
                        val keyIndex = 4095.toUShort()
                        // Is there already a key with index 4095?
                        firstAppKey = meshNetwork
                            .applicationKeys
                            .firstOrNull { it.index == keyIndex }
                        if (firstAppKey == null) {
                            // Create such key.
                            firstAppKey = meshNetwork.add(
                                "Node Identification Key",
                                index = keyIndex,
                                boundNetworkKey = node.networkKeys.first()
                            )
                        }
                        // Send it to the node before binding.
                        val _ = repository.send(node, ConfigAppKeyAdd(firstAppKey))
                    }
                    // Bind the key. Here it is guaranteed, that the key is known to the node.
                    val _ = repository.send(
                        node, ConfigModelAppBind(
                            healthServerModel, firstAppKey
                        )
                    )
                }
                // Finally, start the attention timer for 3 seconds.
                repository.send(
                    healthServerModel,
                    HealthAttentionSetUnacknowledged(attentionTimer = 3u)
                )
            }
        }
}

internal sealed interface MeshNetworkState {
    data object Loading : MeshNetworkState
    data class Success(val network: MeshNetwork) : MeshNetworkState
    data object NoNetwork : MeshNetworkState
}

internal data class NetworkScreenUiState(
    val networkState: MeshNetworkState = MeshNetworkState.Loading,
    val shouldSelectProvisioner: Boolean = false,
    internal val id: Int = 0,
    val importState: ImportState = ImportState.Unknown,
)