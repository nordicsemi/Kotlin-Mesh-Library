package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
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
import no.nordicsemi.kotlin.ble.client.RemoteServices
import no.nordicsemi.kotlin.mesh.core.ProxyFilterState
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppBind
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import javax.inject.Inject

@HiltViewModel
internal class Page1ViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
    private val centralManager: CentralManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(Page1ScreenUiState())
    internal val uiState = _uiState.asStateFlow()
    private lateinit var network: MeshNetwork

    init {
        observeNetwork()
        observeProxyConnectionState()
        observeProxyFilterState()
        observeSmpState()
    }

    private fun observeSmpState() {
        repository.getPeripheral()
            ?.services()
            // Only the discovered state carries services.
            ?.filterIsInstance<RemoteServices.Discovered>()
            ?.onEach { discovered ->
                val service = discovered.services.firstOrNull { it.uuid == SMP_SERVICE }
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

    @Suppress("UNUSED_PARAMETER")
    internal fun startPairing(context: Context) = repository.startPairing()

    internal suspend fun send(
        model: Model,
        message: AcknowledgedMeshMessage,
    ): MeshMessage? {
        bindApplicationKeyIfNeeded(model = model)
        return repository.send(model = model, ackedMessage = message)
    }

    /**
     * Binds an application key to the model, if the model has no application key bound, as a model
     * can only be used with a bound key.
     *
     * The first application key known to the node is used. If the node does not know any, the first
     * application key of the network is added to the node first.
     *
     * @throws IllegalStateException if the key could not be added or bound.
     */
    private suspend fun bindApplicationKeyIfNeeded(model: Model) {
        if (model.boundApplicationKeys.isNotEmpty()) return
        val node = model.parentElement?.parentNode
            ?: throw IllegalStateException("Node not found")
        val key = node.applicationKeys.firstOrNull() ?: addApplicationKey(node = node)

internal data class Page1ScreenUiState(
    val proxyConnectionState: ProxyConnectionState = ProxyConnectionState(),
    val messageState: MessageState = NotStarted,
    val name: String? = null,
    val unicastAddress: UnicastAddress? = null,
    val node: Node? = null,
    val isBonded: Boolean? = false,
)    /**
     * Sends the message to the model, binding an application key to it first, if none is bound.
     */
        val response = repository.send(
            node = node,
            message = ConfigModelAppBind(model = model, applicationKey = key)
        )
        if ((response as? ConfigStatusMessage)?.isSuccess != true || !model.isBound(key = key)) {
            throw IllegalStateException("Binding ${key.name} to ${model.name ?: "the model"} failed")
        }
    }

    /**
     * Adds the first application key of the network to the node.
     *
     * @return The key that was added.
     * @throws IllegalStateException if there is no key to add, or it could not be added.
     */
    private suspend fun addApplicationKey(node: Node): ApplicationKey {
        val key = repository.meshNetwork?.applicationKeys?.firstOrNull()
            ?: throw IllegalStateException("The network has no application keys")
        // An application key can only be added if its network key is known by the node.
        if (!node.knows(key = key.boundNetworkKey)) {
            throw IllegalStateException("${node.name} does not know the network key of ${key.name}")
        }
        val response = repository.send(node = node, message = ConfigAppKeyAdd(key = key))
        if ((response as? ConfigStatusMessage)?.isSuccess != true || !node.knows(key = key)) {
            throw IllegalStateException("Adding ${key.name} to ${node.name} failed")
        }
        return key
    }
}
