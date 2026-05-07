package no.nordicsemi.android.nrfmesh.feature.ivindex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.storage.MeshSecurePropertiesStorage
import no.nordicsemi.kotlin.mesh.core.exception.NoNetwork
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

@HiltViewModel(assistedFactory = IvIndexViewModel.Factory::class)
class IvIndexViewModel @AssistedInject constructor(
    private val repository: CoreDataRepository,
    private val storage: MeshSecurePropertiesStorage,
) : ViewModel() {
    private lateinit var network: MeshNetwork
    private val _uiState = MutableStateFlow(IvIndexScreenUiState())
    internal val uiState: StateFlow<IvIndexScreenUiState> = _uiState.asStateFlow()

    init {
        observeNetworkState()
    }

    /**
     * Observes the network state and updates the UI state with the current IV index.
     */
    private fun observeNetworkState() {
        repository.networkEvents
            .map { repository.meshNetwork }
            .filterNotNull()
            .onEach {
                network = it
                _uiState.value = IvIndexScreenUiState(
                    ivIndex = network.ivIndex,
                    testMode = repository.ivUpdateTestMode,
                    isIvIndexChangeAllowed = network.isIvIndexUpdateAllowed()
                )
            }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Increases the IV index by 1 if the network is empty.
     */
    @OptIn(ExperimentalUuidApi::class)
    internal fun onIvIndexChanged(index: UInt, isIvUpdateActive: Boolean) {
        runCatching {
            network.setIvIndex(index = index, isIvUpdateActive = isIvUpdateActive)
            viewModelScope.launch {
                storage.storeIvIndex(uuid = network.uuid, ivIndex = network.ivIndex)
            }
        }.onSuccess {
            _uiState.value = _uiState.value.copy(ivIndex = network.ivIndex)
        }
    }

    /**
     * Toggles the test mode for IV update.
     */
    internal fun toggleIvUpdateTestMode(flag: Boolean) {
        repository.toggleIvUpdateTestMode(flag = flag)
        _uiState.value = _uiState.value.copy(testMode = flag)
    }

    @AssistedFactory
    internal interface Factory {
        fun create(): IvIndexViewModel
    }
}

@OptIn(ExperimentalTime::class)
internal data class IvIndexScreenUiState(
    val isIvIndexChangeAllowed: Boolean = false,
    val ivIndex: IvIndex = IvIndex(),
    val testMode: Boolean = false,
)