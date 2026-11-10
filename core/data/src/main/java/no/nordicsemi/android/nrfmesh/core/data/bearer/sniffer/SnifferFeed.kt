package no.nordicsemi.android.nrfmesh.core.data.bearer.sniffer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Receives PDUs to be passed on to the nRF Mesh Sniffer. Must never block.
 */
fun interface SnifferPduSink {
    fun offer(pdu: SnifferPdu)
}

/**
 * Delivers a single PDU to the nRF Mesh Sniffer. May block, must not throw.
 */
fun interface SnifferTransport {
    fun deliver(pdu: SnifferPdu)
}

/**
 * Queues PDUs and delivers them in order, one at a time, to a [SnifferTransport] from [scope].
 *
 * [offer] never blocks. When the queue is full, the oldest PDU is dropped. A single consumer
 * coroutine runs in [scope] until it is cancelled.
 *
 * @param transport Where the PDUs are delivered.
 * @param scope     Scope running the consumer, should use a background dispatcher.
 * @param capacity  Maximum number of queued PDUs.
 */
class SnifferFeed(
    private val transport: SnifferTransport,
    scope: CoroutineScope,
    capacity: Int = DEFAULT_CAPACITY,
) : SnifferPduSink {
    private val queue = Channel<SnifferPdu>(
        capacity = capacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        scope.launch {
            try {
                for (pdu in queue) {
                    // Whatever happens in the Sniffer must not stop the feed.
                    runCatching { transport.deliver(pdu) }
                }
            } finally {
                queue.close()
            }
        }
    }

    override fun offer(pdu: SnifferPdu) {
        queue.trySend(pdu)
    }

    companion object {
        const val DEFAULT_CAPACITY = 256
    }
}
