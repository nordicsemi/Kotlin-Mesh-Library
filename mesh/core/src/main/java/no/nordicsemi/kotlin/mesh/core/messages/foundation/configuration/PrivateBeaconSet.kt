package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.RandomUpdateIntervalSteps

/**
 * Defines message sent to set the Private Beacon state of the Node. [PrivateBeaconStatus] will be
 * the response to this message.
 *
 * @property enabled  True to enable the Private Beacon feature or false to disable it.
 * @property steps    Optional Random Update Interval Steps in units of 10 seconds. 0 means the
 *                    random field is updated for every beacon.
 * @property interval The [RandomUpdateIntervalSteps] or null if [steps] is not set.
 */
class PrivateBeaconSet private constructor(
    val enabled: Boolean,
    val steps: UByte?,
) : AcknowledgedConfigMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode: UInt = PrivateBeaconStatus.opCode
    override val parameters: ByteArray = byteArrayOf(if (enabled) 0x01 else 0x00) +
            (steps?.let { byteArrayOf(it.toByte()) } ?: byteArrayOf())

    val interval: RandomUpdateIntervalSteps?
        get() = steps?.let {
            if (it.toInt() == 0) RandomUpdateIntervalSteps.EveryTime
            else RandomUpdateIntervalSteps.Interval(it)
        }

    /**
     * Constructs a PrivateBeaconSet message without the random update interval.
     *
     * @param enabled True to enable the Private Beacon feature or false to disable it.
     */
    @Suppress("unused")
    constructor(enabled: Boolean) : this(enabled = enabled, steps = null)

    override fun toString() = "PrivateBeaconSet(enabled: $enabled, steps: $steps)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8061u

        /**
         * Creates a PrivateBeaconSet that enables the beacon with the given random update
         * interval steps (in units of 10 seconds).
         */
        @Suppress("unused")
        fun enabledWithSteps(steps: UByte) = PrivateBeaconSet(enabled = true, steps = steps)

        /**
         * Creates a PrivateBeaconSet that enables the beacon with the given interval in seconds.
         * The interval is clamped between 10 and 2550 seconds and rounded down to 10 s steps.
         */
        @Suppress("unused")
        fun enabledWithInterval(seconds: Int) = enabledWithSteps(
            steps = (seconds.coerceIn(10, 2550) / 10).toUByte()
        )

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { (it.size == 1 || it.size == 2) && it[0] in 0..1 }
            ?.let {
                PrivateBeaconSet(
                    enabled = it[0].toInt() == 0x01,
                    steps = if (it.size == 2) it[1].toUByte() else null
                )
            }
    }
}
