package no.nordicsemi.android.nrfmesh.feature.provisioning.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.core.navigation.AppState
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.navigation.NodeKey
import no.nordicsemi.android.nrfmesh.core.navigation.NodesKey
import no.nordicsemi.android.nrfmesh.feature.provisioning.ProvisioningScreen
import no.nordicsemi.android.nrfmesh.feature.provisioning.ProvisioningViewModel
import kotlin.uuid.ExperimentalUuidApi

@Serializable
data object ProvisioningKey : NavKey

@OptIn(ExperimentalUuidApi::class)
fun EntryProviderScope<NavKey>.provisioningEntry(
    appState: AppState,
    navigator: Navigator,
) {
    entry<ProvisioningKey> {
        val viewModel = hiltViewModel<ProvisioningViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ProvisioningScreen(
            snackbarHostState = appState.snackbarHostState,
            uiState = uiState,
            beginProvisioning = viewModel::beginProvisioning,
            onNameChanged = viewModel::onNameChanged,
            onAddressChanged = viewModel::onAddressChanged,
            isValidAddress = viewModel::isValidAddress,
            onNetworkKeyClicked = viewModel::onNetworkKeyClicked,
            onAuthenticationMethodSelected = viewModel::onAuthenticationMethodSelected,
            authenticate = viewModel::authenticate,
            onProvisioningComplete = {
                viewModel.onProvisioningComplete(uuid = it)
                navigator.navigate(key = NodesKey)
                navigator.navigate(key = NodeKey(nodeUuid = it.toString()))
            },
            onProvisioningFailed = {
                viewModel.onProvisioningFailed()
                navigator.goBack()
            },
            disconnect = viewModel::disconnect,
            onScanResultSelected = viewModel::onScanResultSelected
        )
    }
}