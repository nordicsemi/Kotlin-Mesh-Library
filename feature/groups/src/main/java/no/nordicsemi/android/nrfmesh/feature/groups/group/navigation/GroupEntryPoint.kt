package no.nordicsemi.android.nrfmesh.feature.groups.group.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.getValue
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
import no.nordicsemi.android.nrfmesh.feature.groups.group.GroupViewModel
import no.nordicsemi.android.nrfmesh.feature.groups.group.controls.navigation.GroupControlsKey
import no.nordicsemi.android.nrfmesh.feature.groups.group.controls.navigation.groupControlsEntry
import no.nordicsemi.kotlin.mesh.core.model.Address
import kotlin.uuid.ExperimentalUuidApi

@Serializable
data class GroupKey(val address: Address) : NavKey

@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.groupEntry(appState: AppState, navigator: Navigator) {
    entry<GroupKey>(
        metadata = ListDetailSceneStrategy.listPane(
            sceneKey = GroupsListDetailSceneKey,
            detailPlaceholder = {
                PlaceHolder(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Outlined.Info,
                    text = stringResource(id = R.string.label_no_models_subscribed_rationale)
                )
            }
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
                navigator.navigate(
                    key = GroupControlsKey(
                        address = address,
                        modelId = modelId.id
                    )
                )
            },
            isDetailPaneVisible = navigator.state.currentKey is GroupKey,
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