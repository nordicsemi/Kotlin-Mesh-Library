package no.nordicsemi.android.feature.config.networkkeys.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.feature.config.networkkeys.AddNetKeysViewModel
import no.nordicsemi.android.feature.config.networkkeys.AddNetworkKeysScreen
import no.nordicsemi.android.nrfmesh.core.navigation.AppState
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.ui.BottomSheetSceneStrategy
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyAdd

@Serializable
data class AddNetKeysKey(val uuid: String) : NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
fun EntryProviderScope<NavKey>.addNetKeysEntry(appState: AppState, navigator: Navigator) {
    entry<AddNetKeysKey>(
        metadata = BottomSheetSceneStrategy.bottomSheet()
    ) { key ->
        val uuid = key.uuid
        val viewModel =
            hiltViewModel<AddNetKeysViewModel, AddNetKeysViewModel.Factory>(key = uuid) {
                it.create(uuid = uuid)
            }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        AddNetworkKeysScreen(
            messageState = uiState.messageState,
            keys = uiState.availableNetworkKeys,
            onAddNetworkKeyClicked = viewModel::addNetworkKey,
            onNetworkKeyClicked = { viewModel.send(ConfigNetKeyAdd(key = it)) },
            resetMessageState = viewModel::resetMessageState
        )
    }
}