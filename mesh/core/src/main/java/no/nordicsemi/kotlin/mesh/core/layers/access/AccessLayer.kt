@file:Suppress("UNUSED_PARAMETER", "MemberVisibilityCanBePrivate", "unused")

package no.nordicsemi.kotlin.mesh.core.layers.access

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.layers.AccessKeySet
import no.nordicsemi.kotlin.mesh.core.layers.DeviceKeySet
import no.nordicsemi.kotlin.mesh.core.layers.KeySet
import no.nordicsemi.kotlin.mesh.core.layers.MessageHandle
import no.nordicsemi.kotlin.mesh.core.layers.NetworkManager
import no.nordicsemi.kotlin.mesh.core.layers.NetworkManagerEvent
import no.nordicsemi.kotlin.mesh.core.layers.foundation.SceneClientHandler
import no.nordicsemi.kotlin.mesh.core.layers.uppertransport.UpperTransportPdu
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigAnyModelMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.TransactionMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnknownMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatPublicationSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationVirtualAddressSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNodeReset
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelSet
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.AllNodes
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.PrimaryGroupAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.Logger
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toDuration

/**
 * Defines a Transaction object that is responsible for generating transaction identifiers for
 * Transaction Messages such as [GenericLevelSet].
 *
 * @property lastTid       Last transaction identifier used.
 * @property timestamp     Timestamp of the last transaction message was sent.
 * @property currentTid    Current transaction identifier.
 * @property nextTid       Next transaction identifier.
 * @property isActive      Whether the transaction can be continued.
 */
@OptIn(ExperimentalTime::class)
private data class Transaction(
    var lastTid: UByte = Random.nextInt(0, UByte.MAX_VALUE.toInt()).toUByte(),
    var timestamp: Instant = Clock.System.now(),
) {

    val currentTid: UByte
        get() {
            timestamp = Clock.System.now()
            return lastTid
        }

    val nextTid: UByte
        get() {
            lastTid = if (lastTid < UByte.MAX_VALUE) (lastTid + 1u).toUByte() else 0u
            timestamp = Clock.System.now()
            return lastTid
        }

    val isActive: Boolean
        get() = Clock.System.now() - timestamp > 6.toDuration(DurationUnit.SECONDS)
}

internal class AcknowledgementContext(
    val request: AcknowledgedMeshMessage,
    val source: Address,
    val destination: Address,
    val delay: Duration,
    val repeatBlock: () -> Unit,
    val timeout: Duration,
    val timeoutBlock: () -> Unit,
) {

    var timeoutTimer: Timer? = Timer()
    private var timeoutTask: TimerTask? = timeoutTimer
        ?.schedule(delay = timeout.inWholeMilliseconds) {
            invalidate()
            timeoutBlock()
        }

    var retryTimer: Timer? = Timer()
    private var retryTimerTask: TimerTask? = retryTimer
        ?.schedule(delay = delay.inWholeMilliseconds) {
            repeatBlock()
        }

    init {
        initializeRetryTimer(delay = delay, callback = repeatBlock)
    }

    fun invalidate() {
        timeoutTask?.cancel()
        timeoutTask = null
        timeoutTimer?.cancel()
        timeoutTimer?.purge()
        timeoutTask = null

        retryTimerTask?.cancel()
        retryTimer?.cancel()
        retryTimer?.purge()
    }

    private fun initializeRetryTimer(delay: Duration, callback: () -> Unit) {
        retryTimerTask?.cancel()
        retryTimer?.cancel()
        retryTimer?.purge()
        retryTimer = Timer()
        retryTimerTask = retryTimer?.schedule(delay = delay.inWholeMilliseconds) {
            if (retryTimer != null) {
                callback()
                initializeRetryTimer(delay = delay * 2, callback = callback)
            }
        }
    }
}

/**
 * Defines the behavior of the Access Layer of the Mesh Networking Stack.
 *
 * @property networkManager  Network manager.
 */
internal class AccessLayer(private val networkManager: NetworkManager) : AutoCloseable {
    private val network: MeshNetwork
        get() = networkManager.meshNetwork

    private val mutex = Mutex()
    private val scope = networkManager.scope
    private val logger: Logger?
        get() = networkManager.logger

