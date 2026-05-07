package no.nordicsemi.android.nrfmesh.feature.groups.group.controls.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.core.navigation.GroupsListDetailSceneKey
import no.nordicsemi.android.nrfmesh.feature.groups.group.controls.GroupControlsScreen
import no.nordicsemi.android.nrfmesh.feature.groups.group.controls.GroupControlsViewModel
import no.nordicsemi.kotlin.mesh.core.model.Address
import kotlin.uuid.ExperimentalUuidApi

@Serializable
data class GroupControlsKey(val address: Address, val modelId: UInt) : NavKey

@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.groupControlsEntry() {
    entry<GroupControlsKey>(
        metadata = ListDetailSceneStrategy.detailPane(sceneKey = GroupsListDetailSceneKey)
    ) { key ->
        val viewModel = hiltViewModel<GroupControlsViewModel, GroupControlsViewModel.Factory> {
            it.create(key.address.toInt(), key.modelId.toInt())
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        GroupControlsScreen(uiState = uiState, send = viewModel::send)
    }
}