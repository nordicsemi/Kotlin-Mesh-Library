package no.nordicsemi.kotlin.mesh.core.layers.lowertransport

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nordicsemi.kotlin.data.shl
import no.nordicsemi.kotlin.mesh.bearer.PduType
import no.nordicsemi.kotlin.mesh.core.layers.NetworkManager
import no.nordicsemi.kotlin.mesh.core.layers.NetworkParameters
import no.nordicsemi.kotlin.mesh.core.layers.network.NetworkPdu
import no.nordicsemi.kotlin.mesh.core.layers.uppertransport.HeartbeatMessage
import no.nordicsemi.kotlin.mesh.core.layers.uppertransport.UpperTransportPdu
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.Logger
import java.util.Timer
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.schedule
import kotlin.math.min
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi

private sealed class Message {

    data class LowerTransportLayerPdu(val message: LowerTransportPdu) : Message()

    data class Acknowledgement(val ack: SegmentAcknowledgementMessage) : Message()
}

sealed class SecurityError : Exception() {

    /**
     * Thrown internally when a possible replay attack is detected. This error is not propagated to
     * higher levels. When it is caught, te received packet discarded.
     */
    @Suppress("unused")
    class ReplayAttack : SecurityError()
}

/**
 * The Lower Transport Layer is responsible for sending and receiving non-segmented, segmented
 * messages and block acknowledgements. It is responsible for Segmentation and Reassembly (SAR) of
 * segmented messages.
 *
 * @property networkManager             Network manager instance.
 * @property scope                      Coroutine scope.
 * @property logger                     Logger handler
 * @property storage                    Secure storage for storing last SeqAuth values.
 * @property networkParams              Network parameters.
 * @property network                    Mesh network.
 * @property mutex                      Mutex for synchronization.
 * @property incompleteSegments         A map of incomplete segments received from a certain node.
 *                                      Once a segmented message is received, it will be added to an
 *                                      ordered list. When all segments are received, they are sent
 *                                      for further processing to a higher layer.
 */
internal class LowerTransportLayer(private val networkManager: NetworkManager) {
    private val network: MeshNetwork
        get() = networkManager.meshNetwork
    private val logger: Logger?
        get() = networkManager.logger
    private val storage = networkManager.securePropertiesStorage
    val scope = networkManager.scope
    private val mutex = Mutex()
    private val networkParams: NetworkParameters
        get() = networkManager.networkParameters

    private val incompleteSegments = mutableMapOf<UInt, MutableList<SegmentedMessage?>>()
    private val acknowledgements = mutableMapOf<Address, SegmentAcknowledgementMessage>()
    private val discardTimers = mutableMapOf<UInt, Timer>()
    private val acknowledgementTimers = mutableMapOf<UInt, Timer>()
    private val outgoingSegments = mutableMapOf<UShort, OutgoingSegments>()
    private val unicastRetransmissionsTimers = mutableMapOf<UShort, Timer>()
    private val remainingNumberOfUnicastRetransmissions =
        mutableMapOf<UShort, RemainingNumberOfUnicastRetransmissions>()
    private var multicastRetransmissionsTimers = mutableMapOf<UShort, Timer>()
    private var remainingNumberOfMulticastRetransmissions = mutableMapOf<UShort, UByte>()
    private val segmentTtl = mutableMapOf<UShort, UByte>()

