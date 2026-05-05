package no.nordicsemi.android.feature.config.networkkeys

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.feature.config.networkkeys.navigation.ConfigNetKeysViewModel
import no.nordicsemi.android.nrfmesh.core.navigation.AppState
import no.nordicsemi.android.nrfmesh.core.navigation.NodeListDetailSceneKey

@Serializable
data class ConfigNetKeysKey(val uuid: String) : NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.configNetKeysEntry(appState: AppState) {
    entry<ConfigNetKeysKey>(
        metadata = ListDetailSceneStrategy.detailPane(
            sceneKey = NodeListDetailSceneKey
        )
    ) {
        val uuid = it.uuid
        val viewModel =
            hiltViewModel<ConfigNetKeysViewModel, ConfigNetKeysViewModel.Factory>(key = uuid) {
                it.create(uuid = uuid)
            }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ConfigNetKeysScreen(
            messageState = uiState.messageState,
            snackbarHostState = appState.snackbarHostState,
            isLocalProvisionerNode = uiState.isLocalProvisionerNode,
            availableNetworkKeys = uiState.availableNetworkKeys,
            addedNetworkKeys = uiState.addedNetworkKeys,
            onAddNetworkKeyClicked = viewModel::addNetworkKey,
            isKeyInUse = viewModel::isKeyInUse,
            send = viewModel::send,
            resetMessageState = viewModel::resetMessageState
        )
    }
}