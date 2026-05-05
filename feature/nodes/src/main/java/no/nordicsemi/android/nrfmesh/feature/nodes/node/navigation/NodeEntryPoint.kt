package no.nordicsemi.android.nrfmesh.feature.nodes.node.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import no.nordicsemi.android.feature.config.networkkeys.ConfigNetKeysKey
import no.nordicsemi.android.feature.config.networkkeys.configNetKeysEntry
import no.nordicsemi.android.nrfmesh.core.navigation.AppState
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.navigation.NodeKey
import no.nordicsemi.android.nrfmesh.core.navigation.NodeListDetailSceneKey
import no.nordicsemi.android.nrfmesh.core.navigation.NodesKey
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.ui.PlaceHolder
import no.nordicsemi.android.nrfmesh.core.ui.isCompactWidth
import no.nordicsemi.android.nrfmesh.feature.config.applicationkeys.ConfigAppKeysKey
import no.nordicsemi.android.nrfmesh.feature.config.applicationkeys.configAppKeysEntry
import no.nordicsemi.android.nrfmesh.feature.nodes.R
import no.nordicsemi.android.nrfmesh.feature.nodes.node.ClickableNodeInfoItem
import no.nordicsemi.android.nrfmesh.feature.nodes.node.NodeScreen
import no.nordicsemi.android.nrfmesh.feature.nodes.node.NodeState
import no.nordicsemi.android.nrfmesh.feature.nodes.node.NodeViewModel
import no.nordicsemi.android.nrfmesh.feature.nodes.node.element.navigation.ElementKey
import no.nordicsemi.android.nrfmesh.feature.nodes.node.element.navigation.elementEntry
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNodeResetStatus
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.nodeEntry(
    appState: AppState,
    navigator: Navigator,
    navigateToWizard: () -> Unit,
) {
    entry<NodeKey>(
        metadata = ListDetailSceneStrategy.listPane(
            sceneKey = NodeListDetailSceneKey,
            detailPlaceholder = {
                PlaceHolder(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Outlined.Info,
                    text = stringResource(R.string.label_select_node_item_rationale)
                )
            }
        )
    ) { key ->
        val uuid = key.nodeUuid
        val viewModel = hiltViewModel<NodeViewModel, NodeViewModel.Factory>(key = uuid) {
            it.create(uuid = uuid)
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(
            uiState
                .run { messageState.didSucceed() && messageState.response is ConfigNodeResetStatus }
        ) {
            uiState
                .takeIf { it.messageState.didSucceed() && it.messageState.response is ConfigNodeResetStatus }
                ?.let {
                    // We navigate to the top level key instead of going back and this will clear
                    // the substack. This is due to the list detail scene strategy on the tablet
                    // may be one entry ahead in the back stack.
                    navigator.navigate(key = NodesKey)
                }
        }

        when (val nodeState = uiState.nodeState) {
            is NodeState.Success -> {
                NodeScreen(
                    messageState = uiState.messageState,
                    nodeData = nodeState.nodeInfoListData,
                    node = nodeState.node,
                    highlightSelectedItem = !isCompactWidth(),
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = viewModel::onRefresh,
                    onNetworkKeysClicked = {
                        viewModel.onItemSelected(item = ClickableNodeInfoItem.NetworkKeys)
                        navigator.navigate(key = ConfigNetKeysKey(uuid = it.toString()))
                    },
                    onApplicationKeysClicked = {
                        viewModel.onItemSelected(item = ClickableNodeInfoItem.ApplicationKeys)
                        navigator.navigate(key = ConfigAppKeysKey(uuid = it.toString()))
                    },
                    onElementClicked = { address ->
                        viewModel.onItemSelected(item = ClickableNodeInfoItem.Element(address = address))
                        navigator.navigate(key = ElementKey(address = address))
                    },
                    onExcluded = viewModel::onExcluded,
                    selectedItem = uiState.selectedNodeInfoItem,
                    send = viewModel::send,
                    save = viewModel::save,
                    navigateBack = { navigator.navigate(key = NodesKey) },
                    removeNode = viewModel::removeNode,
                    tasks = uiState.tasks,
                    onReconfigCompletePressed = viewModel::onReconfigCompletePressed,
                    onCancelPressed = viewModel::onCancelPressed,
                    onRetryPressed = viewModel::onRetryPressed
                )
                var showNoNetworkDialog by remember(key1 = uiState.wasNetworkRemoved) { mutableStateOf(uiState.wasNetworkRemoved) }
                if (showNoNetworkDialog) {
                    MeshAlertDialog(
                        onDismissRequest = { showNoNetworkDialog = false },
                        icon = Icons.Outlined.ErrorOutline,
                        title = stringResource(R.string.label_no_network),
                        text = stringResource(R.string.label_no_network_rationale),
                        iconColor = Color.Red,
                        onConfirmClick = {
                            showNoNetworkDialog = false
                            navigateToWizard()
                            // navigator.navigate(key = NodesKey)
                        }
                    )
                }
            }

            is NodeState.Error -> {

            }

            else -> {

            }
        }
    }
    configNetKeysEntry(appState = appState)
    configAppKeysEntry(appState = appState)
    elementEntry(appState = appState, navigator = navigator)
}