    /**
     * This method handles the received Network PDU. If needed, it will reassemble the messages,
     * send block acknowledgement to the sender, and pass the Upper Transport PDU to the Upper
     * Transport Layer.
     *
     * @param networkPdu Network PDU received.
     * @return Upper Transport PDU to be sent to the Upper Transport Layer or null if the message
     */
    suspend fun handle(networkPdu: NetworkPdu): MeshMessage? {
        require(networkPdu.transportPdu.size > 1) { return null }
        mutex.withLock {
            require(checkAgainstReplayAttack(networkPdu)) { return null }
            val segmented = networkPdu.isSegmented
            var msg: Message? = null
            if (segmented) {
                when (networkPdu.type) {
                    LowerTransportPduType.ACCESS_MESSAGE -> SegmentedAccessMessage
                        .init(pdu = networkPdu)
                        ?.let {
                            logger?.i(category = LogCategory.LOWER_TRANSPORT) {
                                "$it received (decrypted using key: ${it.networkKey.name})"
                            }
                            assemble(segment = it, networkPdu = networkPdu)?.let { pdu ->
                                msg = Message.LowerTransportLayerPdu(pdu)
                            }
                        }

                    LowerTransportPduType.CONTROL_MESSAGE -> SegmentedControlMessage
                        .init(pdu = networkPdu)
                        ?.let {
                            logger?.i(category = LogCategory.LOWER_TRANSPORT) {
                                "$it received (decrypted using key: ${it.networkKey.name})"
                            }
                            assemble(it, networkPdu)?.let { pdu ->
                                msg = Message.LowerTransportLayerPdu(message = pdu)
                            }
                        }
                }
            } else {
                when (networkPdu.type) {
                    LowerTransportPduType.ACCESS_MESSAGE -> AccessMessage
                        .init(pdu = networkPdu)
                        .let {
                            logger?.i(LogCategory.LOWER_TRANSPORT) {
                                "$it received (decrypted using key: ${it.networkKey.name})"
                            }
                            msg = Message.LowerTransportLayerPdu(message = it)
                        }

                    LowerTransportPduType.CONTROL_MESSAGE -> {
                        val opCode = (networkPdu.transportPdu[0].toUByte().toInt() and 0x7F)
                        msg = when (opCode == 0x00) {
                            true -> {
                                val ack = SegmentAcknowledgementMessage.init(networkPdu)
                                logger?.i(category = LogCategory.LOWER_TRANSPORT) {
                                    "$ack received (decrypted using key: ${ack.networkKey.name})"
                                }
                                Message.Acknowledgement(ack = ack)
                            }

                            else -> {
                                val controlMessage = ControlMessage.init(networkPdu)
                                logger?.i(category = LogCategory.LOWER_TRANSPORT) {
                                    "$controlMessage received (decrypted using key: ${
                                        controlMessage.networkKey.name
                                    })"
                                }
                                Message.LowerTransportLayerPdu(message = controlMessage)
                            }
                        }
                    }
                }
            }
            return try {
                when (msg) {
                    is Message.LowerTransportLayerPdu -> {
                        networkManager.upperTransportLayer.handle(
                            lowerTransportPdu = (msg as Message.LowerTransportLayerPdu).message
                        )
                    }

                    is Message.Acknowledgement -> {
                        handle(ack = (msg as Message.Acknowledgement).ack)
                        null
                    }

                    else -> null
                }
            } catch (e: Exception) {
                logger?.e(LogCategory.LOWER_TRANSPORT) { "Failed to handle Network PDU: $e" }
                throw e
            }
        }
    }

    /**
     * Sends the given Upper Transport Message.
     *
     * @param pdu             Upper Transport PDU to be sent.
     * @param initialTtl      TTL to be used to send the message.
     * @param networkKey      Network key to be used to encrypt the message.
     */
    suspend fun sendUnsegmentedUpperTransportPdu(
        pdu: UpperTransportPdu,
        initialTtl: UByte?,
        networkKey: NetworkKey,
    ) {
        network.localProvisioner?.node?.let { node ->
            val ttl = initialTtl ?: node.defaultTTL ?: networkManager.networkParameters.defaultTtl
            val message = AccessMessage(pdu = pdu, networkKey = networkKey)
            try {
                logger?.i(LogCategory.LOWER_TRANSPORT) { "Sending $message" }
                networkManager.run {
                    networkLayer.send(
                        pdu = message,
                        type = PduType.NETWORK_PDU,
                        ttl = ttl
                    )
                    clearOutgoingMessages(destination = pdu.destination)
                }
            } catch (e: Exception) {
                logger?.e(LogCategory.LOWER_TRANSPORT) {
                    "Failed to send unsegmented Upper Transport PDU: $e"
                }
                pdu.message!!.takeIf {
                    it.isAcknowledged
                }?.let {
                    networkManager.clearOutgoingMessages(destination = pdu.destination)
                }
            }
        }
    }

    /**
     * Sends the given Upper Transport Message.
     *
     * @param pdu          Upper Transport PDU to be sent.
     * @param initialTtl   TTL to be used to send the message.
     * @param networkKey   Network key to be used to encrypt the message.
     */
    suspend fun sendSegmentedUpperTransportPdu(
        pdu: UpperTransportPdu,
        initialTtl: UByte?,
        networkKey: NetworkKey,
    ) {
        val provisionerNode = network.localProvisioner?.node ?: return
        // Last 13 bits of the sequence number are known as seqZero.
        val sequenceZero = (pdu.sequence and 0x1FFFu).toUShort()
        // Number of segments to be sent.
        val count = (pdu.transportPdu.size + 11) / 12

        // Create all segments to be sent.
        outgoingSegments[sequenceZero] = OutgoingSegments(
            destination = pdu.destination,
            segments = MutableList(
                size = count,
                init = { index ->
                    SegmentedAccessMessage.init(
                        pdu = pdu, networkKey = networkKey, offset = index.toUByte()
                    )
                }
            )
        )

        // Store the TTL with which the segments are to be sent.
        segmentTtl[sequenceZero] = initialTtl ?: provisionerNode.defaultTTL
                ?: networkManager.networkParameters.defaultTtl
        // Initialize the retransmission counters.
        if (pdu.destination is UnicastAddress) {
            remainingNumberOfUnicastRetransmissions[sequenceZero] =
                RemainingNumberOfUnicastRetransmissions(
                    total = networkParams.sarUnicastRetransmissionsCount,
                    withoutProgress = networkParams.sarUnicastRetransmissionsWithoutProgressCount
                )
        } else {
            remainingNumberOfMulticastRetransmissions[sequenceZero] =
                networkManager.networkParameters.sarMulticastRetransmissionsCount
        }
        sendSegments(sequenceZero = sequenceZero)
    }

    /**
     * Sends a Heartbeat message.
     *
     * @param heartbeat   Heartbeat message to be sent.
     * @param networkKey  Network key to be used to encrypt the message.
     */
    suspend fun send(heartbeat: HeartbeatMessage, networkKey: NetworkKey) {
        val message = ControlMessage(heartbeatMessage = heartbeat, networkKey = networkKey)
        try {
            logger?.i(LogCategory.LOWER_TRANSPORT) { "Sending $message" }
            networkManager.networkLayer.send(
                pdu = message,
                type = PduType.NETWORK_PDU,
                ttl = heartbeat.initialTtl
            )
        } catch (e: Exception) {
            logger?.e(LogCategory.LOWER_TRANSPORT) {
                "Failed to send Heartbeat Message: $e"
            }
        }
    }

    /**
     * Cancels sending segmented Upper Transport PDU.
     *
     * @param segmentedPdu Segmented Upper Transport PDU to be cancelled.
     */
    fun cancelSending(segmentedPdu: UpperTransportPdu) {
        val sequenceZero = (segmentedPdu.sequence and 0x1FFFu).toUShort()
        logger?.w(LogCategory.LOWER_TRANSPORT) {
            "Cancelling sending of message with seqZero: $sequenceZero"
        }
        outgoingSegments.remove(sequenceZero)
        unicastRetransmissionsTimers.remove(sequenceZero)?.let {
            it.cancel()
            it.purge()
        }
        remainingNumberOfUnicastRetransmissions.remove(sequenceZero)
        multicastRetransmissionsTimers.remove(sequenceZero)?.let {
            it.cancel()
            it.purge()
        }
        remainingNumberOfMulticastRetransmissions.remove(sequenceZero)
    }

    /**
     * Returns whether the Lower Transport Layer is in progress of receiving a segmented message
     * from the given address.
     *
     * @param address Address of the sender.
     * @return true if some, but not all packets for a segmented message were received from the
     *         given source address or false if no packets were received or the message was complete
     *         before calling this method.
     */
    fun isReceivingMessage(address: Address): Boolean = incompleteSegments.any {
        ((it.key shr 16) and 0xFFFFu) == address.toUInt()
    }

    /**
     * This method checks the given Network PDU against replay attacks. Unsegmented messages are
     * checked against their sequence number.
     *
     * Segmented messages are checked against the SeqAuth value of the first segment of the message.
     * Segments may be received in random order and unless the message SeqAuth is always greater,
     * the replay attack is not possible.
     *
     * Note: Messages sent to a Unicast Address assigned to other Nodes than the local one are not
     *       checked against reply attacks.
     *
     * @param networkPdu: The Network PDU to validate.
     * @return true if the message is valid and not a replay attack, false if otherwise.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun checkAgainstReplayAttack(networkPdu: NetworkPdu): Boolean {
        // Do not check for replay attacks against messages that are not sent to the local node.
        // Therefore, return True, if the destination is not a unicast address || if the destination
        // is a unicast address, but the local node is not assigned for that address.
        require(
            networkPdu.destination !is UnicastAddress ||
                    (network.localProvisioner?.node?.containsElementWithAddress(
                        address = networkPdu.destination
                    ) == true)
        ) { return true }

        // The SeqAuth value of the message.
        // SeqAuth is 56-bit long and contains the IV Index (32-bit) and SEQ(24-bit).
        val receivedSeqAuth = (networkPdu.ivIndex.toULong() shl 24) or networkPdu.sequence.toULong()
        // Last SeqAuth value received from the source Element.
        val source = networkPdu.source as UnicastAddress
        val lastSeqAuth = storage.lastSeqAuthValue(uuid = network.uuid, source = source)
        if (lastSeqAuth != null) {
            // In general, the SeqAuth of the received message must be greater than SeqAuth of any
            // previously received message from the same source. However, for SAR (Segmentation and
            // Reassembly) sessions, the SeqAuth of the message must be checked not the SeqAuth of
            // the segment. If SAR is active (at last one segment for the same SeqAuth has been
            // previously received), the segments ma be processed in an order. The SeqAuth of this
            // message must be greater or equal to the last one.
            var reassemblyInProgress = false
            networkPdu.takeIf {
                networkPdu.isSegmented && networkPdu.sequenceZero != null
            }?.let { networkPdu ->
                val key = (networkPdu.source.address.toUInt() shl 16) or
                        (networkPdu.sequenceZero!! and 0x1FFFu).toUInt()
                reassemblyInProgress = incompleteSegments[key] != null ||
                        acknowledgements[networkPdu.source.address]?.sequenceZero == networkPdu.sequenceZero!!
            }

            // As the messages are processed in a concurrent queue, it may happen that two messages
            // sent almost immediately were received in the right order, but are processed in the
            // opposite order. To handle that case, the previous SeqAuth is stored. If the received
            // message has SeqAuth less than the last one, but greater thant he previous one, it
            // could not be used to reply attack, as no message with that SeqAuth was ever reached.
            var missed = false
            storage.previousSeqAuthValue(uuid = network.uuid, source = source)
                ?.let { previousSeqAuth ->
                    missed = (receivedSeqAuth < lastSeqAuth) && (receivedSeqAuth > previousSeqAuth)
                }

            // Validate
            require((receivedSeqAuth > lastSeqAuth) || missed || reassemblyInProgress) {
                // Ignore that message.
                logger?.w(LogCategory.LOWER_TRANSPORT) {
                    "Discarding packet(seqAuth: $receivedSeqAuth, expected > $lastSeqAuth)."
                }
                return false
            }

            // The message is valid. Remember the previous SeqAuth.
            val newPreviousSeqAuth = min(receivedSeqAuth, lastSeqAuth)
            storage.storePreviousSeqAuthValue(
                uuid = network.uuid,
                source = networkPdu.source,
                seqAuth = newPreviousSeqAuth
            )

            // If the message was processed after its successor, don;t overwrite the last SeqAuth
            if (missed) return true
        }
        // SeqAuth is valid. Store the new sequence authentication value.
        storage.storeLastSeqAuthValue(
            uuid = network.uuid,
            source = source,
            lastSeqAuth = receivedSeqAuth
        )
        return true
    }

    /**
     * Handles the segment created from the given network PDU.
     *
     * @param segment     The segment to be handled.
     * @param networkPdu  The network PDU from which the segment was created.
     * @return The reassembled message if all segments were received, or null if not.
     */
    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun assemble(
        segment: SegmentedMessage,
        networkPdu: NetworkPdu,
    ): LowerTransportPdu? {
        val key = (networkPdu.source.address shl 16).toUInt() or
                (segment.sequenceZero and 0x1FFFu).toUInt()

        // If the received segment comes from an already completed and acknowledged message, send
        // the same ACK immediately.
        acknowledgements[segment.source.address]?.takeIf { lastAck ->
            lastAck.sequenceZero == segment.sequenceZero
        }?.let { lastAck ->
            network.localProvisioner?.node?.let { provisionerNode ->
                // The lower transport layer shall not send more than one Segment Acknowledgement
                // Message for the same SesAuth ub a period of
                // [NetworkParameters.completeAcknowledgementTimerInterval].
                require(acknowledgementTimers[key] == null) {
                    logger?.v(LogCategory.LOWER_TRANSPORT) {
                        "Message already acknowledged, ACK sent recently"
                    }
                    return null
                }

                acknowledgementTimers[key] = Timer().apply {
                    schedule(
                        delay = networkParams.completeAcknowledgementTimerInterval.inWholeMilliseconds
                    ) {
                        // Until this timer is not executed no Segment Acknowledgement Message will
                        // be sent for the same completed message.
                        acknowledgementTimers.remove(key)?.also {
                            it.cancel()
                            it.purge()
                        }
                    }
                }
                // Mpw we are sure that the ACK has not been sent in a while.
                logger?.v(LogCategory.LOWER_TRANSPORT) {
                    "Message already acknowledged, sending ACK immediately"
                }
                val ttl = if (networkPdu.ttl > 0u) {
                    provisionerNode.defaultTTL ?: networkManager.networkParameters.defaultTtl
                } else 0u
                sendAck(ack = lastAck, ttl = ttl)
            } ?: run {
                acknowledgements.remove(segment.source.address)
            }
            return null
        }

        // Remove the last ACK. The source Node has sent a new message, so the last ACK must have
        // been received.
        acknowledgements.remove(segment.source.address)
        if (segment.isSingleSegment) {
            val segments = listOf(segment)
            val message = segments.reassembled()
            logger?.i(LogCategory.LOWER_TRANSPORT) { "$message received" }
            // A single segment message may immediately be acknowledged.
            network.localProvisioner?.node?.takeIf {
                it.containsElementWithAddress(address = networkPdu.destination)
            }?.let { provisionerNode ->
                val ttl = if (networkPdu.ttl > 0u) {
                    provisionerNode.defaultTTL ?: networkManager.networkParameters.defaultTtl
                } else 0u
                sendAck(segments = segments, ttl = ttl)
            }
            return message
        } else {
            // If a message is composed of multiple segments, they all need to be received before it
            // can be processed.
            if (incompleteSegments[key] == null) {
                incompleteSegments[key] = MutableList(size = segment.count, init = { null })
            }

            require(incompleteSegments[key]!!.size > segment.index) {
                // Segment is invalid. We can stop here.
                logger?.w(LogCategory.LOWER_TRANSPORT) { "Invalid segment" }
                return null
            }
            incompleteSegments[key]!![segment.index] = segment

            // If all segments were received, send ACK and send the PDU to Upper Transport Layer for
            // processing.
            if (incompleteSegments[key]!!.isComplete()) {
                val allSegments = incompleteSegments.remove(key)!!
                val message = allSegments.reassembled()
                logger?.i(LogCategory.LOWER_TRANSPORT) { "$message received" }
                // If the access message was targeting directly the local Provisioner...
                network.localProvisioner?.node?.takeIf {
                    it.containsElementWithAddress(address = networkPdu.destination)
                }?.let { provisionerNode ->
                    // Invalidate timers
                    discardTimers.remove(key)?.also {
                        it.cancel()
                        it.purge()
                    }
                    acknowledgementTimers.remove(key)?.also {
                        it.cancel()
                        it.purge()
                    }

                    // ...and send the ACK that all segments were received.
                    val ttl = if (networkPdu.ttl > 0u) {
                        provisionerNode.defaultTTL ?: networkManager.networkParameters.defaultTtl
                    } else 0u
                    sendAck(segments = allSegments, ttl = ttl)
                }
                return message
            } else {
                // The Provisioner shall send back acknowledgement only if the message was send
                // directly to it's Unicast Address.
                network.localProvisioner?.node?.takeIf {
                    it.containsElementWithAddress(address = networkPdu.destination)
                }?.let { provisionerNode ->
                    // If the Lower Transport Layer receives any segment while the SAR Discard Timer
                    // is active, the timer shall be restarted.
                    discardTimers[key]?.also {
                        it.cancel()
                        it.purge()
                    }
                    discardTimers[key] = Timer().apply {
                        schedule(delay = networkParams.discardTimeout.inWholeMilliseconds) {
                            incompleteSegments.remove(key)?.let { segments ->
                                val marks = segments.fold(0u) { acc, seg ->
                                    seg?.let { acc or ((1u shl seg.segmentOffset.toInt())) } ?: acc
                                }
                                logger?.w(LogCategory.LOWER_TRANSPORT) {
                                    "Discard timeout expired, cancelling message (src: " +
                                            "${
                                                MeshAddress.create((key shr 16).toUShort())
                                                    .toHexString()
                                            }, " +
                                            "seqZero: ${key and 0x1FFFu}, received segments: 0x" +
                                            marks.toHexString() + ")"
                                }
                            }
                            discardTimers.remove(key)?.also {
                                it.cancel()
                                it.purge()
                            }
                            acknowledgementTimers.remove(key)?.also {
                                it.cancel()
                                it.purge()
                            }
                        }
                    }

                    // When a segment is received the SAR Acknowledgement timer shall be
                    // (re)started.
                    acknowledgementTimers[key]?.let {
                        it.cancel()
                        it.purge()
                    }

                    val defaultTtl = provisionerNode.defaultTTL
                        ?: networkManager.networkParameters.defaultTtl
                    val ackTimerInterval = networkManager.networkParameters
                        .acknowledgementTimerInterval(segment.lastSegmentNumber)
                    acknowledgementTimers[key] = Timer().also { ackTimer ->
                        ackTimer.schedule(delay = ackTimerInterval.inWholeMilliseconds) {
                            val segments = requireNotNull(incompleteSegments[key]) {
                                acknowledgementTimers.remove(key)
                                return@schedule
                            }
                            scope.launch {
                                // When the SAR Acknowledgement timer expires, the lower transport
                                // layer shall send a Segment Acknowledgement Message.
                                val ttl = if (networkPdu.ttl > 0u) defaultTtl else 0u
                                logger?.v(LogCategory.LOWER_TRANSPORT) {
                                    "SAR Acknowledgement timer expired, sending ACK"
                                }
                                sendAck(segments = segments, ttl = ttl)

                                // If Segment Acknowledgment retransmission is enabled and the
                                // number of segments of the segmented message is longer than
                                // the SAR Segments Threshold, the lower transport layer should
                                // retransmit the acknowledgment specified number of times.
                                val initialCount =
                                    networkManager.networkParameters.sarAcknowledgementRetransmissionCount
                                var count = initialCount
                                if (count > 0u && segment.lastSegmentNumber >=
                                    networkManager.networkParameters.sarSegmentsThreshold
                                ) {
                                    val interval =
                                        networkManager.networkParameters.segmentReceptionInterval
                                    val timer = Timer()
                                    acknowledgementTimers[key] = timer
                                    timer.also {
                                        if (count > 1u) {
                                            it.schedule(
                                                delay = interval.inWholeMilliseconds,
                                                period = interval.inWholeMilliseconds
                                            ) {
                                                scope.launch {
                                                    logger?.v(LogCategory.LOWER_TRANSPORT) {
                                                        "Retransmitting ACK (${1u + initialCount - count}/$initialCount)"
                                                    }
                                                    sendAck(segments = segments, ttl = ttl)
                                                    // Decrement the counter.
                                                    count = (count - 1u).toUByte()
                                                    if (count == 0.toUByte()) {
                                                        it.cancel()
                                                        it.purge()
                                                        acknowledgementTimers.remove(key)
                                                    }
                                                }
                                            }
                                        } else {
                                            it.schedule(delay = interval.inWholeMilliseconds) {
                                                scope.launch {
                                                    logger?.v(LogCategory.LOWER_TRANSPORT) {
                                                        "Retransmitting ACK (${1u + initialCount - count}/$initialCount)"
                                                    }
                                                    sendAck(segments = segments, ttl = ttl)
                                                    // Decrement the counter.
                                                    count = (count - 1u).toUByte()
                                                    if (count == 0.toUByte()) {
                                                        it.cancel()
                                                        it.purge()
                                                        acknowledgementTimers.remove(key)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return null
            }
        }
    }

    /**
     * This method handles the Segment Acknowledgment Message.
     *
     * @param ack The Segment Acknowledgment Message received.
     */
    private suspend fun handle(ack: SegmentAcknowledgementMessage) {
        // Ensure the ACK is for some message that has been sent.
        val (destination, segments) = outgoingSegments[ack.sequenceZero] ?: return
        require(ack.source.address == destination.address || ack.isOnBehalfOfLowePowerNode) {
            return
        }
        val (total, withProgress) =
            remainingNumberOfUnicastRetransmissions[ack.sequenceZero] ?: return
        val segment = requireNotNull(segments.firstNotAcknowledged()) { return }
        val message = requireNotNull(segment.message) { return }

        // Is the target Node busy?
        require(!ack.isBusy) {
            cancelTransmissionOfSegments(destination = destination, sequenceZero = ack.sequenceZero)
            // Notify the manager about this
            notifyTransmissionState(
                message = message,
                source = segment.source,
                destination = destination,
                error = LowerTransportError.Busy()
            )
            return
        }

        // Whether a progress has been made since the previous ACK
        var progress = false

        // Clear all the acknowledged segments
        for (index in segments.indices) {
            if (ack.isSegmentReceived(index)) {
                outgoingSegments[ack.sequenceZero]?.segments?.takeIf {
                    it[index] != null
                }?.let {
                    progress = true
                    it[index] = null
                }
            }
        }

        // If all the segments were acknowledged, notify the manager.
        if (outgoingSegments[ack.sequenceZero]?.segments?.hasMore() == false) {
            cancelTransmissionOfSegments(destination = destination, sequenceZero = ack.sequenceZero)
            notifyTransmissionState(
                message = message,
                source = segment.source,
                destination = destination
            )
        } else {
            // Check if the SAR Unicast Retransmission timer is running.
            require(unicastRetransmissionsTimers[ack.sequenceZero] != null) {
                // If not, that means that the segments are just being retransmitted
                // and we're done here. We shall receive a new acknowledgment in a bit.
                return
            }
            // Check if more retransmissions are possible.
            require(total > 0u && withProgress > 0u) {
                // If not, the running SAR Unicast Retransmissions timer will cancel
                // the message when it expires. Perhaps another acknowledgment will
                // be received before acknowledging all segments.
                return
            }
            // Stop the unicast retransmissions timer.
            unicastRetransmissionsTimers.remove(ack.sequenceZero)?.let {
                it.cancel()
                it.purge()
            }
            // Decrement the counters.
            // If a progress has been made, reset the remaining number of
            // retransmissions with progress to its initial value.
            remainingNumberOfUnicastRetransmissions[ack.sequenceZero] =
                RemainingNumberOfUnicastRetransmissions(
                    total = (total - 1u).toUByte(),
                    withoutProgress = if (progress)
                        networkManager.networkParameters.sarUnicastRetransmissionsWithoutProgressCount
                    else (withProgress - 1u).toUByte()
                )
            // Lastly, send again all packets that were not acknowledged.
            sendSegments(ack.sequenceZero)
        }
    }

    /**
     * Attempts to send the Segment Acknowledgement Message to the given address. It will attempt to
     * send if hte local provisioner is set and has the Unicast Address assigned.
     *
     * @param segments Segmented message to be acknowledged.
     * @param ttl      TTL to be used to send the ACK.
     */
    private suspend fun sendAck(segments: List<SegmentedMessage?>, ttl: UByte) {
        val ack = SegmentAcknowledgementMessage.init(segments)
        if (segments.isComplete()) acknowledgements[ack.destination.address] = ack
        sendAck(ack, ttl)
    }

    /**
     * Sends the given ACK on the global background queue.
     *
     * @param ack ACK to be sent.
     * @param ttl TTL to be used to send the ACK.
     */
    private suspend fun sendAck(ack: SegmentAcknowledgementMessage, ttl: UByte) {
        logger?.d(LogCategory.LOWER_TRANSPORT) { "Sending $ack" }
        try {
            networkManager.networkLayer.send(pdu = ack, type = PduType.NETWORK_PDU, ttl = ttl)
        } catch (e: Exception) {
            logger?.w(LogCategory.LOWER_TRANSPORT) { "$e" }
        }
    }

    /**
     * Sends all unacknowledged segments with the given `sequenceZero` and starts a retransmissions
     * timer.
     * Note: This is an asynchronous method, It will initiate sending the remaining segments and
     * finish immediately.
     *
     * @param sequenceZero The key to get segments from the map.
     */
    private suspend fun sendSegments(sequenceZero: UShort) {
        outgoingSegments[sequenceZero]?.takeIf {
            it.segments.isNotEmpty()
        }?.let {
            val (destination, segments) = it.destination to it.segments
            // The list of segments to be sent.
            //
            // The list contains only unacknowledged segments. Acknowledge segments are set to null
            // when the Segment Acknowledgment message is received.
            //
            // Note: When the destination is a Group or Virtual Address there are no
            //       acknowledgments, in which case all segments are unacknowledged.
            val remainingSegments = segments.unacknowledged()
            sendSegments(remainingSegments)

            // When the last remaining segment has been sent, the lower transport layer should start
            // the SAR Unicast Retransmissions timer or the SAR Multicast Retransmissions timer.
            if (destination is UnicastAddress) {
                startUnicastRetransmissionsTimer(sequenceZero)
            } else {
                startMulticastRetransmissionTimer(sequenceZero)
            }
        }
    }

    /**
     * Sends the given segments one by one with an interval determined by the segment transmission
     * interval.
     *
     * @param segments List of segments to be sent.
     */
    private suspend fun sendSegments(segments: List<SegmentedMessage>) {
        // The interval with which segments are sent by the lower transport layer.
        val segmentTransmissionInterval: Duration =
            networkManager.networkParameters.segmentTransmissionInterval

        // Start sending segments in the same order as they are in the list.
        // Note: Each segment is sent with a delay, therefore each time we check if the network
        //       manager still exists.
        for (segment in segments) {
            // Make sure the network manager is alive.
            segmentTtl[segment.sequenceZero] ?: break

            // Make sure all the segments were not already acknowledged.
            // The this will turn nil when all segments were acknowledged.
            val ttl = requireNotNull(segmentTtl[segment.sequenceZero]) { return }

            // Send the segment and wait for the segment transmission interval.
            try {
                logger?.i(LogCategory.LOWER_TRANSPORT) { "Sending $segment" }
                networkManager.networkLayer.send(
                    pdu = segment,
                    type = PduType.NETWORK_PDU,
                    ttl = ttl
                )
                delay(duration = segmentTransmissionInterval)
            } catch (e: Exception) {
                logger?.w(LogCategory.LOWER_TRANSPORT) { "$e" }
                break
            }
        }
    }

    /**
     * Starts the SAR Unicast Retransmissions timer for the message with given [sequenceZero].
     *
     * If the remaining number of retransmissions and the remaining number of retransmissions
     * without progress must be set before the timer is started.
     *
     * @param sequenceZero The key to get segments from the map.
     */
    private fun startUnicastRetransmissionsTimer(sequenceZero: UShort) {
        val remainingNumberOfUnicastRetransmissions =
            requireNotNull(remainingNumberOfUnicastRetransmissions[sequenceZero]) { return }
        val (destination, segments) = requireNotNull(outgoingSegments[sequenceZero]) { return }
        val segment = requireNotNull(segments.firstNotAcknowledged()) { return }
        val message = requireNotNull(segment.message) { return }
        val ttl = requireNotNull(segmentTtl[sequenceZero]) { return }

        // Remaining number of retransmissions of segments of an segmented message sent to a Unicast
        // Address. When the number goes to 0 the retransmissions stop.
        val remainingNumberOfRetransmissions = remainingNumberOfUnicastRetransmissions.total
        // Remaining number of retransmissions without progress of segments of an segmented message
        // sent to a Unicast Address. When the number goes to 0 the retransmissions stop.
        val remainingNumberOfUnicastRetransmissionsWithoutProgress =
            remainingNumberOfUnicastRetransmissions.withoutProgress

        // The initial value of the SAR Unicast Retransmissions timer.
        val interval = networkManager.networkParameters.unicastRetransmissionsInterval(ttl)

        // Start the SAR Unicast Retransmissions timer.
        unicastRetransmissionsTimers[sequenceZero] = fixedRateTimer(
            name = "Unicast Retransmission Timer",
            period = interval.inWholeMilliseconds
        ) {
            // The timer has expired, remove it.
            unicastRetransmissionsTimers.remove(sequenceZero)?.also {
                it.cancel()
                it.purge()
            }

            // When the SAR Unicast Retransmissions timer expires and either the remaining number of
            // retransmissions or the remaining number of retransmissions without progress is 0, the
            // lower transport layer shall cancel the transmission of the Upper Transport PDU, shall
            // delete the number of retransmissions value and the number of retransmissions without
            // progress value, shall remove the destination address and the SeqAuth stored for this
            // segmented message, and shall notify the upper transport layer that the transmission
            // of the Upper Transport PDU has timed out.
            require(
                remainingNumberOfRetransmissions > 0u &&
                        remainingNumberOfUnicastRetransmissionsWithoutProgress > 0u
            ) {
                scope.launch {
                    cancelTransmissionOfSegments(
                        destination = destination,
                        sequenceZero = sequenceZero
                    )
                    if (segment.userInitialized && !message.isAcknowledged) {
                        notifyTransmissionState(
                            message = message,
                            source = segment.source,
                            destination = destination,
                            error = LowerTransportError.Timeout()
                        )
                    }
                }
                return@fixedRateTimer
            }

            // Decrement both counters. As the SAR Unicast Retransmission timer
            // has expired, no progress has been made.
            this@LowerTransportLayer.remainingNumberOfUnicastRetransmissions[sequenceZero] =
                RemainingNumberOfUnicastRetransmissions(
                    total = (remainingNumberOfRetransmissions - 1u).toUByte(),
                    withoutProgress = (remainingNumberOfUnicastRetransmissionsWithoutProgress - 1u)
                        .toUByte()
                )
            scope.launch {
                // Send again unacknowledged segments and restart the timer.
                sendSegments(sequenceZero)
            }
        }
    }

    /**
     * Starts the SAR Multicast Retransmissions timer for the message with given [sequenceZero].
     *
     * If the remaining number of retransmissions must be set before the timer is started.
     *
     * @param sequenceZero The key to get segments from the map.
     */
    private fun startMulticastRetransmissionTimer(sequenceZero: UShort) {
        val remainingNumberOfRetransmissions =
            requireNotNull(remainingNumberOfMulticastRetransmissions[sequenceZero]) { return }
        val (destination, segments) = requireNotNull(outgoingSegments[sequenceZero]) { return }
        val segment = requireNotNull(segments.firstNotAcknowledged()) { return }
        val message = requireNotNull(segment.message) { return }


        // The initial value of the SAR Multicast Retransmissions timer.
        val interval = networkManager.networkParameters.multicastRetransmissionsInterval

        // Start the SAR Multicast Retransmissions timer.
        multicastRetransmissionsTimers[sequenceZero] = fixedRateTimer(
            name = "Multicast Retransmission Timer",
            period = interval.inWholeMilliseconds
        ) {
            // The timer has expired, remove it.
            multicastRetransmissionsTimers.remove(sequenceZero)?.also {
                it.cancel()
                it.purge()
            }

            // When the SAR Multicast Retransmissions timer expires and the remaining number of
            // retransmissions value is 0, the lower transport layer shall cancel the transmission
            // of the Upper Transport PDU, shall delete the number of retransmissions value and the
            // number of retransmissions without progress value, shall remove the destination
            // address stored for this segmented message, and shall notify the higher layer that the
            // transmission of the Upper Transport PDU has been completed.
            require(remainingNumberOfRetransmissions > 0u) {
                scope.launch {
                    cancelTransmissionOfSegments(
                        destination = destination,
                        sequenceZero = sequenceZero
                    )
                    notifyTransmissionState(
                        message = message,
                        source = segment.source,
                        destination = destination,
                        error = LowerTransportError.Timeout()
                    )
                }
                return@fixedRateTimer
            }

            // Decrement the counter.
            remainingNumberOfMulticastRetransmissions[sequenceZero] =
                (remainingNumberOfRetransmissions - 1u).toUByte()
            // Send again unacknowledged segments and restart the timer.
            networkManager.scope.launch { sendSegments(sequenceZero = sequenceZero) }
        }
    }

    /**
     * Cancels transmission of segments and any retransmission timers for a given [sequenceZero]
     *
     * @param sequenceZero The key to get segments from the map.
     * @param destination  Destination address.
     */
    private suspend fun cancelTransmissionOfSegments(
        sequenceZero: UShort,
        destination: MeshAddress,
    ) {
        remainingNumberOfUnicastRetransmissions.remove(sequenceZero)
        remainingNumberOfMulticastRetransmissions.remove(sequenceZero)
        outgoingSegments.remove(sequenceZero)
        segmentTtl.remove(sequenceZero)

        networkManager.upperTransportLayer.onLowerTransportLayerSent(
            destination = destination.address
        )
    }

    /**
     * Notifies the network manager about the transmission state.
     *
     * @param message      Message that was sent.
     * @param source       Source address of the message.
     * @param destination  Destination address of the message.
     * @param error        Error that caused the transmission to fail or null if the transmission
     */
    private suspend fun notifyTransmissionState(
        message: MeshMessage,
        source: MeshAddress,
        destination: MeshAddress,
        error: Exception? = null,
    ) {
        network.localProvisioner?.node?.element(
            address = source
        )?.let { element ->
            // TODO we need to clarify here about any error that may occur during transmission.
            error?.let {
                networkManager.clearOutgoingMessages(destination = destination)
                // networkManager.emitNetworkManagerEvent(
                //     NetworkManagerEvent.MessageSendingFailed(
                //         message = message,
                //         localElement = element,
                //         destination = destination,
                //         error = it
                //     )
                // )
            } ?: run {
                networkManager.clearOutgoingMessages(destination = destination)
                // networkManager.emitNetworkManagerEvent(
                //     NetworkManagerEvent.MessageSent(
                //         message = message,
                //         localElement = element,
                //         destination = destination
                //     )
                // )
            }
        }
    }
}

/**
 * Class containing the remaining number of unicast retransmissions.
 *
 * @property total           Remaining number of retransmissions of segmented message sent to a
 *                           Unicast Address. When the number goes to the 0 teh retransmissions
 *                           stop.
 * @property withoutProgress Remaining number of retransmissions without progress of segments of a
 *                           segmented message sent to a Unicast Address. When the number foes to 0
 *                           the retransmission stops.
 */
internal data class RemainingNumberOfUnicastRetransmissions(
    val total: UByte,
    val withoutProgress: UByte,
)

/**
 * Class that defines an outgoing segment
 *
 * @property destination Destination address.
 * @property segments    Segments to be sent.
 * @constructor creates an OutgoingSegment object.
 */
internal data class OutgoingSegments(
    val destination: MeshAddress,
    val segments: MutableList<SegmentedMessage?>,
)