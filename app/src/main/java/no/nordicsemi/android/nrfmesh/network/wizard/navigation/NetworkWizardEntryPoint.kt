package no.nordicsemi.android.nrfmesh.network.wizard.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.network.wizard.NetworkWizardScreen
import no.nordicsemi.android.nrfmesh.network.wizard.NetworkWizardViewModel

@Serializable
object NetworkWizardKey : NavKey

fun EntryProviderScope<NavKey>.networkWizardEntry(
    navigateToNetwork: () -> Unit,
) = entry<NetworkWizardKey> {
    val viewModel = hiltViewModel<NetworkWizardViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NetworkWizardScreen(
        configurations = uiState.configurations,
        configuration = uiState.configuration,
        onConfigurationSelected = viewModel::onConfigurationSelected,
        add = viewModel::increment,
        remove = viewModel::decrement,
        onContinuePressed = {
            viewModel.onContinuePressed()
        },
        importState = uiState.importState,
        importNetwork = { uri, contentResolver ->
            viewModel.importNetwork(uri = uri, contentResolver = contentResolver)
        },
        navigateToNetwork = {
            navigateToNetwork()
            viewModel.resetWizardState()
        },
        onImportErrorAcknowledged = viewModel::onImportErrorAcknowledged
    )
}