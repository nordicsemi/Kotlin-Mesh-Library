package no.nordicsemi.android.nrfmesh.feature.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.ProxyConnectionState
import no.nordicsemi.kotlin.ble.client.android.ScanResult
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import javax.inject.Inject

@HiltViewModel
internal class ScannerViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
) : ViewModel() {
    private lateinit var meshNetwork: MeshNetwork
    private val _uiState = MutableStateFlow(ScannerScreenUiState())
    internal val uiState = _uiState.asStateFlow()

    init {
        observeNetwork()
    }

    private fun observeNetwork() {
        repository.networkEvents
            .mapNotNull { repository.meshNetwork }
            .onEach { network ->
                meshNetwork = network
                _uiState.update {
                    it.copy(meshNetworkState = MeshNetworkState.Success(network = network))
                }
            }
            .launchIn(scope = viewModelScope)

        repository.proxyConnectionStateFlow
            .onEach { proxyConnectionState ->
                _uiState.update { it.copy(proxyConnectionState = proxyConnectionState) }
            }
            .launchIn(scope = viewModelScope)
    }

    internal fun onGattProxySelected(scanResult: ScanResult) {
        repository.changeProxy(peripheral = scanResult.peripheral)
    }
}

internal sealed interface MeshNetworkState {

    data object Loading : MeshNetworkState

    data class Success(val network: MeshNetwork) : MeshNetworkState

}

internal data class ScannerScreenUiState(
    val meshNetworkState: MeshNetworkState = MeshNetworkState.Loading,
    val proxyConnectionState: ProxyConnectionState = ProxyConnectionState(),
    val isProxyLimitReached: Boolean = false,
    val messageState: MessageState = NotStarted,
)