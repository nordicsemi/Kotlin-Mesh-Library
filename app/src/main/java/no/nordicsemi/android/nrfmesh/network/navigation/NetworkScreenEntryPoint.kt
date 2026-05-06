package no.nordicsemi.android.nrfmesh.network.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.core.navigation.MESH_TOP_LEVEL_NAV_ITEMS
import no.nordicsemi.android.nrfmesh.core.navigation.NodesKey
import no.nordicsemi.android.nrfmesh.core.navigation.rememberNavigationState
import no.nordicsemi.android.nrfmesh.network.NetworkScreen
import no.nordicsemi.android.nrfmesh.rememberMeshAppState
import no.nordicsemi.android.nrfmesh.network.NetworkViewModel

@Serializable
object NetworkScreenKey : NavKey

fun EntryProviderScope<NavKey>.networkScreenEntry(
    navigateToWizard: () -> Unit,
) = entry<NetworkScreenKey> {
    val snackbarHostState = remember { SnackbarHostState() }
    val navigationState = rememberNavigationState(
        startKey = NodesKey,
        topLevelKeys = MESH_TOP_LEVEL_NAV_ITEMS.keys
    )
    val appState = rememberMeshAppState(
        snackbarHostState = snackbarHostState, navigationState = navigationState
    )
    val viewModel = hiltViewModel<NetworkViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    NetworkScreen(
        appState = appState,
        uiState = uiState,
        shouldSelectProvisioner = uiState.shouldSelectProvisioner,
        resetNetwork = viewModel::resetNetwork,
        navigateToWizard = navigateToWizard,
        importNetwork = viewModel::importNetwork,
        onImportErrorAcknowledged = viewModel::onImportErrorAcknowledged,
        resetMeshNetworkUiState = viewModel::resetMeshNetworkUiState,
        startAttentionTimer = viewModel::startAttentionTimer
    )
}