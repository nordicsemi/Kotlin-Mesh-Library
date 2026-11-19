package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.RandomUpdateIntervalSteps

/**
 * Defines the response to a [PrivateBeaconGet] or [PrivateBeaconSet] message.
 *
 * @property enabled  True if the Private Beacon feature is enabled.
 * @property steps    Random Update Interval Steps in units of 10 seconds. 0 means the random field
 *                   is updated for every beacon.
 * @property interval The [RandomUpdateIntervalSteps] derived from [steps].
 */
class PrivateBeaconStatus(
    val enabled: Boolean,
    val steps: UByte = 0u,
) : ConfigResponse {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray =
        byteArrayOf(if (enabled) 0x01 else 0x00, steps.toByte())

    val interval: RandomUpdateIntervalSteps
        get() = if (steps.toInt() == 0) RandomUpdateIntervalSteps.EveryTime
        else RandomUpdateIntervalSteps.Interval(steps)

    override fun toString() = "PrivateBeaconStatus(enabled: $enabled, steps: $steps)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8062u

        /**
         * Creates a PrivateBeaconStatus with the given interval in seconds, clamped between 10
         * and 2550 seconds and rounded down to 10 s steps.
         */
        @Suppress("unused")
        fun withInterval(enabled: Boolean, seconds: Int) = PrivateBeaconStatus(
            enabled = enabled,
            steps = (seconds.coerceIn(10, 2550) / 10).toUByte()
        )

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 && it[0] in 0..1 }
            ?.let { PrivateBeaconStatus(enabled = it[0].toInt() == 0x01, steps = it[1].toUByte()) }
    }
}
