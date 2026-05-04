@file:Suppress("unused")

package no.nordicsemi.android.nrfmesh.feature.provisioners.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.core.navigation.AppState
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.navigation.SettingsListDetailSceneKey
import no.nordicsemi.android.nrfmesh.core.ui.isCompactWidth
import no.nordicsemi.android.nrfmesh.feature.provisioners.ProvisionersScreen
import no.nordicsemi.android.nrfmesh.feature.provisioners.ProvisionersViewModel
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.navigation.ProvisionerContentKey
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.navigation.provisionerEntry
import kotlin.uuid.ExperimentalUuidApi

@Serializable
data object ProvisionersContentKey : NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalUuidApi::class)
fun EntryProviderScope<NavKey>.provisionersEntry(appState: AppState, navigator: Navigator) {
    entry<ProvisionersContentKey>(
        metadata = ListDetailSceneStrategy.detailPane(
            sceneKey = SettingsListDetailSceneKey
        )
    ) {
        val viewModel = hiltViewModel<ProvisionersViewModel, ProvisionersViewModel.Factory>(
            key = "ProvisionersViewModel"
        ) { factory ->
            factory.create()
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ProvisionersScreen(
            snackbarHostState = appState.snackbarHostState,
            highlightSelectedItem =  !isCompactWidth() && appState.navigationState.currentKey is ProvisionerContentKey,
            selectedProvisionerUuid = uiState.selectedProvisionerUuid,
            provisioners = uiState.provisioners,
            onAddProvisionerClicked = viewModel::addProvisioner,
            onProvisionerClicked = {
                viewModel.selectProvisioner(uuid = it)
                navigator.navigate(key = ProvisionerContentKey(uuid = it))
            },
            navigateToProvisioner = {
                viewModel.selectProvisioner(uuid = it)
                navigator.navigate(key = ProvisionerContentKey(uuid = it))
            },
            onSwiped = {
                viewModel.onSwiped(provisioner = it)
                if (uiState.selectedProvisionerUuid == it.uuid) {
                    navigator.goBack()
                }
            },
            onUndoClicked = viewModel::onUndoSwipe,
            remove = viewModel::remove
        )
    }
    provisionerEntry(appState = appState, navigator = navigator)
}