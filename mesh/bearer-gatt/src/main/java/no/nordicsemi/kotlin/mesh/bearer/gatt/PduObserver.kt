package no.nordicsemi.kotlin.mesh.bearer.gatt

import no.nordicsemi.kotlin.mesh.bearer.Pdu
import no.nordicsemi.kotlin.mesh.bearer.PduType

/**
 * Observes the PDUs sent and received by a [BaseGattBearer].
 *
 * Set on [BaseGattBearer.pduObserver]. The callbacks are invoked synchronously on the thread that
 * sends or receives the PDU, so implementations must return quickly and must not block. Exceptions
 * thrown by the observer are swallowed by the bearer.
 */
interface PduObserver {

    /**
     * Called when a PDU is about to be sent, before it is segmented.
     *
     * @param pdu  PDU without the Proxy header.
     * @param type Type of the PDU.
     */
    fun onPduSent(pdu: ByteArray, type: PduType)

    /**
     * Called when a PDU has been received and reassembled.
     *
     * @param pdu Reassembled PDU without the Proxy header.
     */
    fun onPduReceived(pdu: Pdu)
}
