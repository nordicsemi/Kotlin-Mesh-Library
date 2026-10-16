package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.common.Sending
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.ProxyConnectionState
import no.nordicsemi.android.nrfmesh.feature.dfu.util.DfuParameters
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdatePolicy
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.TransferMode
import no.nordicsemi.kotlin.mesh.core.model.DistributionMulticastAddress
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Node
import javax.inject.Inject

@HiltViewModel
internal class Page3ViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
) : ViewModel() {
    internal val uiState: StateFlow<Page3ScreenUiState>
        field = MutableStateFlow(Page3ScreenUiState())
    private lateinit var network: MeshNetwork

    init {
        observeNetwork()
    }

    private fun observeNetwork() {
        repository.networkEvents
            .mapNotNull { repository.meshNetwork }
            .onEach {
                network = it
                uiState.update { state ->
                    state.copy(groups = network.groups)
                }
            }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Invoked when the TTL is changed
     *
     * @param ttl TTL
     */
    internal fun onTtlChanged(ttl: UByte) {
        uiState.update { it.copy(dfuParameters = it.dfuParameters.copy(ttl = ttl)) }
    }

    /**
     * Invoked when the timeout is changed
     *
     * @param timeout timeout
     */
    internal fun onTimeoutChanged(timeout: UShort) {
        uiState.update { it.copy(dfuParameters = it.dfuParameters.copy(timeoutBase = timeout)) }
    }

    /**
     * Invoked when the transfer mode is changed
     *
     * @param transferMode Transfer mode
     */
    internal fun onTransferModeChanged(transferMode: TransferMode) {
        uiState.update { it.copy(dfuParameters = it.dfuParameters.copy(mode = transferMode)) }
    }

    /**
     * Invoked when the update policy is changed
     *
     * @param policy Update policy
     */
    internal fun onUpdatePolicyChanged(policy: FirmwareUpdatePolicy) {
        uiState.update { it.copy(dfuParameters = it.dfuParameters.copy(policy = policy)) }
    }

    /**
     * Invoked when the multicast address is changed
     *
     * @param address Selected multicast address
     */
    internal fun onMulticastAddressChanged(address: DistributionMulticastAddress) {
        uiState.update { it.copy(dfuParameters = it.dfuParameters.copy(address = address)) }
    }

    /**
     * Sends a message to the given model
     *
     * @param node              Node to send the message to
     * @param message           Message to send
     * @return Response from the model or null if the message times out or no response was received
     */
    private suspend fun send(node: Node, message: AcknowledgedConfigMessage): MeshMessage? {
        uiState.value = uiState.value.copy(messageState = Sending(message = message))
        return try {
            repository.send(node, message)?.let { response ->
                uiState.value = uiState.value.copy(
                    messageState = Completed(
                        message = message,
                        response = response as ConfigResponse
                    ),
                )
                response
            } ?: run {
                uiState.value = uiState.value.copy(
                    messageState = Failed(
                        message = message,
                        error = IllegalStateException("No response received")
                    ),
                )
                null
            }
        } catch (e: Exception) {
            uiState.value = uiState.value.copy(
                messageState = Failed(message = message, error = e),
            )
            null
        }
    }
}

internal data class Page3ScreenUiState(
    val proxyConnectionState: ProxyConnectionState = ProxyConnectionState(),
    val messageState: MessageState = NotStarted,
    val dfuParameters: DfuParameters = DfuParameters(),
    val groups: List<Group> = emptyList(),
)