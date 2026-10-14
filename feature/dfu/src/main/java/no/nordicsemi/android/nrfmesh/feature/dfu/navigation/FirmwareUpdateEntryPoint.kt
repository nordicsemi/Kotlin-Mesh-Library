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
import no.nordicsemi.android.nrfmesh.feature.dfu.DfuSessionViewModel
import no.nordicsemi.android.nrfmesh.feature.dfu.pager.Page4ViewModel
import no.nordicsemi.android.nrfmesh.feature.dfu.FirmwareUpdateScreen
import no.nordicsemi.kotlin.mesh.bearer.gatt.utils.MeshProxyService

@OptIn(
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3Api::class
)
fun EntryProviderScope<NavKey>.firmwareUpdateEntry(navigator: Navigator) {
    var viewModel: DfuSessionViewModel? = null
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
        val sessionViewModel = hiltViewModel<DfuSessionViewModel>()
        val page4ViewModel = hiltViewModel<Page4ViewModel>()
        viewModel = sessionViewModel
        val uiState by sessionViewModel.uiState.collectAsStateWithLifecycle()
        val session by sessionViewModel.session.collectAsStateWithLifecycle()
        FirmwareUpdateScreen(
            uiState = uiState,
            session = session,
            onGattProxyClicked = {
                navigator.navigate(key = ScannerKey(uuid = MeshProxyService.uuid))
            },
            onBindAppKeysClicked = { navigator.navigate(key = DfuBindAppKeysKey(model = it)) },
            onKeyIndexChanged = { sessionViewModel.setKeyIndex(index = it) },
            onReceiversChanged = { sessionViewModel.setReceivers(receivers = it) },
            onUpdatePackageChanged = { sessionViewModel.setUpdatePackage(updatePackage = it) },
            onDfuParametersChanged = { sessionViewModel.setDfuParameters(dfuParameters = it) },
            onBackClick = { navigator.navigate(key = NodesKey) },
            onClearTasks = page4ViewModel::clear
        )
    }
    dfuBindAppKeysEntry(
        send = { model, message ->
            viewModel?.send(model = model, message = message)
        }
    )
}