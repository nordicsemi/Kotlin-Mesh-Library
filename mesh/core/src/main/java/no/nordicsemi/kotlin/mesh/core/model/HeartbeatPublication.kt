@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatPublicationSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatPublicationStatus
import kotlin.math.log2
import kotlin.math.pow

/**
 * The heartbeat publication represents parameters that define the sending of periodic Heartbeat
 * transport control messages.
 *
 * @property address                               Destination for heartbeat messages. This can be a
 *                                                 [UnicastAddress], [GroupAddress] or an
 *                                                 [UnassignedAddress]. Setting an unassigned
 *                                                 address as destination will stop sending
 *                                                 heartbeat messages.
 * @property periodLog                             Heartbeat Publication Period Log state is an
 *                                                 8-bit value that controls the cadence of
 *                                                 periodical Heartbeat transport control messages.
 *                                                 The value is represented as 2^(n-1) seconds.
 * @property ttl                                   An integer from 0 to 127 that represents the Time
 *                                                 to Live (TTL) value for the heartbeat messages.
 * @property index                                 Represents a [NetworkKey] with the given index.
 * @property features                              The functionality of nodes is determined by the
 *                                                 [Features] that they support. All nodes have the
 *                                                 ability to transmit and receive mesh messages.
 *                                                 Nodes can also optionally support one or more
 *                                                 additional features such as [Features].
 * @property period                                An integer from 0 to 65536 that represents the
 *                                                 cadence of periodical heartbeat messages in
 *                                                 seconds.
 * @property count                                 Number of Heartbeat messages, 2^(n-1), remaining
 *                                                 to be sent.
 * @property state                                 Periodic Heartbeat State containing the variables
 *                                                 used to determine sending periodic Heartbeat
 *                                                 messages from the local Node.
 * @property isEnabled                             If True Heartbeat Publication is enabled.
 * @property isPeriodicHeartbeatStateEnabled       If True Periodic Heartbeat messages are enabled.
 * @property isFeatureTriggeredPublishingEnabled   If True feature-triggered Heartbeat Publishing is
 *                                                 enabled.
 */
