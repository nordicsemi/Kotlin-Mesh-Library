@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.layers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nordicsemi.kotlin.mesh.bearer.MeshBearer
import no.nordicsemi.kotlin.mesh.bearer.PduType
import no.nordicsemi.kotlin.mesh.bearer.Transmitter
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import no.nordicsemi.kotlin.mesh.core.ProxyFilterEventHandler
import no.nordicsemi.kotlin.mesh.core.layers.access.AccessLayer
import no.nordicsemi.kotlin.mesh.core.layers.access.Busy
import no.nordicsemi.kotlin.mesh.core.layers.lowertransport.LowerTransportLayer
import no.nordicsemi.kotlin.mesh.core.layers.network.NetworkLayer
import no.nordicsemi.kotlin.mesh.core.layers.uppertransport.UpperTransportLayer
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.BaseMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.HasOpCode
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyDelete
import no.nordicsemi.kotlin.mesh.core.messages.proxy.ProxyConfigurationMessage
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.Logger
import kotlin.concurrent.timer
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * Network Manager contains the different layers of the mesh network architecture.
 *
 * @property manager Mesh network manager
 * @constructor Constructs the network manager.
 */
internal class NetworkManager internal constructor(
    private val manager: MeshNetworkManager,
) : NetworkManagerEventTransmitter {
    internal val scope: CoroutineScope = manager.scope
    internal var proxy: ProxyFilterEventHandler = manager.proxyFilter

    val logger: Logger?
        get() = manager.logger

    val securePropertiesStorage = manager.secureProperties
    internal var networkLayer = NetworkLayer(this)
        private set
    internal var lowerTransportLayer = LowerTransportLayer(this)
        private set
    internal var upperTransportLayer = UpperTransportLayer(this)
        private set
    internal var accessLayer = AccessLayer(this)
        private set

    var bearer: MeshBearer? = null
        internal set(value) {
            field = value
            awaitBearerPdus()
        }

    val meshNetwork: MeshNetwork
        get() = manager.network!!

    val networkParameters: NetworkParameters
        get() = manager.networkParameters
    private val mutex = Mutex()

    private var outgoingMessages = mutableSetOf<MeshAddress>()

    private val _incomingProxyMessages = MutableSharedFlow<ReceivedMessage>()
    internal val incomingProxyMessages
        get() = _incomingProxyMessages.asSharedFlow()

    private val _incomingMeshMessages = MutableSharedFlow<ReceivedMessage>()
    internal val incomingMeshMessages
        get() = _incomingMeshMessages.asSharedFlow()

    private val _networkManagerEventFlow = MutableSharedFlow<NetworkManagerEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val networkManagerEventFlow
        get() = _networkManagerEventFlow.asSharedFlow()

    private val ioScope = CoroutineScope(context = SupervisorJob() + manager.ioDispatcher)

    /**
     * Awaits and returns the mesh pdu received by the bearer.
     */
    private fun awaitBearerPdus() {
        bearer?.pdus
            ?.onEach {
                runCatching { handle(incomingPdu = it.data, type = it.type) }
                    .onFailure {throwable ->
                        logger?.e(LogCategory.BEARER) { "Bearer error: $throwable" }
                    }
            }?.launchIn(scope = scope)
    }

    /**
     * Emits the network manager event.
     *
     * @param event Network manager event.
     */
    override suspend fun emitNetworkManagerEvent(event: NetworkManagerEvent) {
        _networkManagerEventFlow.emit(value = event)
    }

    /**
     * Handles the received PDU of a given type.
     *
     * This method parses incoming Proxy messages and mesh messages and emits them to their
     * respective SharedFlow which are observed by the respective subscriber.
     *
     * @param incomingPdu Incoming PDU.
     * @param type        PDU type.
     */
    fun handle(incomingPdu: ByteArray, type: PduType) {
        scope.launch {
            networkLayer.handle(incomingPdu = incomingPdu, type = type)
                ?.let {
                    if (it.message is ProxyConfigurationMessage) {
                        _incomingProxyMessages.emit(value = it)
                    } else {
                        _incomingMeshMessages.emit(value = it)
                    }
                }
        }
    }

    /**
     * Await a response to the sent message.
     *
     * @param destination Destination address of the message.
     * @param timeout     Timeout duration.
     */
    suspend fun awaitMeshMessageResponse(
        destination: Address,
        responseOpcode: UInt,
        timeout: Duration,
    ) = awaitMeshMessageResponse(
        destination = MeshAddress.create(destination),
        responseOpcode = responseOpcode,
        timeout = timeout
    )

    /**
     * Awaits for a response for a previously sent message.
     *
     * @param destination Destination address of the message.
     * @param timeout     Timeout duration.
     */
    @OptIn(FlowPreview::class)
    suspend fun awaitMeshMessageResponse(
        destination: MeshAddress,
        responseOpcode: UInt,
        timeout: Duration,
    ): ReceivedMessage? = incomingMeshMessages
        .timeout(timeout = timeout)
        .catch {
            // If it's a timeout exception that's thrown we should log it in the bearer
            if (it is TimeoutCancellationException) {
                logger?.w(LogCategory.BEARER) {
                    "Timed out waiting for a response with response opCode 0x${
                        responseOpcode.toHexString(
                            format = HexFormat.UpperCase
                        )
                    } from ${
                        destination.address.toHexString(
                            format = HexFormat {
                                number.prefix = "0x"
                                upperCase = true
                            }
                        )
                    }: $it"
                }
            }
            throw it
        }
        .firstOrNull {
            destination == it.address && responseOpcode == (it.message as? HasOpCode)?.opCode
        }

    /**
     * Awaits for a response to a sent message.
     */
    suspend fun awaitProxyMessageResponse() = incomingProxyMessages.firstOrNull()

    /**
     * Clear outgoing messages for a given destination.
     *
     * @param destination destination address.
     */
    internal suspend fun clearOutgoingMessages(destination: MeshAddress) {
        mutex.withLock { outgoingMessages.remove(destination) }
    }

    /**
     * Publishes the given message using the Publish information from the given Model. If
     * publication is not set, this message does nothing.
     *
     * If publication retransmission is set, this method will retransmit the message specified
     * number of times, if applicable keeps the same TID value.
     *
     * @param message     Message to be published.
     * @param from        Source model from which the message is originating from.
     */
    suspend fun publish(message: MeshMessage, from: Model) {
        val publish = from.publish ?: return
        val localElement = from.parentElement ?: return
        val applicationKey = meshNetwork.applicationKey(index = publish.index) ?: return

        // calculate the TTL to be used
        val ttl = when (publish.ttl != 0xFF.toUByte()) {
            true -> publish.ttl
            false -> localElement.parentNode?.defaultTTL ?: networkParameters.defaultTtl
        }

        accessLayer.send(
            message = message,
            element = localElement,
            destination = publish.address as MeshAddress,
            ttl = ttl,
            applicationKey = applicationKey,
            retransmit = false
        )

        if (message is AcknowledgedMeshMessage) {
            var count = publish.retransmit.count.toInt()
            if (count > 0) {
                val interval: Duration = publish.retransmit.interval
                timer(
                    daemon = false,
                    period = interval.toLong(DurationUnit.MILLISECONDS)
                ) {
                    if (--count > 0) {
                        scope.launch {
                            accessLayer.send(
                                message = message,
                                element = localElement,
                                destination = publish.address as MeshAddress,
                                ttl = ttl,
                                applicationKey = applicationKey,
                                retransmit = true
                            )
                        }
                    } else {
                        cancel()
                    }
                }
            }
        }
    }

    /**
     * Ensures that the local node is not busy sending a message to the given destination address.
     *
     * @param destination Destination address.
     * @return `true` if the node is busy sending a message to the given destination address,
     * @throws Busy if the node is busy sending a message to the given destination address.
     */
    @Throws(Busy::class)
    private suspend fun ensureNotBusy(destination: MeshAddress) = mutex.withLock {
        require(!outgoingMessages.contains(destination)) { throw Busy() }
        outgoingMessages.add(destination)
        false
    }

    /**
     * Encrypts the message with the Application Key and a Network Key bound to it, and sends to the
     * given destination address.
     *
     * This method does not send nor return PDUs to be sent. Instead, for each created segment it
     * calls transmitter's [Transmitter.send] method, which should send the PDU over the air. This
     * is in order to support retransmission in case a packet was lost and needs to be sent again
     * after block acknowledgment was received.
     *
     * @param message          Message to be sent.
     * @param element          Source Element.
     * @param destination      Destination address.
     * @param initialTtl       Initial TTL (Time To Live) value of the message. If `nil`, the
     *                         default Node TTL will be used.
     * @param applicationKey   Application Key to sign the message.
     * @throws Busy if the node is busy sending a message to the given destination address.
     */
    @Throws(Busy::class)
    suspend fun send(
        message: MeshMessage,
        element: Element,
        destination: MeshAddress,
        initialTtl: UByte?,
        applicationKey: ApplicationKey,
    ): MeshMessage? = if (!ensureNotBusy(destination = destination)) accessLayer.send(
        message = message,
        element = element,
        destination = destination,
        ttl = initialTtl,
        applicationKey = applicationKey,
        retransmit = false
    ).also {
        mutex.withLock { outgoingMessages.remove(destination) }
    } else null

    /**
     * Encrypts the message with the Application Key and a Network Key bound to it, and sends to the
     * given destination address.
     *
     * This method does not send nor return PDUs to be sent. Instead, for each created segment it
     * calls transmitter's [Transmitter.send] method, which should send the PDU over the air. This
     * is in order to support retransmission in case a packet was lost and needs to be sent again
     * after block acknowledgment was received.
     *
     * @param message         Message to be sent.
     * @param element         Source Element.
     * @param destination     Destination Unicast Address.
     * @param initialTtl      Initial TTL (Time To Live) value of the message. If `nil`, the default
     *                        Node TTL will be used.
     * @param applicationKey  Application Key to sign the message.
     * @throws Busy if the node is busy sending a message to the given destination address.
     */
    @Throws(Busy::class)
    suspend fun send(
        message: AcknowledgedMeshMessage,
        element: Element,
        destination: UnicastAddress,
        initialTtl: UByte?,
        applicationKey: ApplicationKey,
    ): MeshMessage? {
        //val meshAddress = MeshAddress.create(address = destination)
        require(!ensureNotBusy(destination = destination)) { return null }

        return accessLayer.send(
            message = message,
            element = element,
            destination = destination,
            ttl = initialTtl,
            applicationKey = applicationKey,
            retransmit = false
        ).also {
            mutex.withLock { outgoingMessages.remove(destination) }
        }
    }

    /**
     * Encrypts the message with the Device Key and the first Network Key known to the target
     * device, and sends to the given destination address.
     *
     * This method does not send nor return PDUs to be sent. Instead, for each created segment it
     * calls transmitter's [Transmitter.send] method, which should send the PDU over the air. This
     * is in order to support retransmission in case a packet was lost and needs to be sent again
     * after block acknowledgment was received.
     *
     * @param configMessage  Message to be sent.
     * @param element        Source Element.
     * @param destination    Destination address.
     * @param initialTtl     Initial TTL (Time To Live) value of the message. If `nil`, the default
     *                       Node TTL will be used.
     * @throws Busy if the node is busy sending a message to the given destination address.
     */
    @Throws(Busy::class)
    suspend fun send(
        configMessage: UnacknowledgedConfigMessage,
        element: Element,
        destination: Address,
        initialTtl: UByte?,
        networkKey: NetworkKey,
    ) {
        val meshAddress = MeshAddress.create(address = destination)
        require(!ensureNotBusy(destination = meshAddress)) { throw Busy() }
        accessLayer.send(
            message = configMessage,
            localElement = element,
            destination = destination,
            initialTtl = initialTtl,
            networkKey = networkKey
        ).also {
            // Added to clear the outgoing message list
            mutex.withLock { outgoingMessages.remove(meshAddress) }
        }
    }

    /**
     * Encrypts the message with the Device Key and the first Network Key known to the target device,
     * and sends to the given destination address.
     *
     * The [ConfigNetKeyDelete] will be signed with a different Network Key that is removing.
     *
     * This method does not send nor return PDUs to be sent. Instead, for each created segment it
     * calls transmitter's [Transmitter.send] method, which should send the PDU over the air. This
     * is in order to support retransmission in case a packet was lost and needs to be sent again
     * after block acknowledgment was received.
     *
     * @param configMessage   Message to be sent.
     * @param element         Source Element.
     * @param destination     Destination address.
     * @param initialTtl      Initial TTL (Time To Live) value of the message. If `nil`, the default
     *                        Node TTL will be used.
     * @throws Busy if the node is busy sending a message to the given destination address.
     */
    @Throws(Busy::class)
    suspend fun send(
        configMessage: AcknowledgedConfigMessage,
        element: Element,
        destination: Address,
        initialTtl: UByte?,
        networkKey: NetworkKey,
    ): MeshMessage? {
        val meshAddress = MeshAddress.create(address = destination)
        require(!ensureNotBusy(destination = meshAddress)) { return null }
        return accessLayer.send(
            message = configMessage,
            localElement = element,
            destination = destination,
            initialTtl = initialTtl,
            networkKey = networkKey
        ).also {
            // Added to clear the outgoing message list
            mutex.withLock { outgoingMessages.remove(meshAddress) }
        }
    }

    /**
     * Sends the Proxy Configuration message to the connected Proxy node.
     *
     * @param message Proxy Configuration message to be sent.
     */
    suspend fun send(message: ProxyConfigurationMessage): ProxyConfigurationMessage? =
        networkLayer.send(message = message)

    /**
     * Replies to the received message, which was sent with the given key set, with the given
     * message.
     *
     * @param origin      Destination address of the message that the reply is for.
     * @param message     Response message to be sent.
     * @param element     Source Element.
     */
    suspend fun reply(
        origin: Address,
        message: MeshResponse,
        element: Element,
        destination: Address,
        keySet: KeySet,
    ) {
        accessLayer.reply(
            origin = origin,
            message = message,
            element = element,
            destination = destination,
            keySet = keySet
        )
    }

    /**
     * Cancels sending the message.
     *
     * @param handle Message handle.
     */
    suspend fun cancel(handle: MessageHandle) {
        accessLayer.cancel(handle)
    }
}

internal data class ReceivedMessage(val address: MeshAddress, val message: BaseMeshMessage)