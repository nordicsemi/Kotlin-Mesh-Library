package no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.navigation

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
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ProvisionerScreen
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ProvisionerViewModel
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges.navigation.GroupRangesContentKey
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges.navigation.SceneRangesContentKey
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges.navigation.UnicastRangesContentKey
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges.navigation.groupRangesEntry
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges.navigation.sceneRangesEntry
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges.navigation.unicastRangesEntry
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class ProvisionerContentKey(val uuid: String) : NavKey {
    constructor(uuid: Uuid) : this(uuid = uuid.toString())
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalUuidApi::class)
fun EntryProviderScope<NavKey>.provisionerEntry(appState: AppState, navigator: Navigator) {
    entry<ProvisionerContentKey>(
        metadata = ListDetailSceneStrategy.extraPane(
            sceneKey = SettingsListDetailSceneKey
        )
    ) { key ->
        val uuid = key.uuid
        val viewModel = hiltViewModel<ProvisionerViewModel, ProvisionerViewModel.Factory>(
            key = "ProvisionerViewModel:${key.uuid}"
        ) { factory ->
            factory.create(uuid = Uuid.parse(uuidString = uuid))
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ProvisionerScreen(
            uiState = uiState,
            snackbarHostState = appState.snackbarHostState,
            moveProvisioner = viewModel::moveProvisioner,
            navigateToUnicastRanges = {
                navigator.navigate(key = UnicastRangesContentKey(uuid = uuid))
            },
            navigateToGroupRanges = {
                navigator.navigate(key = GroupRangesContentKey(uuid = uuid))
            },
            navigateToSceneRanges = {
                navigator.navigate(key = SceneRangesContentKey(uuid = uuid))
            },
            save = viewModel::save
        )
    }
    unicastRangesEntry(appState = appState, navigator = navigator)
    groupRangesEntry(appState = appState, navigator = navigator)
    sceneRangesEntry(appState = appState, navigator = navigator)
}