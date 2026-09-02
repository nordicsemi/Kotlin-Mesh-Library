package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.runtime.mcumgr.McuMgrTransport
import io.runtime.mcumgr.ble.McuMgrBleTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository.Companion.SMP_SERVICE
import no.nordicsemi.android.nrfmesh.core.data.NetworkConnectionState
import no.nordicsemi.android.nrfmesh.core.data.ProxyConnectionState
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.mesh.core.ProxyFilterState
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.LogLevel
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi

@HiltViewModel
internal class Page1ViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
    private val centralManager: CentralManager
) : ViewModel(), McuMgrTransport.ConnectionCallback {
    private val _uiState = MutableStateFlow(Page1ScreenUiState())
    internal val uiState = _uiState.asStateFlow()
    private lateinit var network: MeshNetwork
    private var transport: McuMgrBleTransport? = null

    init {
        observeNetwork()
        observeProxyConnectionState()
        observeProxyFilterState()
        observeSmpState()
    }

    override fun onConnected() {
        repository.logger.log(
            message = { "McuManagerTransport connected" },
            category = LogCategory.MODEL,
            level = LogLevel.APPLICATION
        )
    }

    override fun onDeferred() {
        repository.logger.log(
            message = { "McuManagerTransport deferred" },
            category = LogCategory.MODEL,
            level = LogLevel.APPLICATION
        )
    }

    override fun onError(throwable: Throwable) {
        repository.logger.log(
            message = { "Error in McuManagerTransport in LePairingResponser: ${throwable.message}" },
            category = LogCategory.MODEL,
            level = LogLevel.APPLICATION
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun observeSmpState() {
        repository.getPeripheral()
            ?.services()
            ?.filterNotNull()
            ?.onEach { services ->
                val service = services.firstOrNull { it.uuid == SMP_SERVICE }
                if (service != null) {
                    _uiState.update { state ->
                        state.copy(
                            isBonded = centralManager
                                .getBondedPeripherals()
                                .any { p -> p.identifier == repository.identifier }
                        )
                    }
                }
            }?.launchIn(scope = viewModelScope)
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
                // We update the state here because the proxy state updates confirm that the node is
                // connected and ready to send messages
                when (filterState) {
                    is ProxyFilterState.ProxyFilterUpdateAcknowledged,
                    is ProxyFilterState.ProxyFilterLimitReached,
                        -> {
                        val isBonded = centralManager.getBondedPeripherals()
                            .any { p -> p.identifier == repository.identifier }
                        _uiState.update { state ->
                            state.copy(
                                node = repository.proxyFilter.proxy,
                                unicastAddress = repository.proxyFilter.proxy?.primaryUnicastAddress,
                                isBonded = isBonded
                            )
                        }
                    }

                    else -> {

                    }
                }
            }
            .launchIn(scope = viewModelScope)
    }

    @OptIn(ExperimentalUuidApi::class)
    internal fun startPairing(context: Context) {
        val identifier = repository.identifier ?: return
        val manager = ContextCompat.getSystemService(
            context,
            BluetoothManager::class.java
        ) as BluetoothManager
        val device = manager.adapter.getRemoteDevice(identifier)
        if (transport == null || transport?.isConnected == false) {
            val transport = McuMgrBleTransport(context, device)
                .also { transport = it }
            transport.connect(this)
        }
    }

    internal suspend fun send(
        model: Model,
        message: AcknowledgedMeshMessage,
    ) = repository.send(model = model, ackedMessage = message)
}

internal data class Page1ScreenUiState(
    val proxyConnectionState: ProxyConnectionState = ProxyConnectionState(),
    val messageState: MessageState = NotStarted,
    val name: String? = null,
    val unicastAddress: UnicastAddress? = null,
    val node: Node? = null,
    val isBonded: Boolean? = false,
)