    private var transactions = mutableMapOf<Int, Transaction>()
    private var reliableMessageContexts = mutableListOf<AcknowledgementContext>()
    internal val contexts: List<AcknowledgementContext>
        get() = reliableMessageContexts
    private var publishers = mutableMapOf<Model, TimerTask>()

    private fun finalize() {
        transactions.clear()
        reliableMessageContexts.forEach { it.invalidate() }
        reliableMessageContexts.clear()
        publishers.forEach {
            it.value.cancel()
        }
        publishers.clear()
    }

    override fun close() {
        finalize()
    }

    /**
     * Initialize periodic publishing from local Models.
     */
    internal fun reinitializePublishers() {
        network.localElements
            .flatMap { it.models }
            .forEach { refreshPeriodicPublisher(it) }
    }

    /**
     * This method handles the Upper Transport PDU and reads the Opcode. If the Opcode is supported,
     * a message is created and sent to the corresponding Model, otherwise a generic MeshMessage is
     * created for the app to handle.
     *
     * @param upperTransportPdu Upper Transport PDU received.
     * @param keySet            Key set used to decrypt the message.
     * @return MeshMessage if the message was handled, null otherwise.
     */
    suspend fun handle(upperTransportPdu: UpperTransportPdu, keySet: KeySet): MeshMessage? {
        val accessPdu = AccessPdu.init(pdu = upperTransportPdu) ?: return null
        var request: AcknowledgedMeshMessage? = null

        val index = mutex.withLock {
            reliableMessageContexts.indexOfFirst {
                it.source == upperTransportPdu.destination.address &&
                        it.request.responseOpCode == accessPdu.opCode &&
                        it.destination == upperTransportPdu.source
            }
        }

        if (upperTransportPdu.destination is UnicastAddress && index > -1) {
            mutex.withLock {
                val context = reliableMessageContexts.removeAt(index)
                request = context.request
                context.invalidate()
            }
            logger?.i(LogCategory.ACCESS) {
                "Response $accessPdu received (decrypted using key: $keySet)"
            }
        } else {
            logger?.i(LogCategory.ACCESS) {
                "$accessPdu received (decrypted using key: $keySet)"
            }
        }
        return handle(accessPdu = accessPdu, keySet = keySet, request = request)
    }

    /**
     * Sends the given Mesh Message to the given destination address. The message is encrypted
     * with the given Application Key and the network key bound to it.
     *
     * Before sending the message, the transaction identifier is updated for messages that extend
     * [TransactionMessage].
     *
     * @param message          Mesh message to be sent.
     * @param element          Local Element.
     * @param destination      Destination address.
     * @param ttl              Initial TTL value of the message. If 'null' the default Node TTL will
     *                         be used.
     * @param applicationKey   Application Key to be used to encrypt the message.
     * @param retransmit       If the message is a retransmission of a previous message.
     */
    @OptIn(ExperimentalTime::class)
    suspend fun send(
        message: MeshMessage,
        element: Element,
        destination: MeshAddress,
        ttl: UByte?,
        applicationKey: ApplicationKey,
        retransmit: Boolean,
    ): MeshMessage? {
        var msg = message
        val transactionMessage = message as? TransactionMessage

        transactionMessage?.takeIf {
            it.tid == null
        }?.let {
            val k = key(element = element, destination = destination)
            mutex.withLock {
                transactions[k] = transactions[k] ?: Transaction()

                if (retransmit || it.continueTransaction && transactions[k]!!.isActive) {
                    it.tid = transactions[k]!!.currentTid
                } else {
                    it.tid = transactions[k]!!.nextTid
                }
            }
            msg = it
        }

        logger?.i(LogCategory.MODEL) {
            "Sending $msg from: ${element.unicastAddress.toHexString()}, " +
                    "to: ${destination.toHexString()})"
        }

        val pdu = AccessPdu.init(
            message = msg,
            source = element.unicastAddress.address,
            destination = destination,
            userInitiated = true
        )

        val keySet = AccessKeySet(applicationKey = applicationKey)
        logger?.i(LogCategory.ACCESS) { "Sending $pdu" }

        // Set timers for the acknowledged messages.
        // Acknowledged messages sent to a Group address won't await a Status.
        val ack = if (message is AcknowledgedMeshMessage && destination is UnicastAddress) {
            createReliableContext(pdu = pdu, element = element, initialTtl = ttl, keySet = keySet)
        } else null

        networkManager.upperTransportLayer.send(accessPdu = pdu, ttl = ttl, keySet = keySet)

        return when {
            // If ack is null, the message is not acknowledged, hence return null
            ack == null -> null

            else -> networkManager.awaitMeshMessageResponse(
                destination = destination,
                responseOpcode = ack.request.responseOpCode,
                timeout = ack.timeout
            )?.message as? MeshMessage
        }
    }

