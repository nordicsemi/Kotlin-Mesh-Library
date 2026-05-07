package no.nordicsemi.android.nrfmesh.feature.application.keys.navigation

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
import no.nordicsemi.android.nrfmesh.feature.application.keys.ApplicationKeysScreen
import no.nordicsemi.android.nrfmesh.feature.application.keys.ApplicationKeysViewModel
import no.nordicsemi.android.nrfmesh.feature.application.keys.key.navigation.ApplicationKeyContentKey
import no.nordicsemi.android.nrfmesh.feature.application.keys.key.navigation.applicationKeyEntry

@Serializable
data object ApplicationKeysContentKey : NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.applicationKeysEntry(appState: AppState, navigator: Navigator) {
    entry<ApplicationKeysContentKey>(
        metadata = ListDetailSceneStrategy.detailPane(
            sceneKey = SettingsListDetailSceneKey
        )
    ) {
        val viewModel = hiltViewModel<ApplicationKeysViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ApplicationKeysScreen(
            snackbarHostState = appState.snackbarHostState,
            highlightSelectedItem = !isCompactWidth() && appState.navigationState.currentKey is ApplicationKeyContentKey,
            selectedKeyIndex = uiState.selectedKeyIndex,
            keys = uiState.keys,
            onAddKeyClicked = viewModel::addApplicationKey,
            onApplicationKeyClicked = {
                viewModel.selectKeyIndex(keyIndex = it)
                navigator.navigate(key = ApplicationKeyContentKey(keyIndex = it))
            },
            onSwiped = {
                viewModel.onSwiped(key = it)
                if (uiState.selectedKeyIndex == it.index) {
                    val index = navigator.state.currentSubStack.indexOfFirst { it == ApplicationKeysContentKey }
                    repeat(times = navigator.state.currentSubStack.size - index - 1) {
                        navigator.goBack()
                    }
                    viewModel.selectKeyIndex(keyIndex = null)
                }
            },
            onUndoClicked = viewModel::onUndoSwipe,
            remove = viewModel::remove
        )
    }
    applicationKeyEntry(appState = appState, navigator = navigator)
}