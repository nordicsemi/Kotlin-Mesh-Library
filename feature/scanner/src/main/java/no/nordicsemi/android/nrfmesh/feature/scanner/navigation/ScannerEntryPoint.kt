package no.nordicsemi.android.nrfmesh.feature.scanner.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.navigation.ScannerKey
import no.nordicsemi.android.nrfmesh.core.ui.BottomSheetSceneStrategy
import no.nordicsemi.android.nrfmesh.feature.scanner.ScannerScreen
import no.nordicsemi.android.nrfmesh.feature.scanner.ScannerViewModel
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
fun EntryProviderScope<NavKey>.scannerEntry(navigator: Navigator) {
    entry<ScannerKey>(
        metadata = BottomSheetSceneStrategy.bottomSheet(
            modalBottomSheetProperties = ModalBottomSheetProperties()
        )
    ) {
        val viewModel = hiltViewModel<ScannerViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ScannerScreen(
            networkState = uiState.meshNetworkState,
            uuid = it.uuid,
            onScanResultSelected = {
                viewModel.connect(result = it)
                navigator.goBack()
            }
        )
    }
}