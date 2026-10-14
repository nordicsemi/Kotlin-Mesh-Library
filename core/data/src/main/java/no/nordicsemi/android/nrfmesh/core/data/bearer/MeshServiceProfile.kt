package no.nordicsemi.android.nrfmesh.core.data.bearer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.kotlin.ble.client.Peripheral
import no.nordicsemi.kotlin.ble.client.Profile
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.ble.client.RemoteServices
import no.nordicsemi.kotlin.mesh.bearer.gatt.utils.MeshProvisioningService
import no.nordicsemi.kotlin.mesh.bearer.gatt.utils.MeshProxyService
import no.nordicsemi.kotlin.mesh.bearer.gatt.utils.MeshService
import kotlin.uuid.ExperimentalUuidApi

/**
 * Base class of the profiles of the mesh GATT services, that is the Mesh Proxy and the Mesh
 * Provisioning service, which only differ by their UUIDs.
 *
 * Once the service has been discovered, the profile subscribes to the Data Out characteristic and
 * forwards every notification to [onNotification]. The Data In characteristic is then available
 * for sending the data, until the service is invalidated or the device disconnects.
 *
 * @param meshService    The mesh service the profile is attached to.
 * @param name           Name of the profile, used in the logs of the BLE library.
 * @param onNotification Invoked with the value of every notification of the Data Out characteristic.
 */
@OptIn(ExperimentalUuidApi::class)
abstract class MeshServiceProfile(
    private val meshService: MeshService,
    name: String,
    private val onNotification: suspend (ByteArray) -> Unit,
) : Profile.Simple(serviceUuid = meshService.uuid, name = name) {

    /** The state of the mesh service on the device the profile is attached to. */
    sealed interface State {
        /** The service is not ready, yet, or it is not available anymore. */
        data object Closed : State

        /** The service is ready, data can be sent and notifications are received. */
        data object Open : State

        /** The service is not available or cannot be used. */
        data class Failed(val error: Throwable) : State
    }

    private val _state = MutableStateFlow<State>(State.Closed)

    /** The state of the mesh service. */
    val state: StateFlow<State> = _state.asStateFlow()

    /** The Data In characteristic used to send data, or null if the service is not open. */
    @Volatile
    var dataIn: RemoteCharacteristic? = null
        private set

    private lateinit var dataInCharacteristic: RemoteCharacteristic
    private lateinit var dataOutCharacteristic: RemoteCharacteristic

    override fun prepare(peripheral: Peripheral<*, *>, service: RemoteService) {
        // Throwing NoSuchElementException is treated by the library as the service not being
        // supported.
        dataInCharacteristic = service.characteristics.first { it.uuid == meshService.dataInUuid }
        dataOutCharacteristic = service.characteristics.first { it.uuid == meshService.dataOutUuid }
    }

    override suspend fun CoroutineScope.initialize(peripheral: Peripheral<*, *>) {
        val subscribed = CompletableDeferred<Unit>()
        // Subscribing enables the notifications when the flow is collected, so no notification
        // is missed. The flow completes when the device disconnects or the service is invalidated.
        val notifications = dataOutCharacteristic
            .subscribe { subscribed.complete(Unit) }
            // Fails if subscribing is not permitted, i.e. OperationFailedException.
            .catch { subscribed.completeExceptionally(it) }
            .onEach { onNotification(it) }
            .launchIn(this)
        try {
            // Make sure the notifications are enabled before reporting the service as open.
            subscribed.await()
            dataIn = dataInCharacteristic
            _state.value = State.Open
            // Hold the service open until the device disconnects, or its services get invalidated.
            notifications.join()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = State.Failed(e)
        } finally {
            dataIn = null
            if (_state.value is State.Open) {
                _state.value = State.Closed
            }
        }
    }

    override suspend fun unsupported(peripheral: Peripheral<*, *>) {
        _state.value = State.Failed(IllegalStateException("Mesh service not supported"))
    }

    override suspend fun failed(
        peripheral: Peripheral<*, *>,
        reason: RemoteServices.Failed.Reason,
    ) {
        _state.value = State.Failed(IllegalStateException("Service discovery failed: $reason"))
    }

    /**
     * Resets the state, so that a result of a previous connection is not reported again.
     */
    fun reset() {
        _state.value = State.Closed
    }

    /**
     * Suspends until the service is open.
     *
     * @throws Throwable if the service is not supported or could not be opened.
     */
    suspend fun awaitOpen() {
        val state = state.first { it !is State.Closed }
        if (state is State.Failed) throw state.error
    }
}

/**
 * Profile of the Mesh Proxy service, used to exchange network PDUs, mesh beacons and proxy
 * configuration messages with a GATT proxy node.
 *
 * @param onNotification Invoked with the value of every notification of the Data Out characteristic.
 */
class MeshProxyProfile(
    onNotification: suspend (ByteArray) -> Unit,
) : MeshServiceProfile(
    meshService = MeshProxyService,
    name = "Mesh Proxy",
    onNotification = onNotification
)

/**
 * Profile of the Mesh Provisioning service, used to exchange provisioning PDUs with an
 * unprovisioned device over PB-GATT.
 *
 * @param onNotification Invoked with the value of every notification of the Data Out characteristic.
 */
class MeshProvisioningProfile(
    onNotification: suspend (ByteArray) -> Unit,
) : MeshServiceProfile(
    meshService = MeshProvisioningService,
    name = "Mesh Provisioning",
    onNotification = onNotification
)
