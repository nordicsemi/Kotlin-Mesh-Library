package no.nordicsemi.android.nrfmesh.feature.groups.group.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.core.navigation.AppState
import no.nordicsemi.android.nrfmesh.core.navigation.GroupsListDetailSceneKey
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.ui.PlaceHolder
import no.nordicsemi.android.nrfmesh.feature.groups.R
import no.nordicsemi.android.nrfmesh.feature.groups.group.GroupScreen
import no.nordicsemi.android.nrfmesh.feature.groups.group.GroupState
import no.nordicsemi.android.nrfmesh.feature.groups.group.GroupViewModel
import no.nordicsemi.android.nrfmesh.feature.groups.group.controls.navigation.GroupControlsKey
import no.nordicsemi.android.nrfmesh.feature.groups.group.controls.navigation.groupControlsEntry
import no.nordicsemi.kotlin.mesh.core.model.Address

@Serializable
data class GroupKey(val address: Address) : NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.groupEntry(appState: AppState, navigator: Navigator) {
    entry<GroupKey>(
        metadata = ListDetailSceneStrategy.listPane(
            sceneKey = GroupsListDetailSceneKey,
            detailPlaceholder = { GroupDetailPlaceholder(navigator = navigator) }
        )
    ) { key ->
        val address = key.address
        val viewModel = hiltViewModel<GroupViewModel, GroupViewModel.Factory> {
            it.create(address = address.toInt())
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        GroupScreen(
            uiState = uiState,
            snackbarHostState = appState.snackbarHostState,
            onModelClicked = { modelId, index ->
                viewModel.onModelClicked(index = index)
                navigator.navigateFrom(
                    parent = key,
                    key = GroupControlsKey(
                        address = address,
                        modelId = modelId.id
                    )
                )
            },
            isDetailPaneVisible = navigator.state.currentKey is GroupKey,
            isControlsVisible = navigator.state.currentKey is GroupControlsKey,
            deleteGroup = {
                if (viewModel.deleteGroup(group = it)) {
                    navigator.goBack()
                }
            },
            save = viewModel::save
        )
    }
    groupControlsEntry()
}

/**
 * Shown in the detail pane until a subscribed model is selected. It asks the user to select a model
 * if any of the subscribed models has an application key bound, or else explains what is needed.
 */
@Composable
private fun GroupDetailPlaceholder(navigator: Navigator) {
    val group = navigator.state.currentSubStack.filterIsInstance<GroupKey>().lastOrNull()
    var isControllable = false
    if (group != null) {
        val viewModel = hiltViewModel<GroupViewModel, GroupViewModel.Factory>(
            key = "placeholder-${group.address}"
        ) { it.create(address = group.address.toInt()) }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        (uiState.groupState as? GroupState.Success)?.let { state ->
            isControllable = state.groupInfoListData.models.values.flatten().any { model ->
                state.network.applicationKeys.any { it.isBoundTo(model = model) }
            }
        }
    }
    PlaceHolder(
        modifier = Modifier.fillMaxSize(),
        imageVector = Icons.Outlined.Info,
        text = stringResource(
            id = when (isControllable) {
                true -> R.string.label_select_model_rationale
                false -> R.string.label_no_models_subscribed_rationale
            }
        )
    )
}
