package no.nordicsemi.android.nrfmesh.feature.proxy.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.navigation.ProxyKey
import no.nordicsemi.android.nrfmesh.core.navigation.ScannerKey
import no.nordicsemi.android.nrfmesh.feature.proxy.ProxyScreen
import no.nordicsemi.android.nrfmesh.feature.proxy.ProxyViewModel
import no.nordicsemi.kotlin.mesh.bearer.gatt.utils.MeshProxyService
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
fun EntryProviderScope<NavKey>.proxyEntry(navigator: Navigator) {
    entry<ProxyKey> {
        val viewModel = hiltViewModel<ProxyViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ProxyScreen(
            uiState = uiState,
            onBluetoothEnabled = viewModel::onBluetoothEnabled,
            onLocationEnabled = viewModel::onLocationEnabled,
            onAutoConnectToggled = viewModel::onAutoConnectToggled,
            onConnectClicked = {
                navigator.navigate(key = ScannerKey(uuid = MeshProxyService.uuid))
            },
            onDisconnectClicked = viewModel::disconnect,
            onScanResultSelected = viewModel::connect,
            send = viewModel::send,
            resetMessageState = viewModel::resetMessageState,
        )
    }
}