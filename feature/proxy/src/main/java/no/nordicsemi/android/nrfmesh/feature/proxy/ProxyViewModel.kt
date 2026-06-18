package no.nordicsemi.android.nrfmesh.feature.proxy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.common.Sending
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.ProxyConnectionState
import no.nordicsemi.kotlin.ble.client.android.ScanResult
import no.nordicsemi.kotlin.mesh.core.ProxyFilterState
import no.nordicsemi.kotlin.mesh.core.ProxyFilterType
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.proxy.ProxyConfigurationMessage
import no.nordicsemi.kotlin.mesh.core.messages.proxy.RemoveAddressesFromFilter
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.ProxyFilterAddress
import javax.inject.Inject

@HiltViewModel
internal class ProxyViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
) : ViewModel() {
    private lateinit var meshNetwork: MeshNetwork
    private val _uiState = MutableStateFlow(ProxyScreenUiState())
    internal val uiState = _uiState.asStateFlow()

    init {
        observeNetwork()
    }

    private fun observeNetwork() {
        repository.networkEvents
            .map { repository.meshNetwork }
            .filterNotNull()
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

        // Setup initial state
        _uiState.update {
            it.copy(
                filterType = repository.proxyFilter.type,
                addresses = repository.proxyFilter.addresses.toList(),
            )
        }

        repository.proxyFilter.proxyFilterStateFlow
            .onEach { state ->
                when (state) {
                    is ProxyFilterState.ProxyFilterUpdated -> {
                        val addresses = mutableListOf<ProxyFilterAddress>()
                        addresses.addAll(state.addresses)
                        _uiState.update {
                            it.copy(
                                filterType = state.type,
                                addresses = state.addresses.toList(),
                                isProxyLimitReached = false
                            )
                        }
                    }

                    is ProxyFilterState.ProxyFilterLimitReached -> {
                        _uiState.update {
                            it.copy(
                                filterType = state.type,
                                isProxyLimitReached = true
                            )
                        }
                    }

                    is ProxyFilterState.ProxyFilterUpdateAcknowledged -> {
                        _uiState.update {
                            it.copy(
                                filterType = state.type,
                                addresses = repository.proxyFilter.addresses.toList(),
                            )
                        }
                    }

                    ProxyFilterState.Unknown -> {

                    }
                }
            }
            .launchIn(scope = viewModelScope)
    }

    internal fun onAutoConnectToggled(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAutomaticConnection(enabled = enabled)
        }
    }

    internal fun connect(result: ScanResult) {
        repository.connect(peripheral = result.peripheral)
    }

    internal fun disconnect() {
        viewModelScope.launch {
            repository.toggleAutomaticConnection(enabled = false)
            // When disconnecting we should disable automatic connectivity
            // If not the app will reconnect again
            repository.disconnect()
        }
    }

    internal fun onBluetoothEnabled(enabled: Boolean) {
        if (enabled) {
            repository.onBluetoothEnabled()
        }
    }

    internal fun onLocationEnabled(enabled: Boolean) {
        if (enabled) {
            repository.onBluetoothEnabled()
        }
    }

    internal fun send(message: ProxyConfigurationMessage) {
        _uiState.update {
            it.copy(messageState = Sending(message = message))
        }
        viewModelScope.launch {
            try {
                repository.send(message).let { response ->
                    if (message is RemoveAddressesFromFilter) {
                        val addresses = _uiState.value.addresses.toMutableList()
                        if (addresses.removeAll(message.addresses)) {
                            _uiState.update {
                                it.copy(addresses = addresses.toList())
                            }
                        }
                    }
                    _uiState.update {
                        it.copy(
                            messageState = Completed(
                                message = message,
                                response = response as ConfigResponse
                            ),
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(messageState = Failed(message = message, error = e))
                }
            }
        }
    }

    internal fun resetMessageState() {
        _uiState.value = _uiState.value.copy(messageState = NotStarted, isProxyLimitReached = false)
    }
}

internal sealed interface MeshNetworkState {

    data object Loading : MeshNetworkState

    data class Success(val network: MeshNetwork) : MeshNetworkState

}

internal data class ProxyScreenUiState(
    val meshNetworkState: MeshNetworkState = MeshNetworkState.Loading,
    val proxyConnectionState: ProxyConnectionState = ProxyConnectionState(),
    val addresses: List<ProxyFilterAddress> = emptyList(),
    val filterType: ProxyFilterType? = null,
    val isProxyLimitReached: Boolean = false,
    val messageState: MessageState = NotStarted,
)