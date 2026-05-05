@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.layers.uppertransport

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nordicsemi.kotlin.mesh.core.layers.KeySet
import no.nordicsemi.kotlin.mesh.core.layers.MessageHandle
import no.nordicsemi.kotlin.mesh.core.layers.NetworkManager
import no.nordicsemi.kotlin.mesh.core.layers.access.AccessPdu
import no.nordicsemi.kotlin.mesh.core.layers.lowertransport.AccessMessage
import no.nordicsemi.kotlin.mesh.core.layers.lowertransport.ControlMessage
import no.nordicsemi.kotlin.mesh.core.layers.lowertransport.LowerTransportPdu
import no.nordicsemi.kotlin.mesh.core.layers.lowertransport.LowerTransportPduType
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.Logger
import java.util.Timer
import kotlin.concurrent.timer
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Defines the behaviour of the Upper Transport Layer of the Mesh Networking Stack.
 */
internal class UpperTransportLayer(private val networkManager: NetworkManager) : AutoCloseable{
    private val meshNetwork: MeshNetwork
        get() = networkManager.meshNetwork
    private val logger: Logger?
        get() = networkManager.logger
    private val queue: MutableMap<Address, MutableList<MessageData>> = mutableMapOf()
    private val mutex = Mutex()
    private var heartbeatPublisher: Timer? = null

    override fun close() {
        heartbeatPublisher?.cancel()
        heartbeatPublisher?.purge()
    }