    /**
     * Sends the [ConfigMessage] to the given destination. The message is encrypted using the Device
     * Key which belongs to the target Node, and first Network Key known to this Node.
     *
     * @param message          Config message to be sent.
     * @param localElement     Local Element.
     * @param destination      Destination address.
     * @param initialTtl       Initial TTL value of the message. If 'null' the default Node TTL will
     *                         be used.
     * @throws IllegalArgumentException if the message is not a ConfigMessage.
     */
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun send(
        message: ConfigMessage,
        localElement: Element,
        destination: Address,
        initialTtl: UByte?,
        networkKey: NetworkKey,
    ): MeshMessage? {
        val node = network.node(destination) ?: throw InvalidDestination()

        val keySet = DeviceKeySet.init(
            networkKey = networkKey, node = node
        ) ?: return null
        logger?.i(LogCategory.FOUNDATION_MODEL) {
            "Sending $message to ${
                destination.toHexString(
                    format = HexFormat {
                        upperCase = true
                        number.prefix = "0x"
                    }
                )
            }"
        }
        val pdu = AccessPdu.init(
            message = message,
            source = localElement.unicastAddress.address,
            destination = MeshAddress.create(destination),
            userInitiated = true
        )
        logger?.i(LogCategory.ACCESS) { "Sending $pdu" }

        val ack = createReliableContext(
            pdu = pdu,
            element = localElement,
            initialTtl = initialTtl,
            keySet = keySet
        )

        networkManager.upperTransportLayer.send(accessPdu = pdu, ttl = initialTtl, keySet = keySet)

