package no.nordicsemi.android.nrfmesh.feature.export.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.core.navigation.AppState
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.ui.BottomSheetSceneStrategy
import no.nordicsemi.android.nrfmesh.feature.export.ExportViewModel

@Serializable
data object ExportKey : NavKey

@OptIn(ExperimentalMaterial3Api::class)
fun EntryProviderScope<NavKey>.exportEntry(appState: AppState, navigator: Navigator) =
    entry<ExportKey>(metadata = BottomSheetSceneStrategy.bottomSheet()) {
        val viewModel = hiltViewModel<ExportViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        ExportScreen(
            uiState = uiState,
            onExportOptionSelected = viewModel::onExportOptionSelected,
            onNetworkKeySelected = viewModel::onNetworkKeySelected,
            onProvisionerSelected = viewModel::onProvisionerSelected,
            onExportDeviceKeysToggled = viewModel::onExportDeviceKeysToggled,
            export = { contentResolver, uri ->
                viewModel.export(contentResolver = contentResolver, uri = uri)
                navigator.goBack()
            },
            onExportStateDisplayed = viewModel::onExportStateDisplayed,
            onExportCompleted = {
                navigator.goBack()
                scope.launch {
                    appState.snackbarHostState.run {
                        currentSnackbarData?.dismiss()
                        showSnackbar(message = it, duration = SnackbarDuration.Short)
                    }
                }
            }
        )
    }