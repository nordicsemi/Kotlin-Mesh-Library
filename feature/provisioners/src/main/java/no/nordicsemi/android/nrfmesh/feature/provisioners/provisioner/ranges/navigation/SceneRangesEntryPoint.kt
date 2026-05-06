package no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.core.navigation.AppState
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.ui.BottomSheetSceneStrategy
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges.SceneRangesScreen
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges.SceneRangesViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class SceneRangesContentKey(val uuid: String) : NavKey

@OptIn(
    ExperimentalMaterial3AdaptiveApi::class, ExperimentalUuidApi::class,
    ExperimentalMaterial3Api::class
)
fun EntryProviderScope<NavKey>.sceneRangesEntry(appState: AppState, navigator: Navigator) {
    entry<SceneRangesContentKey>(
        metadata = BottomSheetSceneStrategy.bottomSheet()
    ) { key ->
        val uuid = key.uuid
        val viewModel = hiltViewModel<SceneRangesViewModel, SceneRangesViewModel.Factory>(
            key = "SceneRangesViewModel:${key.uuid}"
        ) { factory ->
            factory.create(uuid = Uuid.parse(uuidString = uuid))
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        SceneRangesScreen(
            snackbarHostState = appState.snackbarHostState,
            sceneRanges = uiState.ranges,
            otherRanges = uiState.otherRanges,
            allocate = viewModel::allocate,
            removeAllRanges = viewModel::removeAllRanges,
            save = viewModel::save,
            navigateBack = navigator::goBack
        )
    }
}