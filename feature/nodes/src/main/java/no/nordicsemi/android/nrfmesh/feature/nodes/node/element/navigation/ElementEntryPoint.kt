package no.nordicsemi.android.nrfmesh.feature.nodes.node.element.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.core.navigation.AppState
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.navigation.NodeListDetailSceneKey
import no.nordicsemi.android.nrfmesh.core.navigation.NodesKey
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.ui.isCompactWidth
import no.nordicsemi.android.nrfmesh.feature.model.navigation.ModelKey
import no.nordicsemi.android.nrfmesh.feature.model.navigation.modelEntry
import no.nordicsemi.android.nrfmesh.feature.nodes.R
import no.nordicsemi.android.nrfmesh.feature.nodes.node.element.ElementScreen
import no.nordicsemi.android.nrfmesh.feature.nodes.node.element.ElementState
import no.nordicsemi.android.nrfmesh.feature.nodes.node.element.ElementViewModel
import no.nordicsemi.kotlin.mesh.core.exception.NoNetwork
import no.nordicsemi.kotlin.mesh.core.model.Address
import kotlin.uuid.ExperimentalUuidApi

@Serializable
data class ElementKey(val address: Address) : NavKey

@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.elementEntry(appState: AppState, navigator: Navigator) {
    entry<ElementKey>(
        metadata = ListDetailSceneStrategy.detailPane(
            sceneKey = NodeListDetailSceneKey
        )
    ) { key ->
        val address = key.address
        val viewModel = hiltViewModel<ElementViewModel, ElementViewModel.Factory> {
            it.create(address = address.toInt())
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        when (val elementState = uiState.elementState) {
            is ElementState.Success -> {
                ElementScreen(
                    element = elementState.element,
                    highlightSelectedItem = !isCompactWidth() && appState.navigationState.currentKey is ModelKey,
                    navigateToModel = { model ->
                        navigator.navigate(
                            key = ModelKey(
                                address = key.address,
                                modelId = model.modelId.id
                            )
                        )
                    },
                    save = viewModel::save
                )
                var showNoNetworkDialog by remember { mutableStateOf(uiState.wasNetworkRemoved) }
                if(showNoNetworkDialog){
                    MeshAlertDialog(
                        onDismissRequest = { showNoNetworkDialog = false },
                        icon = Icons.Outlined.ErrorOutline,
                        title = stringResource(R.string.label_no_network),
                        text = stringResource(R.string.label_no_network_rationale),
                        iconColor = Color.Red,
                        onConfirmClick = {
                            showNoNetworkDialog = false
                            navigator.navigate(key = NodesKey)
                        }
                    )
                }
            }

            is ElementState.Error -> {
                if (elementState.throwable is NoNetwork) {
                    MeshAlertDialog(
                        onDismissRequest = { },
                        icon = Icons.Outlined.ErrorOutline,
                        title = stringResource(R.string.label_no_network),
                        text = stringResource(R.string.label_no_network_rationale),
                        iconColor = Color.Red,
                        onConfirmClick = {
                            navigator.navigate(key = NodesKey)
                        }
                    )
                }
            }
            ElementState.Loading -> {

            }
        }
    }
    modelEntry(appState = appState, navigator = navigator)
}