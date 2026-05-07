package no.nordicsemi.android.nrfmesh.feature.nodes.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import no.nordicsemi.android.nrfmesh.core.navigation.AppState
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.navigation.NodeKey
import no.nordicsemi.android.nrfmesh.core.navigation.NodesKey
import no.nordicsemi.android.nrfmesh.feature.nodes.NodesScreen
import no.nordicsemi.android.nrfmesh.feature.nodes.NodesViewModel
import no.nordicsemi.android.nrfmesh.feature.nodes.node.navigation.nodeEntry
import no.nordicsemi.android.nrfmesh.feature.provisioning.navigation.ProvisioningKey
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun EntryProviderScope<NavKey>.nodesEntry(
    appState: AppState,
    navigator: Navigator,
    navigateToWizard: () -> Unit,
) {
    entry<NodesKey> {
        val viewModel = hiltViewModel<NodesViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        NodesScreen(
            uiState = uiState,
            navigateToNode = { navigator.navigate(key = NodeKey(nodeUuid = it.toString())) },
            addNode = { navigator.navigate(key = ProvisioningKey) }
        )
    }
    nodeEntry(appState = appState, navigator = navigator, navigateToWizard = navigateToWizard)
}
