package no.nordicsemi.android.nrfmesh.feature.dfu.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import no.nordicsemi.android.nrfmesh.core.navigation.FirmwareUpdateKey
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.navigation.NodesKey
import no.nordicsemi.android.nrfmesh.core.navigation.ScannerKey
import no.nordicsemi.android.nrfmesh.feature.dfu.FirmwareUpdateScreen
import no.nordicsemi.android.nrfmesh.feature.dfu.FirmwareUpdateViewModel
import no.nordicsemi.kotlin.mesh.bearer.gatt.utils.MeshProxyService
import kotlin.uuid.ExperimentalUuidApi

@OptIn(
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalUuidApi::class
)
fun EntryProviderScope<NavKey>.firmwareUpdateEntry(navigator: Navigator) {
    entry<FirmwareUpdateKey>(
        metadata = DialogSceneStrategy.dialog(
            dialogProperties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        )
    ) {
        val viewModel = hiltViewModel<FirmwareUpdateViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        FirmwareUpdateScreen(
            uiState = uiState,
            onGattProxyClicked = {
                navigator.navigate(key = ScannerKey(uuid = MeshProxyService.uuid))
            },
            onBindAppKeysClicked = {
                navigator.navigate()
            },
            onBackClick = { navigator.navigate(key = NodesKey) }
        )
    }
}