package no.nordicsemi.android.nrfmesh.feature.config.applicationkeys.navigation

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
import no.nordicsemi.android.nrfmesh.core.navigation.NodeListDetailSceneKey
import no.nordicsemi.android.nrfmesh.feature.config.applicationkeys.ConfigAppKeysScreen
import no.nordicsemi.android.nrfmesh.feature.config.applicationkeys.ConfigAppKeysViewModel

@Serializable
data class ConfigAppKeysKey(val uuid: String) : NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.configAppKeysEntry(appState: AppState, navigator: Navigator) {
    entry<ConfigAppKeysKey>(
        metadata = ListDetailSceneStrategy.detailPane(
            sceneKey = NodeListDetailSceneKey
        )
    ) { key ->
        val uuid = key.uuid
        val viewModel = hiltViewModel<ConfigAppKeysViewModel, ConfigAppKeysViewModel.Factory>(
            key = uuid
        ) { factory ->
            factory.create(uuid = uuid)
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ConfigAppKeysScreen(
            snackbarHostState = appState.snackbarHostState,
            addedApplicationKeys = uiState.addedAppKeys,
            onAddAppKeyClicked = { navigator.navigate(key = AddAppKeysKey(uuid = key.uuid)) },
            readApplicationKeys = viewModel::readApplicationKeys,
            isKeyInUse = viewModel::isKeyInUse,
            messageState = uiState.messageState,
            send = viewModel::send,
            resetMessageState = viewModel::resetMessageState
        )
    }
    addAppKeysEntry(navigator = navigator)
}