        return networkManager.awaitMeshMessageResponse(
            destination = destination,
            responseOpcode = ack.request.responseOpCode,
            timeout = ack.timeout
        )?.message as? MeshMessage
    }

    /**
     * Replies to the received message, which was sent with the given key set, with the given
     * message.
     *
     * @param origin       Destination address of the message that the reply is for.
     * @param message      Response message to be sent.
     * @param element      Source Element.
     * @param destination  Destination address. This must be a Unicast Address.
     * @param keySet       Set of keys that the message was encrypted with.
     */
    @OptIn(ExperimentalStdlibApi::class)
    suspend fun reply(
        origin: Address,
        destination: Address,
        message: MeshMessage,
        element: Element,
        keySet: KeySet,
    ) {
        val category = if (message is ConfigMessage)
            LogCategory.FOUNDATION_MODEL
        else LogCategory.MODEL
        logger?.i(category) {
            "Replying with $message from: $element to ${
                destination.toHexString(
                    format = HexFormat {
                        upperCase = true
                        number.prefix = "0x"
                    }
                )
            }"
        }
        val dst = MeshAddress.create(address = destination)
        val pdu = AccessPdu.init(
            message = message,
            source = origin,
            destination = dst,
            userInitiated = false
        )

        // If the message is sent in response to a received message that was sent to a Unicast
        // Address, the node should transmit the response message with a random delay between 20 and
        // 50 milliseconds. If the message is sent in response to a received message that was sent
        // to a group address or a virtual address, the node should transmit the response message
        // with a random delay between 20 and 500 milliseconds. This reduces the probability of
        // multiple nodes responding to this message at exactly the same time, and therefore
        // increases the probability of message delivery rather than message collisions.
        val delay = if (dst is UnicastAddress) {
            Random.nextInt(20, 50).toDuration(DurationUnit.MILLISECONDS)
        } else {
            Random.nextInt(20, 500).toDuration(DurationUnit.MILLISECONDS)
        }

        delay(duration = delay)
        logger?.i(LogCategory.ACCESS) { "Sending $pdu" }
        networkManager.upperTransportLayer.send(accessPdu = pdu, ttl = null, keySet = keySet)
    }

    internal suspend fun cancel(handle: MessageHandle) {
        logger?.i(LogCategory.ACCESS) {
            "Cancelling messages with op code: ${handle.opCode}, " + "sent from: " +
                    "${
                        handle.source.address.toHexString(
                            format = HexFormat {
                                upperCase = true
                                number.prefix = "0x"
                            })
                    } " +
                    "to: ${
                        handle.destination.address.toHexString(
                            format = HexFormat {
                                upperCase = true
                                number.prefix = "0x"
                            })
                    }"
        }

        mutex.withLock {
            reliableMessageContexts.indexOfFirst {
                it.source == handle.destination.address &&
                        it.request.responseOpCode == handle.opCode &&
                        it.destination == handle.source.address
            }.takeIf { it > -1 }?.let {
                reliableMessageContexts.removeAt(index = it).invalidate()
            }
        }
        networkManager.upperTransportLayer.cancel(handle)
    }

    /**
     *
     * @param accessPdu Access PDU received.
     * @param keySet    Key set used to decrypt the message.
     * @param request   Request message if the message was sent as a response to a request.
     * @return MeshMessage if the message was handled, null otherwise.
     */
    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun handle(
        accessPdu: AccessPdu,
        keySet: KeySet,
        request: AcknowledgedMeshMessage?,
    ): MeshMessage? {
        val localNode = network.localProvisioner?.node ?: return null

        // The access PDU is decoded in to a Mesh Message
        var newMessage: MeshMessage? = null

        if (keySet is AccessKeySet) {
            for (element in localNode.elements) {
                val models = element.models.filter { !it.requiresDeviceKey }

                for (model in models) {
                    val eventHandler = model.eventHandler ?: continue
                    val message = eventHandler.decode(accessPdu = accessPdu) ?: continue
                    // Save and log only the first decoded message
                    if (newMessage == null) {
                        logger?.i(LogCategory.MODEL) {
                            "$message received from: ${
                                accessPdu.source.toHexString(
                                    format = HexFormat {
                                        upperCase = true
                                        number.prefix = "0x"
                                    })
                            }, to: ${
                                accessPdu.destination.address.toHexString(
                                    format = HexFormat {
                                        upperCase = true
                                        number.prefix = "0x"
                                    }
                                )
                            }"
                        }
                        newMessage = message
                    } else if (message::class != newMessage::class) {
                        // If another model's delegate decoded the same message to a different type,
                        // log this with a warning. This other type will be delivered to the
                        // delegate, but not to the global network delegate.
                        logger?.w(LogCategory.MODEL) { "$message already decoded as $newMessage" }
                    }
                    // Deliver the message to the Model if it was signed with an Application Key
                    // bound to this Model and the message is targeting this Element, or the
                    // Model is subscribed to the destination address.
                    //
                    // Note:   Messages sent to .allNodes address shall be processed only by
                    //         Models on the Primary Element. See Bluetooth Mesh Profile 1.0.1,
                    //         chapter 3.4.2.4.
                    // Note 2: As the iOS implementation does not support Relay, Proxy or Friend
                    //         Features, the messages sent to those addresses shall only be
                    //         processed if the Model is explicitly subscribed to these
                    //         addresses.

                    if ((accessPdu.destination is AllNodes && element.isPrimary) ||
                        accessPdu.destination.address == element.unicastAddress.address ||
                        model.isSubscribedTo(accessPdu.destination as PrimaryGroupAddress)
                    ) {
                        if (keySet.applicationKey.isBoundTo(model = model)) {
                            eventHandler.onMeshMessageReceived(
                                model = model,
                                message = message,
                                source = accessPdu.source,
                                destination = accessPdu.destination.address,
                                request = request
                            )?.let { response ->
                                networkManager.reply(
                                    origin = accessPdu.destination.address,
                                    destination = accessPdu.source,
                                    message = response,
                                    element = element,
                                    keySet = keySet
                                )

                                if (eventHandler is SceneClientHandler) {
                                    networkManager.emitNetworkManagerEvent(
                                        NetworkManagerEvent.OnNetworkChanged
                                    )
                                }
                            }
                        } else {
                            logger?.w(LogCategory.MODEL) {
                                "Local ${model.name} model on ${model.parentElement!!.unicastAddress} " +
                                        "not bound to ${keySet.applicationKey}"
                            }
                        }
                    }
                    break
                }
            }
        } else {
            // otherwise, the Device Key was used.
            val models = localNode.elements
                .flatMap { it.models }
                .filter { it.supportsDeviceKey }

            for (model in models) {
                val eventHandler = model.eventHandler ?: continue
                val message = eventHandler.decode(accessPdu = accessPdu) ?: continue
                newMessage = message
                // Is this message targeting the local Node?
                if (localNode.containsElementWithAddress(address = accessPdu.destination.address)) {
                    logger?.i(LogCategory.FOUNDATION_MODEL) {
                        "$message received from: ${
                            accessPdu.source.toHexString(
                                format = HexFormat {
                                    number.prefix = "0x"
                                    upperCase = true
                                }
                            )
                        }"
                    }
                    eventHandler.onMeshMessageReceived(
                        model = model,
                        message = message,
                        source = accessPdu.source,
                        destination = accessPdu.destination.address,
                        request = request
                    )?.let { response ->
                        networkManager.reply(
                            origin = accessPdu.destination.address,
                            destination = accessPdu.source,
                            message = response,
                            element = model.parentElement!!,
                            keySet = keySet
                        )
                        // Some Config Messages require special handling.
                        handle(message = message)
                    }
                    networkManager.emitNetworkManagerEvent(event = NetworkManagerEvent.OnNetworkChanged)
                } else {
                    logger?.i(LogCategory.FOUNDATION_MODEL) {
                        "$message received from: ${
                            accessPdu.source.toHexString(
                                format = HexFormat {
                                    number.prefix = "0x"
                                    upperCase = true
                                }
                            )
                        }, to: ${
                            accessPdu.destination.address.toHexString(
                                format = HexFormat {
                                    number.prefix = "0x"
                                    upperCase = true
                                }
                            )
                        }"
                    }
                }
                break
            }
        }
        // If the message has not been decoded and handled by any of the ModelEventHandlers return
        // it to the user as an Unknown Message.
        // To add support to any new message, create a ModelEventHandler and add it to the local
        // Element.
        return newMessage ?: UnknownMessage(accessPdu = accessPdu)
    }

    /**
     * Handles selected config messages in a special way.
     *
     * @param message Config message to be handled.
     */
    private suspend fun handle(message: MeshMessage) {
        if (message is ConfigHeartbeatPublicationSet) {
            networkManager.upperTransportLayer.refreshHeartbeatPublisher()
        }

        if (message is ConfigModelPublicationSet ||
            message is ConfigModelPublicationVirtualAddressSet
        ) {
            val request = message as? ConfigAnyModelMessage
            request?.let { req ->
                network.localProvisioner?.node?.let { localNode ->
                    localNode.element(address = req.elementAddress)?.let { element ->
                        element.model(modelId = message.modelId)?.let {
                            refreshPeriodicPublisher(model = it)
                        }
                    }
                }
            }
        }
        if (message is ConfigNodeReset) {
            networkManager.emitNetworkManagerEvent(event = NetworkManagerEvent.OnNetworkReset)
        }
    }

    /**
     * Creates a key consisting of the source address and the destination address.
     *
     * @param element       Element to which the message was sent.
     * @param destination   Destination address of the message.
     * @return Key for the transaction which is an Int value.
     */
    private fun key(element: Element, destination: MeshAddress) =
        element.unicastAddress.address.toInt() shl 16 or destination.address.toInt()

    /**
     * Creates the context of an Acknowledged message.
     *
     * The context contains timers responsible for resending the message until a status is received,
     * and allows the message to be canceled.
     *
     * @param pdu           Access PDU received.
     * @param element       Element to which the message was sent.
     * @param initialTtl    Initial TTL value of the message.
     * @param keySet        Key set used to encrypt the message.
     */
    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun createReliableContext(
        pdu: AccessPdu,
        element: Element,
        initialTtl: UByte?,
        keySet: KeySet,
    ): AcknowledgementContext {
        val request = pdu.message as AcknowledgedMeshMessage
        /*val request = pdu.message as? AcknowledgedMeshMessage ?: return null
        require(pdu.destination is UnicastAddress) { return null }*/

        // The ttl with which the request will be sent.
        val ttl = element.parentNode?.defaultTTL ?: networkManager.networkParameters.defaultTtl

        val initialDelay = networkManager.networkParameters.acknowledgementMessageInterval(
            ttl = ttl,
            segmentCount = pdu.segmentsCount
        )

        val timeout = networkManager.networkParameters.acknowledgementMessageTimeout

        val ack = AcknowledgementContext(
            request = request,
            source = pdu.source,
            destination = pdu.destination.address,
            delay = initialDelay,
            repeatBlock = {
                networkManager.takeIf {
                    !it.upperTransportLayer.isReceivingResponse(address = pdu.destination.address)
                }?.let {
                    scope.launch {
                        logger?.w(LogCategory.ACCESS) { "Resending $pdu" }
                        it.upperTransportLayer.send(accessPdu = pdu, ttl = ttl, keySet = keySet)
                    }
                }
            },
            timeout = timeout,
            timeoutBlock = {
                logger?.w(LogCategory.ACCESS) {
                    "Response to $pdu not received (timed out)"
                }
                val category = if (request is AcknowledgedConfigMessage)
                    LogCategory.FOUNDATION_MODEL
                else LogCategory.MODEL
                logger?.w(category) {
                    "$request sent from: ${
                        pdu.source.toHexString(
                            format = HexFormat {
                                number.prefix = "0x"
                                upperCase = true
                            }
                        )
                    } to: ${
                        pdu.destination.address.toHexString(
                            format = HexFormat {
                                number.prefix = "0x"
                                upperCase = true
                            }
                        )
                    } timed out"
                }
                scope.launch {
                    cancel(
                        handle = MessageHandle(
                            message = request,
                            source = pdu.source,
                            destination = pdu.destination,
                            manager = networkManager
                        )
                    )
                    mutex.withLock { reliableMessageContexts.clear() }
                }
            }
        )
        mutex.withLock {
            reliableMessageContexts.add(ack)
        }
        return ack
    }

    /**
     * Invalidates the current and optionally creates a new publisher that will send periodic
     * publications, when they are set up in the Model.
     *
     * @param model The Model for which the publisher is to be refreshed.
     */
    private fun refreshPeriodicPublisher(model: Model) {
        publishers[model]?.cancel()

        val publish = requireNotNull(model.publish) { return }
        require(publish.period.interval > Duration.ZERO) { return }
        val composer = model.eventHandler?.publicationMessageComposer ?: return
        publishers[model] = Timer().schedule(delay = publish.period.interval.inWholeSeconds) {
            val manager = networkManager
            scope.launch {
                manager.publish(composer(), model)
            }
        }
    }
}