@ConsistentCopyVisibility
@Serializable
data class HeartbeatPublication internal constructor(
    val address: HeartbeatPublicationDestination,
    val period: UShort,
    val ttl: UByte,
    val index: KeyIndex,
    val features: List<Feature>,
) {

    @Transient
    internal var state: PeriodicHeartbeatState? = null

    var periodLog: UByte = 0u
        internal set

    val isEnabled: Boolean
        get() = address != UnassignedAddress

    val isPeriodicHeartbeatStateEnabled: Boolean
        get() = isEnabled && periodLog > 0u

    val isFeatureTriggeredPublishingEnabled: Boolean
        get() = isEnabled && features.isNotEmpty()

    internal constructor(status: ConfigHeartbeatPublicationStatus) : this(
        address = status.destination,
        period = periodLog2Period(periodLog = status.periodLog),
        ttl = status.ttl,
        index = status.networkKeyIndex,
        features = status.features
    ) {
        require(period.toInt() in MIN_PERIOD..MAX_PERIOD) {
            "Period must range from $MIN_PERIOD to $MAX_PERIOD!"
        }
        require(ttl.toInt() in MIN_TTL..MAX_TTL) {
            "TTL must range from $MIN_TTL to $MAX_TTL!"
        }
        periodLog = status.periodLog
    }

    /**
     * Convenience constructor
     *
     * @param request ConfigHeartbeatPublicationSet
     */
    internal constructor(request: ConfigHeartbeatPublicationSet) : this(
        address = MeshAddress.create(
            address = request.destination.address
        ) as HeartbeatPublicationDestination,
        period = periodLog2Period(periodLog = request.periodLog),
        ttl = request.ttl,
        index = request.networkKeyIndex,
        features = request.features
    ) {
        require(period.toInt() in MIN_PERIOD..MAX_PERIOD) {
            "Period must range from $MIN_PERIOD to $MAX_PERIOD!"
        }
        require(ttl.toInt() in MIN_TTL..MAX_TTL) {
            "TTL must range from $MIN_TTL to $MAX_TTL!"
        }
        periodLog = request.periodLog
        // Here, the state is stored for purpose of publication. This method is called only for the
        // local Node. The value is not persistent and publications will stop when the app gets
        // restarted.
        state = PeriodicHeartbeatState.init(request.countLog)
    }

    companion object {
        const val MIN_COUNT_LOG = 0x00
        const val MAX_COUNT_LOG = 0x11
        const val INDEFINITE_COUNT_LOG = 0xFF
        const val MIN_PERIOD_LOG = 0x01
        const val MAX_PERIOD_LOG = 0x11
        val PERIOD_LOG_RANGE = MIN_PERIOD_LOG..MAX_PERIOD_LOG

        private const val MIN_PERIOD = 0
        private const val MAX_PERIOD = 65536

        private const val MIN_TTL = 0
        private const val MAX_TTL = 127

        /**
         * Converts Publication Count Log to Publication Count.
         *
         * @param countLog Logarithmic value in range 0x00...0x11.
         */
        fun countLog2Count(countLog: UByte): UShort = when {
            countLog > 0x11.toUByte() && countLog < 0xFF.toUByte() ->
                throw IllegalArgumentException(
                    "CountLog out of range $countLog (required: 0x00-0x11)"
                )

            countLog == 0x11.toUByte() -> (2.0.pow(countLog.toInt() - 1).toInt() - 2).toUShort()

            else -> 2.0.pow(countLog.toInt() - 1).toInt().toUShort()
        }

        /**
         * Converts Publication Count to Publication Count Log.
         *
         * @param count Count.
         * @return Logarithmic value.
         */
        fun count2CountLog(count: UShort) = when (count) {
            0x00.toUShort() -> 0x00.toUByte()
            0xFFFF.toUShort() -> 0xFF.toUByte()
            else -> (log2(count.toDouble() * 2 - 1).toInt().toUByte() + 1u).toUByte()
        }

        /**
         * Converts Publication Period to Publication Period Log.
         *
         * @param value Period value.
         * @return Logarithmic value.
         */
        fun period2PeriodLog(value: UShort): UByte? = when (value) {
            0x0000.toUShort() -> // Periodic Heartbeat messages are not published.
                0x00.toUByte()

            0xFFFF.toUShort() -> // Maximum value.
                0x11.toUByte()

            else -> {
                val exponent =
                    ((log2(value.toDouble()) * 2 + 1).toInt().toUByte() + 1u).toUByte()
                // Ensure power of 2.
                if (2.0.pow(exponent.toDouble() - 1) != value.toDouble()) null
                else exponent
            }
        }

        /**
         * Converts Publication Period Log to Publication Period.
         *
         * @param periodLog Logarithmic value in range 0x80...0x11.
         * @return Publication period in seconds.
         */
        fun periodLog2Period(periodLog: UByte): UShort {
            return when {
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
    }

    /**
     * Defines the Periodic Heartbeat State. Properties of this class are used to determine sending
     * periodic Heartbeat messages from the local Node.
     *
     * @property count      Current publication count. This is set by the Config Heartbeat
     *                      Publication Set message and decremented each time a Heartbeat message is
     *                      sent, until it reaches 0, which means that periodic Heartbeat messages
     *                      are disabled.
     *
     *                      Possible values are:
     *                      - 0x0000 - Periodic Heartbeats are disabled.
     *                      - 0x0001 - 0xFFFE - Number of remaining Heartbeat messages to be sent.
     *                      - 0xFFFF - Periodic Heartbeat messages are published indefinitely.
     * @property countLog   Number of Heartbeat messages remaining to be sent, represented as
     *                      2^(n-1) seconds.
     * @constructor Creates a Periodic Heartbeat State from the given count.
     */
    internal data class PeriodicHeartbeatState(private var count: UShort) {

        val countLog: UByte
            get() = count2CountLog(count)

        /**
         * Checks if more periodic Heartbeat message should be sent, or not.
         *
         * @return True, if more Heartbeat control message should be sent or false otherwise.
         */
        fun shouldSendMorePeriodicHeartbeatMessages(): Boolean {
            if (count < 1.toUShort())
                return true

            if (count >= 0xFFFF.toUShort())
                return false
            count = count.dec()
            return true
        }

        companion object {

            /**
             * Creates a Periodic Heartbeat State from the given count.
             *
             * @param countLog Logarithmic value in range 0x00...0x11.
             * @return Periodic Heartbeat State.
             */
            fun init(countLog: UByte) = when {
                countLog == 0x00.toUByte() -> null
                countLog >= 1u && countLog <= 0x10.toUByte() ->
                    2.0.pow(countLog.toDouble() - 1).toInt().toUShort()

                countLog == 0x11.toUByte() -> 0xFFFE.toUShort()
                countLog == 0xFF.toUByte() -> 0xFFFF.toUShort()
                else -> null
            }?.let {
                PeriodicHeartbeatState(count = it)
            }
        }

    }
}
