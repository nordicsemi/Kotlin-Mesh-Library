package no.nordicsemi.android.nrfmesh.feature.dfu.pager

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
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.common.Sending
import no.nordicsemi.android.nrfmesh.core.common.firmwareDistributionServer
import no.nordicsemi.android.nrfmesh.core.common.lePairingResponder
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository.Companion.SMP_SERVICE
import no.nordicsemi.android.nrfmesh.core.data.NetworkConnectionState
import no.nordicsemi.android.nrfmesh.core.data.ProxyConnectionState
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.mesh.core.ProxyFilterState
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigModelAppList
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigSigModelAppGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigVendorModelAppGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCapabilitiesGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCapabilitiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionStatus
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi

@HiltViewModel
internal class Page0ViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
    private val centralManager: CentralManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(Page0ScreenUiState())
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
            .onEach {
                network = it
                val selectedKey = uiState.value.selectedKey ?: repository.proxyFilter.proxy
                    ?.model(modelId = firmwareDistributionServer)
                    ?.boundApplicationKeys
                    ?.firstOrNull()
                _uiState.update { state ->
                    state.copy(selectedKey = selectedKey)
                }
            }
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
                        },
                        isBonded = when (state) {
                            is NetworkConnectionState.Connected -> centralManager
                                .getBondedPeripherals()
                                .any { p -> p.identifier == repository.identifier }

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
                    is ProxyFilterState.ProxyFilterLimitReached,
                        -> {
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
                                isProxyReady = true,
                            )
                        }
                        repository.proxyFilter.proxy
                            ?.model(modelId = firmwareDistributionServer)
                            ?.let { model ->
                                try {
                                    val message = when (model.modelId) {
                                        is SigModelId -> ConfigSigModelAppGet(
                                            modelId = model.modelId as SigModelId,
                                            elementAddress = model.parentElement?.unicastAddress
                                                ?: throw IllegalStateException("Element not found")
                                        )

                                        is VendorModelId -> ConfigVendorModelAppGet(
                                            modelId = model.modelId as VendorModelId,
                                            elementAddress = model.parentElement?.unicastAddress
                                                ?: throw IllegalStateException("Element not found")
                                        )
                                    }
                                    val status = send(
                                        model = model,
                                        message = message
                                    ) as? ConfigModelAppList
                                    status?.let { appKeyList ->
                                        if (appKeyList.applicationKeyIndexes.isNotEmpty()) {
                                            val key = model.boundApplicationKeys.firstOrNull()
                                            _uiState.update { it.copy(selectedKey = key) }
                                            if (_uiState.value.distributionStatus == null && key != null) {
                                                val distributionStatus = send(
                                                    model = model,
                                                    message = FirmwareDistributionGet()
                                                ) as? FirmwareDistributionStatus
                                                val capabilitiesStatus = send(
                                                    model = model,
                                                    message = FirmwareDistributionCapabilitiesGet()
                                                ) as? FirmwareDistributionCapabilitiesStatus
                                                _uiState.update { state ->
                                                    state.copy(
                                                        selectedKey = key,
                                                        capabilitiesStatus = capabilitiesStatus,
                                                        distributionStatus = distributionStatus
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    // val key = it.boundApplicationKeys.firstOrNull()
                                    // // _uiState.value = _uiState.value.copy(selectedKey = key)
                                    // if (_uiState.value.distributionStatus == null && key != null) {
                                    //     val distributionStatus = send(
                                    //         model = it,
                                    //         message = FirmwareDistributionGet()
                                    //     ) as? FirmwareDistributionStatus
                                    //     //_uiState.update { state -> state.copy(distributionStatus = distributionStatus) }
                                    //     val capabilitiesStatus = send(
                                    //         model = it,
                                    //         message = FirmwareDistributionCapabilitiesGet()
                                    //     ) as? FirmwareDistributionCapabilitiesStatus
                                    //     _uiState.update { state ->
                                    //         state.copy(
                                    //             selectedKey = key,
                                    //             capabilitiesStatus = capabilitiesStatus,
                                    //             distributionStatus = distributionStatus
                                    //         )
                                    //     }
                                    // }
                                } catch (e: Exception) {

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
        // Check if the selected key is bound to LE Pairing Responder
        val isBound = _uiState.value.node?.model(modelId = lePairingResponder)
            ?.boundApplicationKeys
            ?.any { it.index == key.index }
        _uiState.update { it.copy(selectedKey = key) }
    }

    internal suspend fun send(
        model: Model,
        message: AcknowledgedMeshMessage,
    ): MeshMessage? {
        return try {
            _uiState.value = _uiState.value.copy(messageState = Sending(message = message))
            val response = if (message is AcknowledgedConfigMessage) {
                val parentNode = model.parentElement?.parentNode
                    ?: throw IllegalStateException("Parent not found")
                repository.send(node = parentNode, message = message)
            } else {
                repository.send(model = model, ackedMessage = message)
            }
            response?.let {
                _uiState.value = _uiState.value.copy(messageState = Completed(message = message, response = it))
            } ?: run {
                _uiState.value = _uiState.value.copy(messageState = Failed(message = message, error = IllegalStateException("No response received")))
            }
            response
        } catch (e: Exception) {
            _uiState.value =
                _uiState.value.copy(messageState = Failed(message = message, error = e))
            null
        }
    }
}

internal data class Page0ScreenUiState(
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
    val distributionStatus: FirmwareDistributionStatus? = null,
    val capabilitiesStatus: FirmwareDistributionCapabilitiesStatus? = null,
    val isBonded: Boolean? = false,
)