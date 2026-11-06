package no.nordicsemi.android.nrfmesh.feature.nodes.node.scenes.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.core.navigation.NodeListDetailSceneKey
import no.nordicsemi.android.nrfmesh.feature.nodes.node.scenes.NodeScenesScreen
import no.nordicsemi.android.nrfmesh.feature.nodes.node.scenes.NodeScenesViewModel

/**
 * Navigation key of the screen listing the scenes stored on a node.
 *
 * @property nodeUuid UUID of the node.
 */
@Serializable
data class NodeScenesKey(val nodeUuid: String) : NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.nodeScenesEntry() {
    entry<NodeScenesKey>(
        metadata = ListDetailSceneStrategy.detailPane(
            sceneKey = NodeListDetailSceneKey
        )
    ) { key ->
        val viewModel = hiltViewModel<NodeScenesViewModel, NodeScenesViewModel.Factory>(
            key = key.nodeUuid
        ) {
            it.create(uuid = key.nodeUuid)
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        NodeScenesScreen(
            uiState = uiState,
            onRefresh = viewModel::onRefresh,
            onSceneClicked = viewModel::recall,
            onSceneDeleted = viewModel::delete,
            onSceneStored = viewModel::store,
            onFixClicked = viewModel::fixClient,
            onUniqueKeySetupClicked = viewModel::uniqueKeySetup,
            onRetryPressed = viewModel::onRetryPressed,
            onTasksDismissed = viewModel::onTasksDismissed,
            onMessageHandled = viewModel::onMessageHandled
        )
    }
}
