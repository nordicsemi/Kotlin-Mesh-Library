package no.nordicsemi.android.nrfmesh.core.data.bearer.sniffer

import no.nordicsemi.kotlin.mesh.bearer.Pdu
import no.nordicsemi.kotlin.mesh.bearer.PduType
import no.nordicsemi.kotlin.mesh.bearer.gatt.PduObserver
import kotlin.time.Clock

/**
 * Forwards the PDUs of a GATT bearer to the nRF Mesh Sniffer.
 *
 * The timestamp is taken when the bearer calls the observer, before the PDU is queued. The
 * [address] is attached to received PDUs only, never to sent ones.
 *
 * @param sink    Receiver of the PDUs.
 * @param bearer  Kind of the observed bearer.
 * @param address Returns the identifier of the peripheral.
 * @param name    Returns the name of the peripheral, if known.
 * @param now     Returns the current time in epoch milliseconds.
 */
class SnifferPduObserver(
    private val sink: SnifferPduSink,
    private val bearer: SnifferBearer,
    private val address: () -> String?,
    private val name: () -> String?,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : PduObserver {

    override fun onPduSent(pdu: ByteArray, type: PduType) = offer(
        SnifferPdu(
            timestamp = now(),
            data = pdu.copyOf(),
            type = SnifferPdu.proxyType(type),
            direction = SnifferDirection.SENT,
            bearer = bearer,
            address = null,
            name = name()
        )
    )

    override fun onPduReceived(pdu: Pdu) = offer(
        SnifferPdu(
            timestamp = now(),
            data = pdu.data.copyOf(),
            type = SnifferPdu.proxyType(pdu.type),
            direction = SnifferDirection.RECEIVED,
            bearer = bearer,
            address = address(),
            name = name()
        )
    )

    private fun offer(pdu: SnifferPdu) {
        runCatching { sink.offer(pdu) }
    }
}
