package no.nordicsemi.android.nrfmesh.network.wizard

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.Configuration
import no.nordicsemi.android.nrfmesh.core.common.ConfigurationProperty
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.network.util.ImportState
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi

@HiltViewModel
class NetworkWizardViewModel @Inject constructor(
    private val repository: CoreDataRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkWizardUiState())
    internal val uiState: StateFlow<NetworkWizardUiState> = _uiState.asStateFlow()

    internal fun onConfigurationSelected(configuration: Configuration) {
        _uiState.update { it.copy(configuration = configuration) }
    }

    /**
     * Adds the [configurationProperty] to the current configuration.
     */
    internal fun increment(configurationProperty: ConfigurationProperty) {
        when (val currentConfiguration = uiState.value.configuration) {
            is Configuration.Custom -> {
                val newConfiguration = when (configurationProperty) {
                    ConfigurationProperty.NETWORK_KEYS -> currentConfiguration.copy(
                        networkKeys = currentConfiguration.networkKeys + 1,
                    )

                    ConfigurationProperty.APPLICATION_KEYS -> currentConfiguration.copy(
                        applicationKeys = currentConfiguration.applicationKeys + 1
                    )

                    ConfigurationProperty.GROUPS -> currentConfiguration.copy(
                        groups = currentConfiguration.groups + 1
                    )

                    ConfigurationProperty.VIRTUAL_GROUPS -> currentConfiguration.copy(
                        virtualGroups = currentConfiguration.virtualGroups + 1
                    )

                    ConfigurationProperty.SCENES -> currentConfiguration.copy(
                        scenes = currentConfiguration.scenes + 1
                    )
                }
                _uiState.update {
                    it.copy(
                        configuration = newConfiguration,
                        configurations = updateList(
                            oldConfiguration = currentConfiguration,
                            newConfiguration = newConfiguration
                        )
                    )
                }
            }

            is Configuration.Debug -> {
                val newConfiguration = when (configurationProperty) {
                    ConfigurationProperty.NETWORK_KEYS -> currentConfiguration.copy(
                        networkKeys = currentConfiguration.networkKeys + 1,
                    )

                    ConfigurationProperty.APPLICATION_KEYS -> currentConfiguration.copy(
                        applicationKeys = currentConfiguration.applicationKeys + 1
                    )

                    ConfigurationProperty.GROUPS -> currentConfiguration.copy(
                        groups = currentConfiguration.groups + 1
                    )

                    ConfigurationProperty.VIRTUAL_GROUPS -> currentConfiguration.copy(
                        virtualGroups = currentConfiguration.virtualGroups + 1
                    )

                    ConfigurationProperty.SCENES -> currentConfiguration.copy(
                        scenes = currentConfiguration.scenes + 1
                    )
                }
                _uiState.update {
                    it.copy(
                        configuration = newConfiguration,
                        configurations = updateList(
                            oldConfiguration = currentConfiguration,
                            newConfiguration = newConfiguration
                        )
                    )
                }
            }

            else -> {

            }
        }
    }

    /**
     * Removes the [configurationProperty] from the current configuration.
     */
    internal fun decrement(configurationProperty: ConfigurationProperty) {
        when (val currentConfiguration = uiState.value.configuration) {
            is Configuration.Custom -> {
                val newConfiguration = when (configurationProperty) {
                    ConfigurationProperty.NETWORK_KEYS -> if (currentConfiguration.networkKeys > 1) currentConfiguration.copy(
                        networkKeys = currentConfiguration.networkKeys - 1,
                    ) else currentConfiguration

                    ConfigurationProperty.APPLICATION_KEYS -> if (currentConfiguration.applicationKeys > 0) currentConfiguration.copy(
                        applicationKeys = currentConfiguration.applicationKeys - 1
                    ) else currentConfiguration


                    ConfigurationProperty.GROUPS -> if (currentConfiguration.groups > 0) currentConfiguration.copy(
                        groups = currentConfiguration.groups - 1
                    ) else currentConfiguration

                    ConfigurationProperty.VIRTUAL_GROUPS -> if (currentConfiguration.virtualGroups > 0) currentConfiguration.copy(
                        virtualGroups = currentConfiguration.virtualGroups - 1
                    ) else currentConfiguration

                    ConfigurationProperty.SCENES -> if (currentConfiguration.scenes > 0) currentConfiguration.copy(
                        scenes = currentConfiguration.scenes - 1
                    ) else currentConfiguration
                }

                _uiState.update {
                    it.copy(
                        configuration = newConfiguration,
                        configurations = updateList(
                            oldConfiguration = currentConfiguration,
                            newConfiguration = newConfiguration
                        )
                    )
                }
            }

            is Configuration.Debug -> {
                val newConfiguration = when (configurationProperty) {
                    ConfigurationProperty.NETWORK_KEYS -> if (currentConfiguration.networkKeys > 1) currentConfiguration.copy(
                        networkKeys = currentConfiguration.networkKeys - 1,
                    ) else currentConfiguration

                    ConfigurationProperty.APPLICATION_KEYS -> if (currentConfiguration.applicationKeys > 0) currentConfiguration.copy(
                        applicationKeys = currentConfiguration.applicationKeys - 1
                    ) else currentConfiguration


                    ConfigurationProperty.GROUPS -> if (currentConfiguration.groups > 0) currentConfiguration.copy(
                        groups = currentConfiguration.groups - 1
                    ) else currentConfiguration

                    ConfigurationProperty.VIRTUAL_GROUPS -> if (currentConfiguration.virtualGroups > 0) currentConfiguration.copy(
                        virtualGroups = currentConfiguration.virtualGroups - 1
                    ) else currentConfiguration

                    ConfigurationProperty.SCENES -> if (currentConfiguration.scenes > 0) currentConfiguration.copy(
                        scenes = currentConfiguration.scenes - 1
                    ) else currentConfiguration
                }
                _uiState.update {
                    it.copy(
                        configuration = newConfiguration,
                        configurations = updateList(
                            oldConfiguration = currentConfiguration,
                            newConfiguration = newConfiguration
                        )
                    )
                }
            }

            else -> {

            }
        }
    }

    /**
     * Updates the list of configuration with the new configuration.
     */
    private fun updateList(
        oldConfiguration: Configuration,
        newConfiguration: Configuration,
    ): List<Configuration> {
        val index = _uiState.value.configurations.indexOf(oldConfiguration)
        val newList = _uiState.value.configurations.toMutableList()
        newList[index] = newConfiguration
        return newList.toList()
    }

    /**
     * Called when the user clicks the continue button after selecting a configuration.
     */
    internal fun onContinuePressed() {
        viewModelScope.launch {
            val configuration = _uiState.value.configuration
            repository.createNewMeshNetwork(configuration = configuration)
            repository.save()
            resetWizardState()
        }
    }

    internal fun resetWizardState() {
        _uiState.update { NetworkWizardUiState() }
    }

    internal fun onImportErrorAcknowledged() {
        _uiState.update { it.copy(importState = ImportState.Unknown) }
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
}

internal data class NetworkWizardUiState(
    val configurations: List<Configuration> = listOf(
        Configuration.Empty,
        Configuration.Custom(),
        Configuration.Debug()
    ),
    val configuration: Configuration = Configuration.Empty,
    val importState: ImportState = ImportState.Unknown,
)