@file:Suppress("unused", "MemberVisibilityCanBePrivate", "LocalVariableName")

package no.nordicsemi.kotlin.mesh.core.model

import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nordicsemi.kotlin.mesh.core.layers.uppertransport.HeartbeatMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatSubscriptionSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatSubscriptionStatus
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * The heartbeat subscription object represents parameters that define the receiving of periodical
 * Heartbeat transport control messages.
 *
 * @property source         The source property contains the source address for Heartbeat messages
 *                          that a node processes.
 * @property destination    The destination property represents the destination address for the
 *                          Heartbeat messages.
 *
 * @property state          The state property contains the state of the Heartbeat subscription.
 * @property isEnabled      Returns true if the Heartbeat subscription is enabled.
 */
@ConsistentCopyVisibility
@Serializable
data class HeartbeatSubscription internal constructor(
    val source: HeartbeatSubscriptionSource,
    val destination: HeartbeatSubscriptionDestination,
) {

    @Transient
    var state: State? = null
        private set

    val isEnabled: Boolean
        get() = state?.let { it.periodLog > 0u } ?: false

    /**
     * Convenience constructor to use when sending a message to disable a heartbeat subscription.
     */
    internal constructor() : this(source = UnassignedAddress, destination = UnassignedAddress)

    internal constructor(request: ConfigHeartbeatSubscriptionSet) : this(
        source = request.source,
        destination = request.destination
    ) {
        // Here, the state is stored for purpose of subscription.
        // This method is called only for the local Node. The value is not persistent and
        // subscription will stop when the app gets restarted.
        state = State(_periodLog = request.periodLog)
    }

    internal constructor(status: ConfigHeartbeatSubscriptionStatus) : this(
        source = status.source,
        destination = status.destination
    ) {
        // The current state of the heartbeat subscription is not set for 2 reasons:
        // - it is dynamic - the device is listening for heartbeat messages for some time only,
        // - it is not saved in the Configuration Database.
        //
        state = State(_periodLog = status.periodLog)
    }

    /**
     * Checks if the received Heartbeat message matches the subscription parameters.
     *
     * @param heartbeat Received Heartbeat message.
     * @return true if the Heartbeat message matches the subscription parameters.
     */
    internal fun matches(heartbeat: HeartbeatMessage) =
        source == heartbeat.source && destination == heartbeat.destination

    /**
     * Updates the counter based on received Heartbeat message.
     *
     * @param heartbeat Received Heartbeat message.
     */
    internal fun updateIfMatches(heartbeat: HeartbeatMessage) {
        require(isEnabled) { return }
        val state = requireNotNull(state) { return }
        require(matches(heartbeat)) { return }

        if (state.count < 0xFFFF.toUShort()) {
            state.count = (state.count + 1u).toUShort()
        }
        state.minHops = min(state.minHops.toInt(), heartbeat.hops.toInt()).toUByte()
        state.maxHops = max(state.maxHops.toInt(), heartbeat.hops.toInt()).toUByte()
    }

    companion object {
        const val PERIOD_LOG_MIN = 0x00
        const val PERIOD_LOG_MAX = 0x11
        val PERIOD_LOG_RANGE = 0x01u..0x11u

        /**
         * Converts Subscription Count to Subscription Count Log.
         *
         * This method uses algorithm compatible to Table 4.1 in Bluetooth Mesh Profile
         * Specification 1.0.1.
         *
         * @param value Count.
         * @return Logarithmic value.
         */
        private fun countToCountLog(value: UShort) = when (value) {
            0x0000.toUShort() -> 0x00.toUByte() // No Heartbeat messages are published.
            0xFFFF.toUShort() -> 0xFF.toUByte() // Maximum value.
            else -> (log2(value.toDouble()) + 1).toInt().toUByte()
        }

        /**
         * Converts Subscription Period to Subscription Period Log.
         *
         * @param remainingPeriod Remaining period in seconds.
         * @return Logarithmic value.
         */
        private fun period2PeriodLog(remainingPeriod: Duration): UByte {
            val period = remainingPeriod.toDouble(DurationUnit.SECONDS)
            return when {
                period == 0.0 -> 0x00.toUByte()
                period >= 0xFFFF -> 0x11.toUByte()
                else -> (log2(remainingPeriod.toDouble(DurationUnit.SECONDS)) + 1).toInt()
                    .toUByte()
            }
        }

        /**
         * Converts Subscription Period Log to Subscription Period.
         *
         * @param periodLog Logarithmic value in range 0x80...0x11.
         * @return Subscription period in seconds.
         */
        fun periodLog2Period(periodLog: UByte): UShort = when {
            periodLog == 0x00.toUByte() -> // Periodic Heartbeat messages are not published.
                0x0000.toUShort()

            periodLog >= 0x01u && periodLog <= 0x10u -> // Period = 2^(periodLog - 1) seconds.
                2.0.pow((periodLog - 1u).toDouble()).toInt().toUShort()

            periodLog == 0x11.toUByte() -> // Maximum value.
                0xFFFF.toUShort()

            else -> throw IllegalArgumentException(
                "PeriodLog out of range $periodLog (required: 0x00-0x11)"
            )
        }
    }

    /**
     * Defines the state of the Heartbeat subscription.
     *
     * @param _periodLog     Period Log.
     * @property startDate   Start date of the subscription.
     * @property period      Period of the subscription. This controls the duration for processing
     *                       Heartbeat transport control messages. When set to 0x0000, heartbeat
     *                       messages are not processed. WHen set to a value greater than or equal
     *                       to 0x0001, Heartbeat messages are processed.
     * @property count       Heartbeat Subscription Count state is a 16-bit counter that controls
     *                       the number of periodical  Heartbeat transport control messages received
     *                       since receiving the most recent Config Heartbeat Subscription set
     *                       message. The counter stops counting at 0xFFFF.
     * @property minHops     Heartbeat Subscription Min Hops state determines the minimum hops
     *                       value registered when receiving Heartbeat messages since receiving the
     *                       most recent Config Heartbeat Subscription Set message.
     * @property maxHops     Heartbeat Subscription Max Hops state determines the maximum hops value
     *                       registered when receiving Heartbeat messages since receiving the most
     *                       recent Config Heartbeat Subscription Set message.
     * @property countLog    The Heartbeat Subscription Count Log is a representation of the
     *                       Heartbeat Subscription Count state value. The Heartbeat Subscription
     *                       Count Log and Heartbeat Subscription Count with the value 0x00 and
     *                       0x0000 are equivalent. The Heartbeat Subscription Count Log value of
     *                       0xFF is equivalent to the Heartbeat Subscription count value of 0xFFFF.
     *                       The Heartbeat Subscription Count Log value between 0x01 and 0x10 shall
     *                       represent the Heartbeat Subscription Count value, using the
     *                       transformation defined in Table 4.1, where 0xFF means that more than
     *                       0xFFFF messages were received.
     */
    class State internal constructor(_periodLog: UByte) {
        private val startDate = Clock.System.now()
        val period = periodLog2Period(_periodLog).toInt().toDuration(DurationUnit.SECONDS)
        var count = 0.toUShort()
            internal set
        var minHops = 0x7F.toUByte()
            internal set
        var maxHops = 0x00.toUByte()
            internal set

        val periodLog: UByte
            get() {
                val timeIntervalSinceSubscriptionStart = Clock.System.now() - startDate
                val remainingPeriod = period - timeIntervalSinceSubscriptionStart
                return if (remainingPeriod.inWholeSeconds >= 0) period2PeriodLog(remainingPeriod) else 0u
            }

        val countLog: UByte
            get() = countToCountLog(count)

    }
}
