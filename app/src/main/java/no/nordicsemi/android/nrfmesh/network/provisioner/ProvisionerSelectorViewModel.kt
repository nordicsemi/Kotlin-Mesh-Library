package no.nordicsemi.android.nrfmesh.network.provisioner

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
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Provisioner
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi

@HiltViewModel
internal class ProvisionerSelectorViewModel @Inject constructor(
    private val repository: CoreDataRepository,
    private val storage: MeshSecurePropertiesStorage,
) : ViewModel() {
    private lateinit var meshNetwork: MeshNetwork
    private val _uiState = MutableStateFlow(value = ProvisionerSelectorUiState())
    internal val uiState: StateFlow<ProvisionerSelectorUiState> = _uiState.asStateFlow()

    init {
        observeNetworkChanges()
    }

    // Observes the mesh network for any changes i.e. network reset etc.
    private fun observeNetworkChanges() {
        repository.networkEvents
            .map { repository.meshNetwork }
            .filterNotNull()
            .onEach {
                meshNetwork = it
                _uiState.update { state ->
                    state.copy(provisioners = meshNetwork.provisioners)
                }
            }.launchIn(scope = viewModelScope)
    }

    /**
     * Selects the given provisioner.
     *
     * @param provisioner Provisioner to be selected.
     */
    @OptIn(ExperimentalUuidApi::class)
    internal fun onProvisionerSelected(provisioner: Provisioner) {
        viewModelScope.launch {
            meshNetwork.move(provisioner = provisioner, to = 0)
            storage.storeLocalProvisioner(
                uuid = meshNetwork.uuid,
                localProvisionerUuid = provisioner.uuid
            )
            repository.save()
        }
    }
}

internal data class ProvisionerSelectorUiState(val provisioners: List<Provisioner> = listOf())