/**
 * Attempts to decode the given AccessPdu. The Model Handler must support the opcode to specify to
 * which type should the message be decoded.
 *
 * @param accessPdu Access PDU received.
 * @return The decoded mesh message or null if the message is not supported.
 */
private fun ModelEventHandler.decode(accessPdu: AccessPdu): MeshMessage? =
    messageTypes[accessPdu.opCode]?.init(accessPdu.parameters) as MeshMessage?


/**
 * When invoked, the decoded message is processed and is passed to the proper event handler,
 * depending on its type or in case if it was a response to a previously sent request.
 *
 * @param model         Model that received the message.
 * @param message       Message that was received by the model.
 * @param source        Address of the Element from which the message was sent.
 * @param destination   Address to which the message was sent.
 * @param request       Request that was sent.
 */
private suspend fun ModelEventHandler.onMeshMessageReceived(
    model: Model,
    message: MeshMessage,
    source: Address,
    destination: Address,
    request: AcknowledgedMeshMessage?,
) = when {
    request != null -> {
        val response = message as? MeshResponse
            ?: error("$message is not MeshResponse")
        handle(
            event = ModelEvent.ResponseReceived(
                model = model,
                response = response,
                request = request,
                source = source
            )
        )
    }

    message is AcknowledgedMeshMessage -> runCatching {
        handle(
            event = ModelEvent.AcknowledgedMessageReceived(
                model = model,
                request = message,
                source = source,
                destination = MeshAddress.create(address = destination)
            )
        )
    }.getOrNull()

    message is UnacknowledgedMeshMessage -> handle(
        event = ModelEvent.UnacknowledgedMessageReceived(
            model = model,
            message = message,
            source = source,
            destination = MeshAddress.create(address = destination)
        )
    )

    else -> error("$message is neither Acknowledged nor Unacknowledged")
}