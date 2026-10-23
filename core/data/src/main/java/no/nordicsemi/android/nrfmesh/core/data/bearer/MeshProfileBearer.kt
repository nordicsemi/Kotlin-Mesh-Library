package no.nordicsemi.android.nrfmesh.core.data.bearer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.core.WriteType
import no.nordicsemi.kotlin.mesh.bearer.BearerError
import no.nordicsemi.kotlin.mesh.bearer.BearerEvent
import no.nordicsemi.kotlin.mesh.bearer.Pdu
import no.nordicsemi.kotlin.mesh.bearer.PduType
import no.nordicsemi.kotlin.mesh.bearer.PduTypes
import no.nordicsemi.kotlin.mesh.bearer.ProxyProtocolHandler
import no.nordicsemi.kotlin.mesh.bearer.gatt.GattBearer
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.Logger

/**
 * Base class of the GATT bearers, sending and receiving the mesh PDUs over the Mesh Proxy or the
 * Mesh Provisioning service.
 *
 * The service is handled by a [MeshServiceProfile] attached to the peripheral. The profile is
 * required, so a device without the service is disconnected by the BLE library and the bearer is
 * closed. Losing the connection, or the service, closes the bearer as well.
 *
 * @param centralManager Central manager used to connect to the peripheral.
 * @param peripheral     Peripheral to connect to.
 * @param profileFactory Creates the profile of the mesh service of the peripheral, given the
 *                       callback receiving the notifications of the Data Out characteristic.
 * @param supportedTypes PDU types supported by the bearer.
 * @param ioDispatcher   Coroutine dispatcher used by the bearer.
 * @property logger      Receives the logs of the bearer, containing the raw data of the packets
 *                       sent and received, as well as the connection events.
 */
abstract class MeshProfileBearer(
    private val centralManager: CentralManager,
    protected val peripheral: Peripheral,
    profileFactory: (onNotification: suspend (ByteArray) -> Unit) -> MeshServiceProfile,
    final override val supportedTypes: Array<PduTypes>,
    ioDispatcher: CoroutineDispatcher,
) : GattBearer {
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

    private var mtu: Int = DEFAULT_MTU
    private val proxyProtocolHandler = ProxyProtocolHandler()
    private val profile = profileFactory { data ->
        logger?.v(LogCategory.BEARER) { "<- ${data.toLogString()}" }
        proxyProtocolHandler
            .reassemble(data = data)
            ?.let { pdu -> _pdus.emit(pdu) }
    }
    private var connection: Job? = null

    override suspend fun open() {
        // Avoid multiple opens if already connecting or connected.
        if (connection?.isActive == true) return

        profile.reset()
        val ready = CompletableDeferred<Unit>()
        connection = scope.launch {
            try {
                centralManager.connect(
                    peripheral = peripheral,
                    options = CentralManager.ConnectionOptions.Direct(
                        automaticallyRequestHighestValueLength = true
                    )
                )
                // The profile is required, a device without the mesh service is disconnected.
                peripheral.profile(scope = this, profile = profile, required = true)

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
                    }
                }

                // Suspend until the device disconnects.
                peripheral.awaitDisconnection()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger?.e(LogCategory.BEARER) { "Connection failed: $e" }
            } finally {
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
        logger?.v(LogCategory.BEARER) { "Mesh profile bearer closed" }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun ByteArray.toLogString() = toHexString(
        format = HexFormat {
            number.prefix = "0x"
            upperCase = true
        }
    )

    private companion object {
        /** Default MTU, that is the default ATT MTU of 23 bytes minus the 3 bytes header. */
        const val DEFAULT_MTU = 23 - 3
    }
}
