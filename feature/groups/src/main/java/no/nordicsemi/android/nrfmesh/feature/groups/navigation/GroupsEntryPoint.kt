package no.nordicsemi.android.nrfmesh.feature.groups.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import no.nordicsemi.android.nrfmesh.core.navigation.AppState
import no.nordicsemi.android.nrfmesh.core.navigation.GroupsKey
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.feature.groups.GroupsScreen
import no.nordicsemi.android.nrfmesh.feature.groups.GroupsViewModel
import no.nordicsemi.android.nrfmesh.feature.groups.group.navigation.GroupKey
import no.nordicsemi.android.nrfmesh.feature.groups.group.navigation.groupEntry
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun EntryProviderScope<NavKey>.groupsEntry(appState: AppState, navigator: Navigator) {
    entry<GroupsKey> {
        val viewModel = hiltViewModel<GroupsViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        GroupsScreen(
             snackbarHostState = appState.snackbarHostState,
            uiState = uiState,
            navigateToGroup = { navigator.navigate(key = GroupKey(address = it.address)) },
            onAddGroupClicked = viewModel::addGroup,
            nextAvailableGroupAddress = viewModel::nextAvailableGroupAddress
        )
    }
    groupEntry(appState = appState, navigator = navigator)
}