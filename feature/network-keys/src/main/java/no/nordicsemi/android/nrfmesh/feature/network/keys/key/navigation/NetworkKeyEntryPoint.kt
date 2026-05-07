package no.nordicsemi.android.nrfmesh.feature.network.keys.key.navigation

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
import no.nordicsemi.android.nrfmesh.feature.network.keys.key.NetworkKeyScreen
import no.nordicsemi.android.nrfmesh.feature.network.keys.key.NetworkKeyViewModel
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex

@Serializable
data class NetworkKeyContentKey(val keyIndex: KeyIndex) : NavKey {
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.networkKeyEntry(appState: AppState, navigator: Navigator) {
    entry<NetworkKeyContentKey>(
        metadata = ListDetailSceneStrategy.extraPane(
            sceneKey = SettingsListDetailSceneKey
        )
    ) { key ->
        val viewModel = hiltViewModel<NetworkKeyViewModel, NetworkKeyViewModel.Factory> { factory ->
            factory.create(index = key.keyIndex.toInt())
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        NetworkKeyScreen(uiState = uiState, save = viewModel::save)
    }
}