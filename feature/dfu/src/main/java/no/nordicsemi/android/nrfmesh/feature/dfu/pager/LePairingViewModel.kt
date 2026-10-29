package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import no.nordicsemi.android.nrfmesh.core.common.lePairingResponder
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.model.vendor.lepairing.message.PairingRequest
import no.nordicsemi.android.nrfmesh.core.data.model.vendor.lepairing.message.PairingResponse
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppBind
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Time given to the connection to complete before assuming that the device is not bonded yet and
 * that the pass key has to be entered by the user.
 */
private val BONDED_CONNECTION_DELAY = 500.milliseconds

@HiltViewModel
internal class LePairingViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LePairingScreenUiState())
    internal val uiState = _uiState.asStateFlow()
    private var pairingJob: Job? = null

    /**
     * Pairs with the connected proxy.
     *
     * The Mcu Manager transport is connected first, which requires an encrypted link. The bond
     * state of the phone is not used, as a bond that is stored on the phone may be stale.
     *
     * If the connection completes within [BONDED_CONNECTION_DELAY] the device was bonded already
     * and the state becomes [PairingState.Paired]. Otherwise, the device is waiting for the pass
     * key to be entered, so a [PairingRequest] is sent to the LE Pairing Responder model, if the
     * node has one, and the pass key is shown for the user to enter in the pairing dialog. The
     * connection is then awaited with no timeout.
     *
     * Nothing is done if the pairing is already in progress.
     */
    internal fun pair() {
        if (pairingJob?.isActive == true) return
        // Updated before launching, so that a previous result is never seen when the page is shown.
        _uiState.update { it.copy(passKey = null, state = PairingState.Connecting) }
        pairingJob = viewModelScope.launch {
            val connection = async {
                try {
                    repository.connectMcuMgrTransport()
                    Result.success(Unit)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            // Waiting for the result does not cancel the connection if the delay expires.
            val result = withTimeoutOrNull(BONDED_CONNECTION_DELAY) { connection.await() }
                ?: run {
                    // The device is not bonded, the user has to enter the pass key in the pairing
                    // dialog of the system.
                    _uiState.update { it.copy(state = PairingState.RequestingPassKey) }
                    try {
                        requestPassKey()?.let { passKey ->
                            _uiState.update { it.copy(passKey = passKey) }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // The connection is canceled so that a retry starts a new one.
                        connection.cancel()
                        _uiState.update { it.copy(state = PairingState.RequestFailed(error = e)) }
                        return@launch
                    }
                    _uiState.update { it.copy(state = PairingState.Connecting) }
                    connection.await()
                }
            _uiState.update {
                it.copy(
                    state = result.fold(
                        onSuccess = { PairingState.Paired },
                        onFailure = { error -> PairingState.ConnectionFailed(error = error) }
                    )
                )
            }
        }
    }

    /**
     * Requests the pass key from the LE Pairing Responder model of the connected proxy.
     *
     * @return The pass key, or null if the node does not have the model.
     * @throws IllegalStateException if no response was received, or the request was rejected.
     */
    private suspend fun requestPassKey(): Int? {
        val model = repository.proxyFilter.proxy?.model(modelId = lePairingResponder) ?: return null
        val response = send(model = model, message = PairingRequest()) as? PairingResponse
            ?: throw IllegalStateException("No response received")
        if (response.status != 0.toUByte()) {
            throw IllegalStateException("Pairing request failed, status ${response.status}")
        }
        return response.passKey
    }

    /**
     * Sends the message to the model, binding an application key to it first, if none is bound.
     */
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

internal data class LePairingScreenUiState(
    val passKey: Int? = null,
    val state: PairingState = PairingState.Idle,
)

/** The state of the pairing with the proxy. */
internal sealed interface PairingState {
    /** Nothing is in progress. */
    data object Idle : PairingState

    /** The pass key is requested from the LE Pairing Responder. */
    data object RequestingPassKey : PairingState

    /** The Mcu Manager transport is connecting, which pairs the devices if they are not paired. */
    data object Connecting : PairingState

    /** The connection with an encrypted link was established. */
    data object Paired : PairingState

    /** The pair request was not answered or was rejected. */
    data class RequestFailed(val error: Throwable) : PairingState

    /** The connection failed, e.g. due to an invalid key or a stale bond. */
    data class ConnectionFailed(val error: Throwable) : PairingState

    val isInProgress: Boolean
        get() = this is RequestingPassKey || this is Connecting
}
