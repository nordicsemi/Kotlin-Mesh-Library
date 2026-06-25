package no.nordicsemi.android.nrfmesh.feature.dfu.smp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.common.firmwareDistributionServer
import no.nordicsemi.android.nrfmesh.core.common.lePairingResponder
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository.Companion.SMP_SERVICE
import no.nordicsemi.android.nrfmesh.core.data.NetworkConnectionState
import no.nordicsemi.android.nrfmesh.core.data.ProxyConnectionState
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.mesh.core.ProxyFilterState
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionPhase
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionStatus
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi

@HiltViewModel
internal class SmpViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
    private val centralManager: CentralManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SmpScreenUiState())
    internal val uiState = _uiState.asStateFlow()
    private lateinit var network: MeshNetwork

    init {
        observeNetwork()
        observeProxyConnectionState()
        observeProxyFilterState()
    }


    @OptIn(ExperimentalUuidApi::class)
    private fun observeNetwork() {
        repository.networkEvents
            .mapNotNull { repository.meshNetwork }
            .onEach { network = it }
            .launchIn(scope = viewModelScope)
    }

    private fun observeProxyConnectionState() {
        repository.proxyConnectionStateFlow
            .onEach { proxyConnectionState ->
                val state = proxyConnectionState.connectionState
                _uiState.update {
                    it.copy(
                        proxyConnectionState = proxyConnectionState,
                        name = when (state) {
                            is NetworkConnectionState.Connecting -> state.name
                            is NetworkConnectionState.Connected -> state.name
                            NetworkConnectionState.Disconnected,
                            NetworkConnectionState.Scanning,
                                -> null
                        },
                        unicastAddress = repository.proxyFilter.proxy?.primaryUnicastAddress,
                        isLePairingSupported = when (state) {
                            is NetworkConnectionState.Connected ->
                                repository.proxyFilter.proxy?.let { proxy ->
                                    proxy.model(modelId = lePairingResponder) != null
                                }

                            else -> null
                        },
                        isDistributorServerModelSupported = when (state) {
                            is NetworkConnectionState.Connected -> repository.proxyFilter.proxy?.let { proxy ->
                                proxy.model(modelId = firmwareDistributionServer) != null
                            }

                            else -> null
                        },
                        selectedKey = when (state) {
                            is NetworkConnectionState.Connected -> repository.proxyFilter.proxy
                                ?.model(modelId = firmwareDistributionServer)
                                ?.boundApplicationKeys
                                ?.firstOrNull()

                            else -> null
                        },
                        isProxyReady = when (state) {
                            // Proxy is connected but not ready until proxy filter messages are acknowledged.
                            is NetworkConnectionState.Connected -> false
                            else -> null
                        }
                    )
                }
            }
            .launchIn(scope = viewModelScope)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun observeProxyFilterState() {
        repository.proxyFilter.proxyFilterStateFlow
            .onEach { filterState ->
                val services = repository.getPeripheral()
                    ?.services()
                    ?.filterNotNull()
                    ?.first()
                // We update the state here because the proxy state updates confirm that the node is
                // connected and ready to send messages
                when (filterState) {
                    is ProxyFilterState.ProxyFilterUpdateAcknowledged,
                    is ProxyFilterState.ProxyFilterLimitReached-> {
                        _uiState.update { state ->
                            state.copy(
                                node = repository.proxyFilter.proxy,
                                unicastAddress = repository.proxyFilter.proxy?.primaryUnicastAddress,
                                isSmpServiceSupported = services?.any { it.uuid == SMP_SERVICE },
                                isLePairingSupported = repository.proxyFilter.proxy
                                    ?.model(modelId = lePairingResponder) != null,
                                isDistributorServerModelSupported = repository.proxyFilter.proxy
                                    ?.model(modelId = firmwareDistributionServer) != null,
                                selectedKey = repository.proxyFilter.proxy
                                    ?.model(modelId = firmwareDistributionServer)
                                    ?.boundApplicationKeys
                                    ?.firstOrNull(),
                                isProxyReady = true
                            )
                        }

                        if(_uiState.value.phase == null) {
                            repository.proxyFilter.proxy
                                ?.model(modelId = firmwareDistributionServer)
                                ?.let {
                                    if(it.boundApplicationKeys.isNotEmpty()) {
                                        val status = send(it, FirmwareDistributionGet()) as? FirmwareDistributionStatus
                                        _uiState.update { state -> state.copy(phase = status?.phase) }
                                    }
                                }
                        }
                    }

                    else -> {

                    }
                }
            }
            .launchIn(scope = viewModelScope)
    }

    internal fun resetMessageState() {
        _uiState.value = _uiState.value.copy(messageState = NotStarted)
    }

    internal fun onApplicationKeyClicked(key: ApplicationKey) {
        _uiState.update { it.copy(selectedKey = key) }
    }

    internal fun onBindAppKeyClicked() {

    }

    internal suspend fun send(
        model: Model,
        message: AcknowledgedMeshMessage,
    ) = repository.send(model = model, ackedMessage = message)
}

internal data class SmpScreenUiState(
    val proxyConnectionState: ProxyConnectionState = ProxyConnectionState(),
    val messageState: MessageState = NotStarted,
    val name: String? = null,
    val unicastAddress: UnicastAddress? = null,
    val isSmpServiceSupported: Boolean? = null,
    val isLePairingSupported: Boolean? = null,
    val isDistributorServerModelSupported: Boolean? = null,
    val node: Node? = null,
    val selectedKey: ApplicationKey? = null,
    val isProxyReady: Boolean? = null,
    val phase: FirmwareDistributionPhase? = null
)