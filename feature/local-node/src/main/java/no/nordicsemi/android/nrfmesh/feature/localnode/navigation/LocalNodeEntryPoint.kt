package no.nordicsemi.android.nrfmesh.feature.localnode.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import no.nordicsemi.android.nrfmesh.core.navigation.LocalNodeKey
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.navigation.NodeKey
import no.nordicsemi.android.nrfmesh.core.navigation.NodesKey
import no.nordicsemi.android.nrfmesh.feature.model.navigation.ModelKey
import no.nordicsemi.android.nrfmesh.feature.nodes.node.element.navigation.ElementKey
import no.nordicsemi.android.nrfmesh.feature.localnode.LocalNodeScreen
import no.nordicsemi.android.nrfmesh.feature.localnode.LocalNodeViewModel

fun EntryProviderScope<NavKey>.localNodeEntry(navigator: Navigator) {
    entry<LocalNodeKey> {
        val viewModel = hiltViewModel<LocalNodeViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        LocalNodeScreen(
            uiState = uiState,
            onOnOff = viewModel::setOnOff,
            onLevelStep = viewModel::stepLevel,
            navigateToModel = { model ->
                // Open the model through the Nodes tab: Nodes > local node > element > model. The
                // Local Node tab is the start destination, so going back all the way returns to it.
                val element = model.parentElement
                val node = element?.parentNode
                if (element != null && node != null) {
                    navigator.navigate(key = NodesKey)
                    navigator.navigate(key = NodeKey(nodeUuid = node.uuid.toString()))
                    navigator.navigate(key = ElementKey(address = element.unicastAddress.address))
                    navigator.navigate(
                        key = ModelKey(address = element.unicastAddress.address, modelId = model.modelId.id)
                    )
                }
            }
        )
    }
}
