package no.nordicsemi.android.nrfmesh.feature.application.keys.key

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
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey

@HiltViewModel(assistedFactory = ApplicationKeyViewModel.Factory::class)
internal class ApplicationKeyViewModel @AssistedInject internal constructor(
    private val repository: CoreDataRepository,
    @Assisted index: Int,
) : ViewModel() {
    private val keyIndex = index.toUShort()
    private lateinit var network: MeshNetwork

    private val _uiState = MutableStateFlow(ApplicationKeyScreenUiState())
    internal val uiState: StateFlow<ApplicationKeyScreenUiState> = _uiState.asStateFlow()

    init {
        observeNetwork()
    }

    private fun observeNetwork() {
        repository.networkEvents
            .map { repository.meshNetwork }
            .filterNotNull()
            .onEach {
                network = it
                val keyState = network.applicationKey(index = keyIndex)
                    ?.let { AppKeyState.Success(key = it) }
                    ?: AppKeyState.Error(throwable = IllegalStateException("Application Key not found."))
                _uiState.update { state ->
                    state.copy(keyState = keyState, networkKeys = network.networkKeys)
                }
            }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Saves the network.
     */
    internal fun save() {
        repository.save()
    }

    @AssistedFactory
    interface Factory {
        fun create(index: Int): ApplicationKeyViewModel
    }
}

internal sealed interface AppKeyState {

    data object Loading : AppKeyState

    data class Success(val key: ApplicationKey) : AppKeyState

    data class Error(val throwable: Throwable) : AppKeyState
}

@ConsistentCopyVisibility
internal data class ApplicationKeyScreenUiState internal constructor(
    val keyState: AppKeyState = AppKeyState.Loading,
    val networkKeys: List<NetworkKey> = emptyList(),
)
