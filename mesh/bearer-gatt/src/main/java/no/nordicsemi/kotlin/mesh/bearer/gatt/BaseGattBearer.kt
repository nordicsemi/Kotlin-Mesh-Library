@file:Suppress("unused", "PropertyName")

package no.nordicsemi.kotlin.mesh.bearer.gatt

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.suspendCancellableCoroutine
import no.nordicsemi.kotlin.ble.client.CentralManager
import no.nordicsemi.kotlin.ble.client.Peripheral
import no.nordicsemi.kotlin.ble.client.RemoteCharacteristic
import no.nordicsemi.kotlin.ble.client.RemoteService
import no.nordicsemi.kotlin.ble.client.ScanResult
import no.nordicsemi.kotlin.ble.core.WriteType
import no.nordicsemi.kotlin.mesh.bearer.BearerError
import no.nordicsemi.kotlin.mesh.bearer.BearerEvent
import no.nordicsemi.kotlin.mesh.bearer.Pdu
import no.nordicsemi.kotlin.mesh.bearer.PduType
import no.nordicsemi.kotlin.mesh.bearer.PduTypes
import no.nordicsemi.kotlin.mesh.bearer.ProxyProtocolHandler
import no.nordicsemi.kotlin.mesh.bearer.gatt.utils.MeshService
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.uuid.ExperimentalUuidApi

/**
 * Base implementation of the GATT Proxy Bearer.
 *
 * @property state             Flow that emits events whenever the bearer state changes.
 * @property pdus              Flow that emits events whenever a PDU is received.
 * @property supportedTypes    List of supported PDU types.
 * @property logger            Logger receives logs sent from the bearer. The logs will contain raw
 *                             data of sent and received packets, as well as connection events.
 * @property isOpen            Returns true if the bearer is open, false otherwise.
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
    private val scope = CoroutineScope(context = SupervisorJob() + ioDispatcher)
    private val _pdus = MutableSharedFlow<Pdu>()
    override val pdus: Flow<Pdu> = _pdus.asSharedFlow()
    private val _state = MutableStateFlow<BearerEvent>(BearerEvent.Closed(BearerError.Closed()))
    override val state: StateFlow<BearerEvent> = _state.asStateFlow()
    override val supportedTypes: Array<PduTypes> = arrayOf(
        PduTypes.NetworkPdu,
        PduTypes.MeshBeacon,
        PduTypes.ProxyConfiguration,
        PduTypes.ProvisioningPdu
    )
    override var isOpen: Boolean = false
        internal set
    protected var mtu: Int = DEFAULT_MTU
    override val name: String?
        get() = peripheral.name

    private val proxyProtocolHandler = ProxyProtocolHandler()
    protected var dataInCharacteristic: RemoteCharacteristic? = null

    var logger: Logger? = null
    private var servicesObserver: Job? = null

    protected abstract val meshService: MeshService

    /**
     * Configures the GATT services and characteristics required for the bearer to operate.
     *
     * @param services List of remote services discovered on the peripheral.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun configureGatt(services: List<RemoteService>?) {
        var dataInChar: RemoteCharacteristic? = null
        var dataOutChar: RemoteCharacteristic? = null
        services?.forEach { service ->
            service.characteristics.forEach { characteristic ->
                when (characteristic.uuid) {
                    meshService.dataInUuid -> dataInChar = characteristic
                    meshService.dataOutUuid -> dataOutChar = characteristic
                }
            }
        }
        when {
            dataInChar != null && dataOutChar != null -> {
                dataInCharacteristic = dataInChar
                // Marks device as ready
                onOpened()
                subscribe(dataOutCharacteristic = dataOutChar)
            }

            else -> {
                // Subscription for dataOutCharacteristic may cancel automatically in case the
                // services gets invalidated.
                dataInCharacteristic = null
                onClosed()
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun open() {
        // Avoid multiple opens if already observing services
        if (servicesObserver != null) return
        // Observe the connection state
        centralManager.connect(peripheral = peripheral)
        configurePeripheral(peripheral)
        suspendCancellableCoroutine { continuation ->
            var suspended = true
            // Start observing the discovered services
            servicesObserver = peripheral.services()
                .onEach { services ->
                    configureGatt(services = services)
                    if (suspended && services != null) {
                        suspended = false
                        when (isOpen) {
                            true -> continuation.resume(Unit)
                            false -> continuation.resumeWithException(BearerError.Closed())
                        }
                    }
                }
                .onCompletion {
                    // Let's clear the reference to the observer after cancellation
                    servicesObserver = null
                    logger?.v(LogCategory.BEARER) { "Service observer completed. Setting bearer state to closed." }
                    _state.value = BearerEvent.Closed(error = BearerError.Closed())
                }
                .launchIn(scope = scope)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    protected open suspend fun configurePeripheral(peripheral: P) {
        // Empty
    }

    override suspend fun close() {
        onClosed()
        peripheral.disconnect()
    }

    /**
     * Invoked when the bearer is opened
     */
    private fun onOpened() {
        isOpen = true
        _state.value = BearerEvent.Opened
        logger?.v(LogCategory.BEARER) { "Bearer opened" }
    }


    /**
     * Invoked when the bearer is closed
     */
    private fun onClosed() {
        if (isOpen) {
            isOpen = false
            servicesObserver?.cancel()
            // Following line has been moved to the service observer flow onCompletion
            // _state.value = BearerEvent.Closed(error = BearerError.Closed())
            logger?.v(LogCategory.BEARER) { "Bearer closed" }
        }
    }

    override suspend fun send(pdu: ByteArray, type: PduType) {
        require(supports(type = type)) { throw BearerError.PduTypeNotSupported() }
        require(isOpen) { throw BearerError.Closed() }

        proxyProtocolHandler.segment(data = pdu, type = type, mtu = mtu)
            .forEach {
                dataInCharacteristic
                    ?.also {
                        logger?.v(LogCategory.BEARER) {
                            "-> ${pdu.toHexString(format = HexFormat { number.prefix = "0x"; upperCase = true })}"
                        }
                    }
                    ?.write(data = it, writeType = WriteType.WITHOUT_RESPONSE)
                    ?: run {
                        logger?.e(category = LogCategory.BEARER) {
                            "Error: dataInCharacteristic is null"
                        }
                    }
            }
    }

    /**
     * Subscribes to the Data Out characteristic to receive notifications.
     *
     * @param dataOutCharacteristic The Data Out characteristic to subscribe to.
     */
    protected suspend fun subscribe(dataOutCharacteristic: RemoteCharacteristic) {
        // Call subscribe first before setting notifying to avoid missing packets
        dataOutCharacteristic.subscribe()
            .onEach {
                logger?.v(LogCategory.BEARER) {
                    "<- ${it.toHexString(format = HexFormat { number.prefix = "0x"; upperCase = true })}"
                }
                proxyProtocolHandler
                    .reassemble(data = it)
                    ?.let { pdu -> _pdus.emit(pdu) }
            }
            .onCompletion {
                logger?.v(LogCategory.BEARER) {
                    "Unsubscribed from: $dataOutCharacteristic"
                }
            }
            .launchIn(scope = scope)
        dataOutCharacteristic.setNotifying(enabled = true)
    }

    companion object {
        const val DEFAULT_MTU = 23 - 3
    }
}