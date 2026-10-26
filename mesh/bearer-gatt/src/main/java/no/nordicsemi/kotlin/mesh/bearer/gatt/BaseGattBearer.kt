@file:Suppress("unused", "PropertyName")

package no.nordicsemi.kotlin.mesh.bearer.gatt

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nordicsemi.kotlin.ble.client.CentralManager
import no.nordicsemi.kotlin.ble.client.Peripheral
import no.nordicsemi.kotlin.ble.client.ScanResult
import no.nordicsemi.kotlin.ble.core.WriteType
import no.nordicsemi.kotlin.mesh.bearer.BearerError
import no.nordicsemi.kotlin.mesh.bearer.BearerEvent
import no.nordicsemi.kotlin.mesh.bearer.Pdu
import no.nordicsemi.kotlin.mesh.bearer.PduType
import no.nordicsemi.kotlin.mesh.bearer.ProxyProtocolHandler
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.Logger

/**
 * Base implementation of the GATT bearers, sending and receiving the mesh PDUs over the Mesh Proxy
 * or the Mesh Provisioning service.
 *
 * The service is handled by the [profile], which is attached to the peripheral when the bearer is
 * opened. The profile is not required, so a device without the service is not disconnected by the
 * BLE library. The bearer disconnects from it, instead, when it fails to open. Losing the
 * connection closes the bearer.
 *
 * @param centralManager Central manager used to connect to the peripheral.
 * @param peripheral     Peripheral to connect to.
 * @param ioDispatcher   Coroutine dispatcher used by the bearer.
 * @property state       Flow that emits events whenever the bearer state changes.
 * @property pdus        Flow that emits events whenever a PDU is received.
 * @property logger      Receives the logs of the bearer, containing the raw data of the packets
 *                       sent and received, as well as the connection events.
 * @property isOpen      Returns true if the bearer is open, false otherwise.
 */
abstract class BaseGattBearer<
        ID : Any,
        C : CentralManager<ID, P, EX, F, SR>,
        P : Peripheral<ID, EX>,
        EX : Peripheral.Executor<ID>,
        F : CentralManager.ScanFilterScope,
        SR : ScanResult<*, *>,
        >(
    protected val centralManager: C,
    protected val peripheral: P,
    ioDispatcher: CoroutineDispatcher,
) : GattBearer {
    protected abstract val profile: MeshServiceProfile
    private val scope = CoroutineScope(context = SupervisorJob() + ioDispatcher)
    private val _pdus = MutableSharedFlow<Pdu>()
    override val pdus: Flow<Pdu> = _pdus.asSharedFlow()
    private val _state = MutableStateFlow<BearerEvent>(BearerEvent.Closed(BearerError.Closed()))
    override val state: StateFlow<BearerEvent> = _state.asStateFlow()

    private var _isOpen = false
    override val isOpen: Boolean
        get() = _isOpen
    override val name: String?
        get() = peripheral.name

    var logger: Logger? = null

    protected var mtu: Int = DEFAULT_MTU
    private val proxyProtocolHandler = ProxyProtocolHandler()
    private var connection: Job? = null
    private var notificationsJob: Job? = null

    /**
     * Starts forwarding the notifications of the profile as PDUs, if it hasn't been started yet.
     *
     * This can't be done when the bearer is constructed, as the profile is provided by the
     * subclass and isn't initialized yet.
     */
    private fun startReceiving() {
        if (notificationsJob?.isActive == true) return
        // Undispatched, so that the bearer is collecting before the first notification arrives.
        notificationsJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            profile.notifications.collect { data ->
                logger?.v(LogCategory.BEARER) { "<- ${data.toLogString()}" }
                proxyProtocolHandler
                    .reassemble(data = data)
                    ?.let { pdu -> _pdus.emit(pdu) }
            }
        }
    }

    /**
     * Connects to the peripheral.
     *
     * Implementations may override this to connect using platform specific options.
     */
    protected open suspend fun connect() {
        centralManager.connect(peripheral = peripheral)
    }

    override suspend fun open() {
        // Avoid multiple opens if already connecting or connected.
        if (connection?.isActive == true) return

        startReceiving()
        profile.reset()
        val ready = CompletableDeferred<Unit>()
        connection = scope.launch {
            try {
                connect()
                // The profile is not required, a device without the mesh service is not
                // disconnected by the BLE library.
                peripheral.profile(scope = this, profile = profile, required = false)

                // Open the bearer when the service becomes ready. This runs as a child job, so
                // that the disconnection below is awaited from the moment the device connects.
                launch {
                    try {
                        profile.awaitOpen()
                        mtu = peripheral.maximumWriteValueLength(WriteType.WITHOUT_RESPONSE)
                        onOpened()
                        ready.complete(Unit)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger?.e(LogCategory.BEARER) { "Failed to open the bearer: $e" }
                        ready.completeExceptionally(BearerError.Closed())
                        // The profile is not required, so disconnect from the device here, which
                        // also ends the wait for the disconnection below.
                        val _ = runCatching { peripheral.disconnect() }
                    }
                }

                // Suspend until the device disconnects.
                peripheral.awaitDisconnection()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger?.e(LogCategory.BEARER) { "Connection failed: $e" }
            } finally {
                // The profile is registered in this job, so it would keep running after the
                // device disconnected, as a job only completes once its children have. Cancel it,
                // otherwise it is run again, next to the profile of the next bearer, as soon as
                // the services of the same peripheral are discovered.
                coroutineContext.cancelChildren()
                // Fail anyone still waiting for the bearer to open, this is a no-op if it is
                // already completed.
                ready.completeExceptionally(BearerError.Closed())
                onClosed()
                withContext(NonCancellable) {
                    runCatching { peripheral.disconnect() }
                }
            }
        }
        ready.await()
    }

    override suspend fun close() {
        connection?.cancelAndJoin()
        connection = null
        peripheral.disconnect()
        onClosed()
    }

    override suspend fun send(pdu: ByteArray, type: PduType) {
        require(supports(type = type)) { throw BearerError.PduTypeNotSupported() }
        require(isOpen) { throw BearerError.Closed() }

        val dataIn = profile.dataIn
        if (dataIn == null) {
            logger?.e(category = LogCategory.BEARER) { "Error: dataInCharacteristic is null" }
            return
        }
        proxyProtocolHandler.segment(data = pdu, type = type, mtu = mtu).forEach {
            logger?.v(LogCategory.BEARER) { "-> ${pdu.toLogString()}" }
            dataIn.write(data = it, writeType = WriteType.WITHOUT_RESPONSE)
        }
    }

    private fun onOpened() {
        _isOpen = true
        _state.value = BearerEvent.Opened
        logger?.v(LogCategory.BEARER) { "Bearer opened" }
    }

    private fun onClosed() {
        _isOpen = false
        _state.value = BearerEvent.Closed(error = BearerError.Closed())
        logger?.v(LogCategory.BEARER) { "Bearer closed" }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun ByteArray.toLogString() = toHexString(
        format = HexFormat {
            number.prefix = "0x"
            upperCase = true
        }
    )

    companion object {
        /** Default MTU, that is the default ATT MTU of 23 bytes minus the 3 bytes header. */
        const val DEFAULT_MTU = 23 - 3
    }
}