    /**
     * Handles a received Lower Transport Pdu.
     *
     * Depending on the PDU type, the message will be either propagated to Access Layer or handled
     * internally.
     * @param lowerTransportPdu The received Lower Transport PDU.
     */
    suspend fun handle(lowerTransportPdu: LowerTransportPdu): MeshMessage? {
        when (lowerTransportPdu.type) {
            LowerTransportPduType.ACCESS_MESSAGE -> {
                val accessMessage = lowerTransportPdu as AccessMessage
                val message = UpperTransportPdu.decode(
                    message = accessMessage, network = meshNetwork
                )?.let {
                    logger?.i(LogCategory.UPPER_TRANSPORT) { "Received ${it.first}" }
                    networkManager.accessLayer.handle(
                        upperTransportPdu = it.first,
                        keySet = it.second
                    )
                }
                if (message == null) {
                    logger?.w(LogCategory.UPPER_TRANSPORT) { "Failed to decode PDU" }
                }
                return message
            }

            LowerTransportPduType.CONTROL_MESSAGE -> {
                val message = lowerTransportPdu as ControlMessage
                when (message.opCode) {
                    HeartbeatMessage.OP_CODE -> {
                        HeartbeatMessage.init(message = message).also { heartbeat ->
                            logger?.i(LogCategory.UPPER_TRANSPORT) {
                                "$heartbeat received from ${message.source.toHexString()}."
                            }
                            handle(heartbeat = heartbeat)
                        }
                    }

                    else -> {
                        logger?.i(LogCategory.UPPER_TRANSPORT) {
                            "Unsupported Control Message received (opCode: ${
                                message.opCode.toHexString(
                                    format = HexFormat {
                                        number.prefix = "0x"
                                        upperCase = true
                                    }
                                )
                            })"
                        }
                    }
                }
                return null
            }
        }
    }

    /**
     * Encrypts the Access PDU using given key set and sends it down to the Lower Transport Layer.
     *
     * @param accessPdu  Access PDU to be sent.
     * @param ttl        Initial TTL value of the message. If 'null', default Node TTL will be used.
     * @param keySet     Key set to be used to encrypt the message.
     */
    suspend fun send(accessPdu: AccessPdu, ttl: UByte?, keySet: KeySet) {
        // Get the current sequence number for the given source Element's address.
        val sequence = networkManager.networkLayer.nextSequenceNumber(
            address = UnicastAddress(address = accessPdu.source)
        )
        val pdu = UpperTransportPdu.init(
            pdu = accessPdu,
            keySet = keySet,
            sequence = sequence,
            ivIndex = meshNetwork.ivIndex
        )
        logger?.i(LogCategory.UPPER_TRANSPORT) { "Sending $pdu (encrypted using key: $keySet)" }

        if (pdu.transportPdu.size > 15 || accessPdu.isSegmented) {
            // Enqueue the PDU. If the queue was empty, the PDU will be sent immediately.
            enqueue(pdu, ttl, keySet.networkKey)
        } else {
            networkManager.lowerTransportLayer.sendUnsegmentedUpperTransportPdu(
                pdu = pdu,
                initialTtl = ttl,
                networkKey = keySet.networkKey
            )
        }
    }

    /**
     * Cancels sending all segmented messages matching given handle. Unsegmented messages are sent
     * almost instantaneously and cannot be canceled.
     *
     * @param handle Message handle.
     */
    internal suspend fun cancel(handle: MessageHandle) {
        var shouldSendNext = false
        // Check if the message that is currently being sent matches the handler data. If so, cancel
        // it.
        mutex.withLock {
            queue[handle.destination.address]?.firstOrNull()
        }?.takeIf { first ->
            first.pdu.message!!.opCode == handle.opCode &&
                    first.pdu.source == handle.source.address
        }?.let {
            logger?.d(LogCategory.UPPER_TRANSPORT) { "Cancelling sending ${it.pdu}" }
            networkManager.lowerTransportLayer.cancelSending(segmentedPdu = it.pdu)
            shouldSendNext = true
        }

        mutex.withLock {
            queue[handle.destination.address]?.removeAll {
                it.pdu.message!!.opCode == handle.opCode &&
                        it.pdu.source == handle.source.address &&
                        it.pdu.destination == handle.destination
            }
        }
        // If sending a message was canceled, try sending another one.
        if (shouldSendNext) {
            onLowerTransportLayerSent(handle.destination.address)
        }
    }

    /**
     * Returns whether the underlying layer is in progress of receiving a message from the given
     * address.
     *
     * @param address Source address.
     * @return 'true' if some, but not all packets of a segmented message were received from the
     *         given source address; 'false' if not packets were received or the message was
     *         complete before calling this method.
     */
    fun isReceivingResponse(address: Address): Boolean {
        return networkManager.lowerTransportLayer.isReceivingMessage(address = address)
    }

    /**
     * Invoked by the lower transport layer when a segmented message has been sent to the
     * destination or failed.
     *
     * This removes the PDU that was sent from the queue and sends the next one if available.
     *
     * @param destination Destination address.
     */
    suspend fun onLowerTransportLayerSent(destination: Address) {
        mutex.withLock {
            require(queue[destination]?.isNotEmpty() ?: false) { return }
            // Remove the PDU that has just been sent.
            queue[destination]?.removeAt(index = 0)
        }

        // Try to send the next one
        sendNext(destination = destination)
    }

    /**
     * Invalidates and optionally restarts the periodic Heartbeat publisher if Heartbeat publication
     * has been set.
     */
    fun refreshHeartbeatPublisher() {
        heartbeatPublisher?.let {
            logger?.i(category = LogCategory.UPPER_TRANSPORT) {
                "Publishing periodic Heartbeat messages cancelled"
            }
            it.cancel()
        }

        meshNetwork.localProvisioner?.node?.heartbeatPublication?.takeIf {
            it.isPeriodicHeartbeatStateEnabled
        }?.let { heartbeatPublication ->
            heartbeatPublication.state?.let {
                logger?.i(category = LogCategory.UPPER_TRANSPORT) {
                    "Publishing periodic Heartbeat messages initiated."
                }
                val interval = heartbeatPublication.period.toInt()
                    .toDuration(DurationUnit.SECONDS)
                heartbeatPublisher = timer(
                    name = "HeartbeatPublisher",
                    period = interval.inWholeMilliseconds
                ) {
                    val layer = this@UpperTransportLayer
                    // Check if the local node still exists.
                    val localNode = requireNotNull(meshNetwork.localProvisioner?.node) {
                        layer.heartbeatPublisher?.cancel()
                        layer.heartbeatPublisher?.purge()
                        logger?.i(category = LogCategory.UPPER_TRANSPORT) {
                            "Publishing periodic Heartbeat messages cancelled."
                        }
                        return@timer
                    }
                    // Check if the network key exists.
                    val networkKey = requireNotNull(
                        localNode.networkKey(index = heartbeatPublication.index)
                    ) {
                        layer.heartbeatPublisher?.cancel()
                        layer.heartbeatPublisher?.purge()
                        logger?.i(category = LogCategory.UPPER_TRANSPORT) {
                            "Publishing periodic Heartbeat messages cancelled."
                        }
                        return@timer
                    }

                    val state = requireNotNull(
                        heartbeatPublication.state
                    ) {
                        layer.heartbeatPublisher?.cancel()
                        layer.heartbeatPublisher?.purge()
                        logger?.i(category = LogCategory.UPPER_TRANSPORT) {
                            "Publishing periodic Heartbeat messages cancelled."
                        }
                        return@timer
                    }
                    require(heartbeatPublication.isPeriodicHeartbeatStateEnabled) {
                        layer.heartbeatPublisher?.cancel()
                        layer.heartbeatPublisher?.purge()
                        logger?.i(LogCategory.UPPER_TRANSPORT) {
                            "Publishing periodic Heartbeat messages cancelled."
                        }
                        return@timer
                    }
                    val heartbeat = HeartbeatMessage.init(
                        heartbeatPublication = heartbeatPublication,
                        source = localNode.primaryUnicastAddress,
                        destination = heartbeatPublication.address,
                        ivIndex = meshNetwork.ivIndex,
                    )

                    networkManager.scope.launch {
                        send(heartbeat = heartbeat, networkKey = networkKey)
                    }
                    // If the last periodic Heartbeat message has been sent, cancel the timer.
                    if (!state.shouldSendMorePeriodicHeartbeatMessages()) {
                        layer.heartbeatPublisher?.cancel()
                        layer.heartbeatPublisher?.purge()
                        logger?.i(LogCategory.UPPER_TRANSPORT) {
                            "Publishing periodic Heartbeat messages finished."
                        }
                        return@timer
                    }
                    // Do nothing. Timer will be fired again.
                }
            }
        }
    }

    /**
     * Enqueue a message to be sent
     *
     * @param pdu            PDU to be sent.
     * @param initialTtl     Initial TTL.
     * @param networkKey     Network key used to encrypt ht message.
     */

    private suspend fun enqueue(
        pdu: UpperTransportPdu,
        initialTtl: UByte?,
        networkKey: NetworkKey
    ) {
        val destination = pdu.destination.address
        var count: Int
        mutex.withLock {
            queue[destination] = queue[destination] ?: mutableListOf()
            queue[destination]!!.add(
                element = MessageData(
                    pdu = pdu,
                    ttl = initialTtl,
                    networkKey = networkKey
                )
            )
            count = queue[destination]!!.size
        }

        if (count == 1) sendNext(destination = destination)
    }

    /**
     * Sends the next enqueued PDU. This method does nothing if the queue for the given destination
     * is empty or does not exist.
     */
    private suspend fun sendNext(destination: Address) {
        val messageData = requireNotNull(value = mutex.withLock {
            queue[destination]?.firstOrNull()
        }) {
            return
        }
        // If another PDU has been enqueued, send it.
        networkManager.lowerTransportLayer.sendSegmentedUpperTransportPdu(
            pdu = messageData.pdu,
            initialTtl = messageData.ttl,
            networkKey = messageData.networkKey
        )
    }

    /**
     * Handles received Heartbeat message. If the local Node has active subscription matching
     * received Heartbeat, the count value will be incremented.
     *
     * @param heartbeat Received Heartbeat message.
     */
    private fun handle(heartbeat: HeartbeatMessage) {
        meshNetwork.localProvisioner?.node?.heartbeatSubscription?.updateIfMatches(heartbeat)
    }

    /**
     * Sends a Heartbeat message.
     *
     * @param heartbeat   Heartbeat message to be sent.
     * @param networkKey  Network key to be used to encrypt the message.
     */
    private suspend fun send(heartbeat: HeartbeatMessage, networkKey: NetworkKey) {
        logger?.i(LogCategory.UPPER_TRANSPORT) {
            "Sending $heartbeat to ${heartbeat.destination.toHexString()}" + "encrypted " +
                    "using key: $networkKey"
        }
        networkManager.lowerTransportLayer.send(heartbeat = heartbeat, networkKey = networkKey)
    }
}

/**
 * Message data class containing the pdu, ttl and network key.
 *
 * @property pdu            Pdu to be sent.
 * @property ttl            TTL value of the pdu.
 * @property networkKey     Network key to be used to encrypt the pdu.
 * @constructor Creates a message data object.
 */
internal data class MessageData(
    var pdu: UpperTransportPdu,
    val ttl: UByte?,
    val networkKey: NetworkKey
)