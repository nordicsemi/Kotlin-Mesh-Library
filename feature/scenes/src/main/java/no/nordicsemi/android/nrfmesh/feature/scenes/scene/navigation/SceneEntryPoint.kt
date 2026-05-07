package no.nordicsemi.android.nrfmesh.feature.scenes.scene.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.core.navigation.SettingsListDetailSceneKey
import no.nordicsemi.android.nrfmesh.feature.scenes.scene.SceneScreen
import no.nordicsemi.android.nrfmesh.feature.scenes.scene.SceneViewModel
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber

@Serializable
data class SceneContentKey(val number: SceneNumber) : NavKey {
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.sceneEntry() {
    entry<SceneContentKey>(
        metadata = ListDetailSceneStrategy.extraPane(
            sceneKey = SettingsListDetailSceneKey
        )
    ) { key ->
        val viewModel = hiltViewModel<SceneViewModel, SceneViewModel.Factory>(
            key = "SceneViewModel:${key.number}"
        ) { factory ->
            factory.create(number = key.number.toInt())
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        SceneScreen(uiState = uiState, save = viewModel::save)
    }
}