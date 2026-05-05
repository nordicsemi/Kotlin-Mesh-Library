@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.bearer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import no.nordicsemi.kotlin.mesh.bearer.BearerError.PduTypeNotSupported

/**
 * Transmitter is responsible for delivering messages to the mesh network.
 */
interface Transmitter {

    /**
     * Sends the given data over the transmitter.
     *
     * @param pdu     Data to be sent.
     * @param type    Type of the PDU.
     * @throws PduTypeNotSupported if the PDU type is not supported by the transmitter.
     */
    suspend fun send(pdu: ByteArray, type: PduType)
}

/**
 * Receiver is responsible for receiving messages from the mesh network.
 *
 * @property pdus A flow that emits the Reassembled PDU received from the Proxy Protocol handler.
 */
interface Receiver {

    val pdus: Flow<Pdu>

}

/**
 * Bearer is responsible for sending and receiving messages to and from the mesh network.
 *
 * @property state               A flow that emits events whenever the bearer state changes.
 * @property supportedTypes      List of supported PDU types.
 * @property isOpen              Returns true if the bearer is open, false otherwise.
 */
interface Bearer : Transmitter, Receiver {
    val state: StateFlow<BearerEvent>
    val supportedTypes: Array<PduTypes>
    val isOpen: Boolean

    /**
     * Opens the bearer and is ready for sending and receiving messages.
     */
    suspend fun open()

    /**
     * Closes the bearer.
     */
    suspend fun close()

    /**
     * Sends the given data over the bearer.
     *
     * Data longer than MTU will automatically be segmented using the bearer protocol if bearer
     * implements segmentation.
     *
     * @param pdu     Data to be sent.
     * @param type    Type of the PDU.
     * @throws PduTypeNotSupported if the PDU type is not supported by the bearer.
     * @throws BearerError.Closed if the bearer is not open.
     */
    override suspend fun send(pdu: ByteArray, type: PduType)

    /**
     * Returns whether the bearer supports the given message type.
     *
     * @param type PDU type.
     * @return True if the bearer supports the given message type, false otherwise.
     */
    fun supports(type: PduType): Boolean = try {
        supportedTypes.contains(PduTypes.from(type.mask))
    } catch (e: PduTypeNotSupported) {
        false
    }
}

/**
 * A mesh bearer is used to send mesh messages to provisioned nodes.
 */
interface MeshBearer : Bearer