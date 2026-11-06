package no.nordicsemi.android.nrfmesh.feature.developer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.DeveloperSettings
import javax.inject.Inject

@HiltViewModel
class DeveloperSettingsViewModel @Inject constructor(
    private val repository: CoreDataRepository,
) : ViewModel() {

    internal val uiState: StateFlow<DeveloperSettingsScreenUi>
        field = MutableStateFlow(value = DeveloperSettingsScreenUi())

    init {
        observeDeveloperSettingsChanges()
    }

    private fun observeDeveloperSettingsChanges() {
        repository.developerSettingsStateFlow.onEach {
            uiState.update { state ->
                state.copy(settings = it)
            }
        }.launchIn(scope = viewModelScope)
    }

    fun onQuickProvisioningEnabled(flag: Boolean) {
        viewModelScope.launch {
            repository.toggleQuickProvisioning(flag = flag)
        }
    }

    fun onAlwaysReconfigure(flag: Boolean) {
        viewModelScope.launch {
            repository.toggleAlwaysReconfigure(flag = flag)
        }
    }
}

internal data class DeveloperSettingsScreenUi(
    val settings: DeveloperSettings = DeveloperSettings(),
)