package no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner

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
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.models.ProvisionerData
import no.nordicsemi.android.nrfmesh.core.data.storage.MeshSecurePropertiesStorage
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Provisioner
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@HiltViewModel(assistedFactory = ProvisionerViewModel.Factory::class)
internal class ProvisionerViewModel
@AssistedInject internal constructor(
    private val repository: CoreDataRepository,
    private val storage: MeshSecurePropertiesStorage,
    @Assisted uuid: Uuid,
) : ViewModel() {
    private val provisionerUuid = uuid//Uuid.parse(uuidString = uuid)
    private lateinit var network: MeshNetwork
    private lateinit var provisioner: Provisioner
    private val _uiState = MutableStateFlow(ProvisionerScreenUiState())
    internal val uiState: StateFlow<ProvisionerScreenUiState> = _uiState.asStateFlow()

    init {
        observeNetwork()
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun observeNetwork() {
        repository.networkEvents
            .map { repository.meshNetwork }
            .filterNotNull()
            .onEach {
                network = it
                val provisionerState = network
                    .provisioner(uuid = provisionerUuid)
                    ?.let { provisioner ->
                        this.provisioner = provisioner
                        ProvisionerState.Success(
                            provisioner = provisioner,
                            provisionerData = ProvisionerData(provisioner = provisioner)
                        )
                    } ?: ProvisionerState.Error(throwable = IllegalStateException("Provisioner not found."))
                _uiState.update { state ->
                    state.copy(
                        provisionerState = provisionerState,
                        index = network.provisioners.indexOf(provisioner)
                    )
                }
            }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Moves the provisioner to a new index in the list.
     */
    @OptIn(ExperimentalUuidApi::class)
    fun moveProvisioner(provisioner: Provisioner, newIndex: Int) {
        viewModelScope.launch {
            network.move(provisioner = provisioner, to = newIndex)
            storage.storeLocalProvisioner(
                uuid = network.uuid,
                localProvisionerUuid = provisioner.uuid
            )
            repository.save()
        }
    }

    /**
     * Saves the network.
     */
    internal fun save() {
        repository.save()
    }

    @AssistedFactory
    interface Factory {
        @OptIn(ExperimentalUuidApi::class)
        fun create(uuid: Uuid): ProvisionerViewModel
    }
}

internal sealed interface ProvisionerState {

    data object Loading : ProvisionerState

    data class Success(
        val provisioner: Provisioner,
        val provisionerData: ProvisionerData,
    ) : ProvisionerState

    data class Error(val throwable: Throwable) : ProvisionerState
}

@ConsistentCopyVisibility
internal data class ProvisionerScreenUiState internal constructor(
    val provisionerState: ProvisionerState = ProvisionerState.Loading,
    val index: Int = -1,
)
