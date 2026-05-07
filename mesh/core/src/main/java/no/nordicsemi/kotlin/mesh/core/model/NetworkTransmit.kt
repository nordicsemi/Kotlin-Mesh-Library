@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetworkTransmitSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetworkTransmitStatus

/**
 * The network transmit object represents the parameters of the transmissions of network layer
 * messages originating from a mesh node.
 *
 * @param count         The count property contains an integer from 1 to 8 that represents the
 *                      number of transmissions for network messages.
 * @param interval      The interval property contains an integer from 10 to 320 that represents the
 *                      interval in milliseconds between the transmissions.
 */
@ConsistentCopyVisibility
@Serializable
data class NetworkTransmit internal constructor(val count: Int, val interval: Int) {
    /**
     * Interval in milliseconds.
     */
    @Transient
    val intervalAsMilliseconds : Long = interval.toLong()

    init {
        require(count in COUNT_RANGE) {
            "Network Transmit count must be in range $COUNT_RANGE"
        }
        require(interval in INTERVAL_RANGE) {
            "Network Transmit interval must in range $INTERVAL_RANGE milliseconds"
        }
    }

    /**
     * Convenience constructor.
     *
     * @param request Network transmit settings received from a node.
     */
    internal constructor(request: ConfigNetworkTransmitSet) : this(
        count = request.count.toInt() + 1,
        interval = (request.steps.toInt() + 1) * 10
    )

    /**
     * Convenience constructor.
     *
     * @param status Network transmit status received from the node.
     */
    internal constructor(status: ConfigNetworkTransmitStatus) : this(
        count = status.count.toInt() + 1,
        interval = (status.steps.toInt() + 1) * 10
    )

    companion object {
        const val MIN_COUNT = 1
        const val MAX_COUNT = 8
        val COUNT_RANGE = MIN_COUNT..MAX_COUNT
        const val MIN_INTERVAL = 10
        const val MAX_INTERVAL = 320
        val INTERVAL_RANGE = MIN_INTERVAL..MAX_INTERVAL
    }
}