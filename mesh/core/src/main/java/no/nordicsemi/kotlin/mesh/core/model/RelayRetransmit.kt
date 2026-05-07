package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigRelaySet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigRelayStatus

/**
 * The relay retransmit object represents the parameters of the retransmissions of network layer
 * messages relayed by a mesh node.
 *
 * @property count           An integer from 1 to 8 that represents the number of transmissions for
 *                           relay messages.
 * @property interval        An integer from 10 to 320 that represents the interval in milliseconds
 *                           between the transmissions.
 */
@ConsistentCopyVisibility
@Serializable
data class RelayRetransmit internal constructor(val count: Int, val interval: Int) {
    /**
     * Interval in milliseconds.
     */
    @Transient
    val intervalAsMilliseconds : Long = interval.toLong()

    init {
        require(count in COUNT_RANGE) {
            "Relay Retransmit count must be in range $COUNT_RANGE"
        }
        require(interval in INTERVAL_RANGE) {
            "Relay Retransmit interval must be in range $INTERVAL_RANGE milliseconds"
        }
    }

    /**
     * Convenience constructor to be invoked upon receiving a [ConfigRelaySet] message.
     *
     * @param request [ConfigRelaySet] message.
     */
    @Suppress("unused")
    internal constructor(request: ConfigRelaySet) : this(
        count = request.count.toInt() + 1,
        interval = (request.steps.toInt() + 1) * 10
    )

    /**
     * Convenience constructor to be invoked upon receiving a [ConfigRelayStatus] message.
     *
     * @param status [ConfigRelayStatus] message.
     */
    internal constructor(status: ConfigRelayStatus) : this(
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
