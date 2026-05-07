@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toDuration

/**
 * The mesh message security enum determines authentication level which shall be used when
 * encrypting a segmented mesh message.
 *
 * This filed is used to determine the TransMIC.
 *
 * The Message Integrity Check for Transport (TransMIC) is a 32-bit or 64-bit field that
 * authenticates that the Access payload has not been changed.
 *
 * For a segmented message, where SEG is set to 1, the size of the TransMIC is determined by the
 * value of the SZMIC field in the Lower Transport PDU. For unsegmented messages, SZMIC is 32 bits
 * for data messages.
 *
 * @property transportMicSize The size of the Transport MIC.
 */
sealed class MeshMessageSecurity {

    val transportMicSize: UByte
        get() = when (this) {
            is High -> 4u
            is Low -> 8u
        }

    /**
     * Message will be sent with 64-bit Transport MIC.
     *
     * Note: Unsegmented messages cannot be sent with 64-bit Transport MIC.
     */
    object High : MeshMessageSecurity() {
        override fun toString() = "High (64-bit TransMIC)"
    }

    /**
     * Message will be sent with 32-bit Transport MIC.
     */
    object Low : MeshMessageSecurity() {
        override fun toString() = "Low (32-bit TransMIC)"
    }
}

/**
 * A functional interface containing a decoder for a mesh message.
 */
fun interface HasInitializer {
    /**
     * Initializes the mesh message based on the given parameters.
     *
     * @param parameters Byte array containing the payload of the mesh message.
     * @return the decoded [BaseMeshMessage].
     */
    fun init(parameters: ByteArray?): BaseMeshMessage?
}

/**
 * Defines the Op Code of a mesh message.
 */
interface HasOpCode {
    /** The Op Code of the message. */
    val opCode: UInt
}

/**
 * The base interface of every mesh message. Mesh messages can be sent to and received from a mesh
 * network.
 */
interface BaseMeshMessage {
    /** Access layer payload, including the Op Code. */
    val parameters: ByteArray?
}

/**
 * A base decoder interface for all mesh message decoders.
 */
interface BaseMeshMessageInitializer : HasInitializer

/**
 * The base interface of every mesh message. Mesh messages can be sent to and received from a mesh
 * network.
 *
 * Parameters [security] and [isSegmented] are checked and should be set only for outgoing messages.
 *
 * @property opCode           Op Code of the message.
 * @property security         Defines if the message should be sent or has been sent using 32-bit or
 *                            64-bit TransMIC value. By default [MeshMessageSecurity.Low] is used.
 *                            Only Segmented Access Messages can use 64-bit MIC. If the payload is
 *                            shorter than 11 bytes, make sure you return `true` from
 *                            [MeshMessage.isSegmented], otherwise this field will be ignored.
 * @property isSegmented      Defines if the message should be sent or was sent as Segmented Access
 *                            Message. By default, this  returns `false`. To force segmentation for
 *                            shorter messages return `true` despite payload length. If payload size
 *                            is longer than 11 bytes this field is not checked as the message must
 *                            be segmented.
 * @property isVendorMessage  Whether the message is a Vendor Message, or not.
 *
 *                            Vendor messages use 3-byte Op Codes, where the 2 most significant bits
 *                            of the first octet are set to 1. The remaining bits of the first octet
 *                            are the operation code, while the last 2 bytes are the Company
 *                            Identifier (Big Endian), as registered by Bluetooth SIG.
 * @property isAcknowledged   Defines if the message should be sent as an acknowledged message.
 */
interface MeshMessage : BaseMeshMessage, HasOpCode {

    val security: MeshMessageSecurity
        get() = MeshMessageSecurity.Low

    val isSegmented: Boolean
        get() = false

    val isVendorMessage: Boolean
        get() = opCode and 0xFFC00000.toUInt() == 0x00C00000.toUInt()

    val isAcknowledged: Boolean
        get() = opCode and 0x80000000.toUInt() == 0x80000000.toUInt()
}


/**
 * The base interface for unacknowledged messages.
 */
interface UnacknowledgedMeshMessage : MeshMessage

/**
 * The base interface for response messages.
 */
interface MeshResponse : MeshMessage

/**
 * The base interface for acknowledged messages. An acknowledged message is transmitted and
 * acknowledged by each receiving element by responding to that message. The response is typically a
 * status message. If a response is not received within an arbitrary time period, the message will
 * be retransmitted automatically until the timeout occurs.
 *
 * @property responseOpCode Op Code of the response message.
 */
interface AcknowledgedMeshMessage : MeshMessage {
    val responseOpCode: UInt
}

/**
 * A mesh message containing the operation status.
 *
 * @property isSuccess True if the message was successful or false otherwise.
 * @property message   Status message of the operation.
 */
interface StatusMessage : MeshMessage {

    val isSuccess: Boolean

    val message: String
}

/**
 * A message with Transaction Identifier.
 *
 * The Transaction Identifier will automatically be set and incremented each time a message is sent.
 * The counter is reused for all the types that extend this protocol.
 *
 * @property tid                  Transaction Identifier of the message. if not set, this field will
 *                                automatically be set when the message is being sent or received.
 * @property continueTransaction  Defines if a message should start a new transaction.
 *
 *                                The messages within a transaction carry the cumulative values of a
 *                                field. In case one or more messages within a transaction are not
 *                                received by the Server (e.g., as a result of radio collisions),
 *                                the next received message will make up for the lost messages,
 *                                carrying cumulative values of the field.
 *
 *                                A new transaction is started when this field is set to `true`, or
 *                                when the last message of the transaction was sent 6 or more
 *                                seconds earlier.
 *
 *                                This defaults to `false`, which means that each new message will
 *                                receive a new transaction identifier (if not set explicitly).
 */
interface TransactionMessage : MeshMessage {

    var tid: UByte?

    val continueTransaction: Boolean
        get() = false

    /**
     * Checks whether this message is a continuation of another transaction message sent before at
     * the given timestamp
     *
     * @param previousTid Transaction Identifier of the previous message.
     * @param timestamp   Timestamp of the previous message.
     * @return true if the message is a continuation of the previous message.
     */
    @OptIn(ExperimentalTime::class)
    fun isNewTransaction(previousTid: UByte, timestamp: Instant) = tid != previousTid ||
            (Clock.System.now() - timestamp) < 6.toDuration(DurationUnit.SECONDS)
}

/**
 * The base interface for a message that can initiate a non-immediate state transition.
 *
 * @property transitionTime The Transition Time field identifies the time that an Element will take
 *                          to transition to the target state from the present state.
 * @property delay          Message execution delay in 5 millisecond steps.
 *
 *                          The purpose of this field is to synchronize transitions initiated by
 *                          sending the same message multiple times with a short delay. E.g.:
 *                          A Node would want to send a Generic On Off Set Unacknowledged message to
 *                          a Group Address. In order to increase changes of successful delivery,
 *                          such message can be repeated. The first message could be sent with
 *                          longer [TransitionMessage.delay] and each following with a shorter one,
 *                          so when different Nodes receive different messages, the action they take
 *                          seems more synchronized.
 *
 *                          This filed has to be set together with [transitionTime].
 */
interface TransitionMessage : MeshMessage {

    val transitionTime: TransitionTime?

    val delay: UByte?
}

/**
 * The base interface for a message that's sent as a response to a [TransitionMessage].
 *
 * @property remainingTime Defines the time that an element will take to transition to the target
 *                         state from the present state.
 */
interface TransitionStatusMessage : MeshMessage {

    val remainingTime: TransitionTime?
}