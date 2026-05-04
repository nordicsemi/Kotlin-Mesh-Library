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
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges.UnicastRangesScreen
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges.UnicastRangesViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class UnicastRangesContentKey(val uuid: String) : NavKey {
}

@OptIn(
    ExperimentalMaterial3AdaptiveApi::class, ExperimentalUuidApi::class,
    ExperimentalMaterial3Api::class
)
fun EntryProviderScope<NavKey>.unicastRangesEntry(appState: AppState, navigator: Navigator) {
    entry<UnicastRangesContentKey>(
        metadata = BottomSheetSceneStrategy.bottomSheet()
    ) { key ->
        val uuid = key.uuid
        val viewModel = hiltViewModel<UnicastRangesViewModel, UnicastRangesViewModel.Factory>(
            key = "UnicastRangesViewModel:${key.uuid}"
        ) { factory ->
            factory.create(uuid = Uuid.parse(uuidString = uuid))
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        UnicastRangesScreen(
            snackbarHostState = appState.snackbarHostState,
            unicastRanges = uiState.ranges,
            otherUnicastRanges = uiState.otherRanges,
            allocate = viewModel::allocate,
            removeAllRanges = viewModel::removeAllRanges,
            save = viewModel::save,
            navigateBack = navigator::goBack
        )
    }
}