package no.nordicsemi.android.nrfmesh.feature.scenes.navigation

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
import no.nordicsemi.android.nrfmesh.feature.scenes.ScenesScreen
import no.nordicsemi.android.nrfmesh.feature.scenes.ScenesViewModel
import no.nordicsemi.android.nrfmesh.feature.scenes.scene.navigation.SceneContentKey
import no.nordicsemi.android.nrfmesh.feature.scenes.scene.navigation.sceneEntry

@Serializable
data object ScenesContentKey : NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.scenesEntry(appState: AppState, navigator: Navigator) {
    entry<ScenesContentKey>(
        metadata = ListDetailSceneStrategy.detailPane(
            sceneKey = SettingsListDetailSceneKey
        )
    ) {
        val viewModel = hiltViewModel<ScenesViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ScenesScreen(
            snackbarHostState = appState.snackbarHostState,
            highlightSelectedItem = !isCompactWidth() && appState.navigationState.currentKey is SceneContentKey,
            selectedSceneNumber = uiState.selectedSceneNumber,
            scenes = uiState.scenes,
            onAddSceneClicked = viewModel::addScene,
            onSceneClicked = { sceneNumber ->
                viewModel.selectScene(number = sceneNumber)
                navigator.navigate(key = SceneContentKey(number = sceneNumber))
            },
            onSwiped = { sceneData ->
                viewModel.onSwiped(scene = sceneData)
                if (uiState.selectedSceneNumber == sceneData.number) {
                    val index = navigator.state.currentSubStack.indexOfFirst { it == ScenesContentKey }
                    repeat(times = navigator.state.currentSubStack.size - index - 1) {
                        navigator.goBack()
                    }
                    viewModel.selectScene(number = null)
                }
            },
            onUndoClicked = viewModel::onUndoSwipe,
            remove = viewModel::remove
        )
    }
    sceneEntry()
}