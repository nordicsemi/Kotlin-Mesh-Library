package no.nordicsemi.android.nrfmesh.feature.settings

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
import no.nordicsemi.android.nrfmesh.core.navigation.ClickableSetting
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork

@HiltViewModel(assistedFactory = SettingsViewModel.Factory::class)
class SettingsViewModel @AssistedInject constructor(
    private val repository: CoreDataRepository,
    @Assisted clickableSetting: ClickableSetting?,
) : ViewModel() {
    private lateinit var meshNetwork: MeshNetwork
    private val _uiState = MutableStateFlow(value = SettingsScreenUiState(selectedSetting = clickableSetting))
    val uiState: StateFlow<SettingsScreenUiState> = _uiState.asStateFlow()

    init {
        observeNetworkState()
    }

    /**
     * Observes the network state and updates the UI state with the current network data.
     */
    private fun observeNetworkState() {
        repository.networkEvents
            .map { repository.meshNetwork }
            .filterNotNull()
            .onEach {
                meshNetwork = it
                _uiState.update { state ->
                    state.copy(
                        networkState = MeshNetworkState.Success(
                            settings = SettingsListData(network = meshNetwork)
                        ),
                        selectedSetting = state.selectedSetting
                    )
                }
            }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Invoked when a setting is selected.
     *
     * @param clickableSetting The setting that was clicked.
     */
    internal fun onItemSelected(clickableSetting: ClickableSetting) {
        _uiState.update { it.copy(selectedSetting = clickableSetting) }
    }

    /**
     * Resets the selected setting to null.
     */
    internal fun resetSelectedSetting() {
        _uiState.update { it.copy(selectedSetting = null) }
    }

    /**
     * Invoked when the name of the network is changed.
     *
     * @param name Name of the network.
     */
    fun onNameChanged(name: String) {
        meshNetwork.name = name
        save()
    }

    fun save() {
        repository.save()
    }

    @AssistedFactory
    interface Factory {
        fun create(setting: ClickableSetting?): SettingsViewModel
    }
}

sealed interface MeshNetworkState {
    data class Success(val settings: SettingsListData) : MeshNetworkState
    data object Loading : MeshNetworkState
}

data class SettingsScreenUiState(
    val networkState: MeshNetworkState = MeshNetworkState.Loading,
    val selectedSetting: ClickableSetting? = null,
)