package no.nordicsemi.android.nrfmesh.feature.model.navigation

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
import no.nordicsemi.android.nrfmesh.core.navigation.GroupsKey
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.navigation.NodeListDetailSceneKey
import no.nordicsemi.android.nrfmesh.core.navigation.NodesKey
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.feature.model.ModelScreen
import no.nordicsemi.android.nrfmesh.feature.model.ModelState
import no.nordicsemi.android.nrfmesh.feature.model.ModelViewModel
import no.nordicsemi.android.nrfmesh.feature.model.dfu.navigation.FirmwareInformationKey
import no.nordicsemi.android.nrfmesh.feature.model.dfu.navigation.firmwareInformationEntryPoint
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.model.Address
import kotlin.uuid.ExperimentalUuidApi

@Serializable
data class ModelKey(val address: Address, val modelId: UInt) : NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalUuidApi::class)
fun EntryProviderScope<NavKey>.modelEntry(appState: AppState, navigator: Navigator) {
    var viewModel: ModelViewModel? = null
    entry<ModelKey>(
        metadata = ListDetailSceneStrategy.extraPane(
            sceneKey = NodeListDetailSceneKey
        )
    ) { key ->
        viewModel = hiltViewModel<ModelViewModel, ModelViewModel.Factory> {
            it.create(key.address.toInt(), key.modelId.toInt())
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        when (val modelState = uiState.modelState) {
            is ModelState.Success -> {
                ModelScreen(
                    snackbarHostState = appState.snackbarHostState,
                    modelState = modelState,
                    messageState = uiState.messageState,
                    nodeIdentityStates = uiState.nodeIdentityStates,
                    requestNodeIdentityStates = viewModel::requestNodeIdentityStates,
                    onAddGroupClicked = { navigator.navigate(GroupsKey) },
                    resetMessageState = viewModel::resetMessageState,
                    navigateToGroups = { navigator.navigate(key = GroupsKey) },
                    navigateToFirmwareInformation = { model, information ->
                        navigator.navigate(
                            key = FirmwareInformationKey(
                                model = model,
                                information = information
                            )
                        )
                    },
                    send = viewModel::send,
                    sendApplicationMessage = viewModel::sendApplicationMessage,
                    sendAcknowledgedMessage = viewModel::send
                )
                var showNoNetworkDialog by remember { mutableStateOf(uiState.wasNetworkRemoved) }
                if (showNoNetworkDialog) {
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

            is ModelState.Error -> {}
            ModelState.Loading -> {}
        }
    }
    firmwareInformationEntryPoint(
        isInProgress = false,
        send = { model, message ->
            viewModel?.send(model, message)
        }
    )
}