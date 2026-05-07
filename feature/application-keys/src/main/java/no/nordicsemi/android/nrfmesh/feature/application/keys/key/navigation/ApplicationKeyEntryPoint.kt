package no.nordicsemi.android.nrfmesh.feature.application.keys.key.navigation

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
import no.nordicsemi.android.nrfmesh.feature.application.keys.key.ApplicationKeyScreen
import no.nordicsemi.android.nrfmesh.feature.application.keys.key.ApplicationKeyViewModel
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex

@Serializable
data class ApplicationKeyContentKey(val keyIndex: KeyIndex) : NavKey {
}


@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.applicationKeyEntry(appState: AppState, navigator: Navigator) {
    entry<ApplicationKeyContentKey>(
        metadata = ListDetailSceneStrategy.extraPane(
            sceneKey = SettingsListDetailSceneKey
        )
    ) { key ->
        val viewModel = hiltViewModel<ApplicationKeyViewModel, ApplicationKeyViewModel.Factory> { factory ->
            factory.create(index = key.keyIndex.toInt())
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ApplicationKeyScreen(
            uiState = uiState,
            snackbarHostState = appState.snackbarHostState,
            save = viewModel::save
        )
    }
}