package no.nordicsemi.android.nrfmesh.feature.export

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.Provisioner
import no.nordicsemi.kotlin.mesh.core.model.serialization.config.DeviceKeyConfig
import no.nordicsemi.kotlin.mesh.core.model.serialization.config.NetworkConfiguration
import no.nordicsemi.kotlin.mesh.core.model.serialization.config.NetworkKeysConfig
import no.nordicsemi.kotlin.mesh.core.model.serialization.config.NodesConfig
import no.nordicsemi.kotlin.mesh.core.model.serialization.config.ProvisionersConfig
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi

@HiltViewModel
class ExportViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
) : ViewModel() {
    private lateinit var meshNetwork: MeshNetwork
    private val _uiState = MutableStateFlow(ExportScreenUiState())
    val uiState: StateFlow<ExportScreenUiState> = _uiState.asStateFlow()

    init {
        observeNetwork()
    }

    private fun observeNetwork() {
        repository.networkEvents
            .map { repository.meshNetwork }
            .filterNotNull()
            .onEach { meshNetwork ->
                this.meshNetwork = meshNetwork
                _uiState.update { state ->
                    state.copy(
                        networkName = meshNetwork.name,
                        provisionerItemStates = meshNetwork.provisioners
                            .map { ProvisionerItemState(provisioner = it)
                        },
                        networkKeyItemStates = meshNetwork.networkKeys
                            .map { NetworkKeyItemState(it) }
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Invoked when export option is toggled.
     *
     * @param option Selected export option
     */
    internal fun onExportOptionSelected(option: ExportOption) {
        _uiState.update { state ->
            state.copy(exportOption = option)
        }
    }

    /**
     * Invoked when a Provisioner is selected/unselected to be exported.
     *
     * @param provisioner     Provisioner.
     * @param selected        True if selected or false otherwise.
     */
    @OptIn(ExperimentalUuidApi::class)
    internal fun onProvisionerSelected(provisioner: Provisioner, selected: Boolean) {
        _uiState.update { state ->
            state.copy(
                provisionerItemStates = state.provisionerItemStates.map {
                    if (it.provisioner.uuid == provisioner.uuid)
                        it.copy(isSelected = selected)
                    else it
                }
            )
        }
    }

    /**
     * Invoked when the network keys to exported are selected.
     *
     * @param key          Network key.
     * @param selected     True if selected or false otherwise.
     */
    internal fun onNetworkKeySelected(key: NetworkKey, selected: Boolean) {
        _uiState.update { state ->
            state.copy(
                networkKeyItemStates = state.networkKeyItemStates.map {
                    if (it.networkKey.index == key.index)
                        it.copy(isSelected = selected)
                    else it
                }
            )
        }
    }

    /**
     * Updates the ui state when device keys check box is toggled.
     *
     * @param isToggled True if toggled and false otherwise.
     */
    internal fun onExportDeviceKeysToggled(isToggled: Boolean) {
        _uiState.update { state ->
            state.copy(exportDeviceKeys = isToggled)
        }
    }

    /**
     * Invoked when the current export state is displayed to the user.
     */
    internal fun onExportStateDisplayed() {
        _uiState.update { state ->
            state.copy(exportState = ExportState.Unknown)
        }
    }

    /**
     * Exports the mesh network based on the selected configuration.
     * @param contentResolver Content resolver
     * @param uri             Uri of the file.
     */
    fun export(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiState.update { state ->
                state.copy(exportState = ExportState.Error(throwable))
            }
        }) {
            val network = meshNetwork
            uiState.value.run {
                val data = repository.exportNetwork(
                    configuration = when (exportOption) {
                        ExportOption.ALL -> NetworkConfiguration.Full
                        ExportOption.PARTIAL -> createPartialConfiguration(
                            network = network,
                            networkKeyItemStates = networkKeyItemStates,
                            provisionerItemStates = provisionerItemStates,
                            exportDeviceKeys = exportDeviceKeys
                        )
                    }
                )

                contentResolver.openOutputStream(uri)?.run {
                    write(data)
                    close()
                }
            }
            _uiState.update { state ->
                state.copy(exportState = ExportState.Success)
            }
        }
    }

    /**
     * Creates a partial network configuration for a given mesh network.
     *
     * @param network                     Mesh network to be exported.
     * @param networkKeyItemStates        Network keys UI state.
     * @param provisionerItemStates       Provisioners UI state.
     * @param exportDeviceKeys            Specifies if device keys should be exported.
     */
    private fun createPartialConfiguration(
        network: MeshNetwork,
        networkKeyItemStates: List<NetworkKeyItemState>,
        provisionerItemStates: List<ProvisionerItemState>,
        exportDeviceKeys: Boolean,
    ) = NetworkConfiguration.Partial(
        networkKeysConfig = networkKeyConfiguration(
            network = network,
            selectedNetworkKeys = networkKeyItemStates.filter { it.isSelected }
        ),
        provisionersConfig = provisionerConfiguration(
            network = network,
            selectedProvisioners = provisionerItemStates.filter { it.isSelected }
        ),
        nodesConfig = NodesConfig.All(
            deviceKeyConfig = when (exportDeviceKeys) {
                true -> DeviceKeyConfig.INCLUDE_KEY
                false -> DeviceKeyConfig.EXCLUDE_KEY
            }
        )
    )

    /**
     * Creates the provisioner configuration for a given mesh network based on user selection.
     *
     * @param network                Mesh network to be exported.
     * @param selectedProvisioners   Provisioners to be exported.
     */
    private fun provisionerConfiguration(
        network: MeshNetwork,
        selectedProvisioners: List<ProvisionerItemState>,
    ): ProvisionersConfig = when (selectedProvisioners.size == network.provisioners.size) {
        true -> ProvisionersConfig.All
        false -> ProvisionersConfig.Some(selectedProvisioners.map { it.provisioner })
    }

    /**
     * Creates the network configuration for a given mesh network based on user selection.
     *
     * @param network                Mesh network to be exported.
     * @param selectedNetworkKeys    Network keys that are selected to be exported.
     */
    private fun networkKeyConfiguration(
        network: MeshNetwork,
        selectedNetworkKeys: List<NetworkKeyItemState>,
    ): NetworkKeysConfig = when (selectedNetworkKeys.size == network.networkKeys.size) {
        true -> NetworkKeysConfig.All
        false -> NetworkKeysConfig.Some(selectedNetworkKeys.map { it.networkKey })
    }
}

sealed interface ExportState {
    data object Success : ExportState
    data class Error(val throwable: Throwable) : ExportState
    data object Unknown : ExportState
}

@ConsistentCopyVisibility
data class ExportScreenUiState internal constructor(
    val exportState: ExportState = ExportState.Unknown,
    val exportOption: ExportOption = ExportOption.ALL,
    val networkName: String = "Mesh Network",
    val provisionerItemStates: List<ProvisionerItemState> = listOf(),
    val networkKeyItemStates: List<NetworkKeyItemState> = listOf(),
    val exportDeviceKeys: Boolean = true,
)

@ConsistentCopyVisibility
data class ProvisionerItemState internal constructor(
    val provisioner: Provisioner,
    val isSelected: Boolean = false,
)

@ConsistentCopyVisibility
data class NetworkKeyItemState internal constructor(
    val networkKey: NetworkKey,
    val isSelected: Boolean = false,
)