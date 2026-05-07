@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.health

import no.nordicsemi.kotlin.mesh.core.messages.HealthMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage

/**
 * A Health Period Set Unacknowledged is an unacknowledged message used to set the current
 * Health Fast Period Divisor state of an element.
 *
 * The Health Fast Period Divisor state controls the increased cadence of publishing
 * Health Current Status messages.
 *
 * Modified Publish Period is used for sending Current Health Status messages when there are
 * active faults to communicate. The value is used to divide the Publish Period state of the
 * Health Server model by `2^n` where `n `is the value of the Health Fast Period Divisor state.
 *
 * @property fastPeriodDivisor The Health Fast Period Divisor state, in range 0..15.
 */
class HealthPeriodSetUnacknowledged(
    val fastPeriodDivisor: UByte
) : UnacknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray = byteArrayOf(fastPeriodDivisor.toByte())

    init {
        require(fastPeriodDivisor <= 15u) { "Fast Period Divisor must be between 0 and 15" }
    }

    override fun toString() = "HealthPeriodSetUnacknowledged(fastPeriodDivisor: $fastPeriodDivisor)"

    companion object Initializer : HealthMessageInitializer {
        override val opCode = 0x8036u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { params ->
                HealthPeriodSetUnacknowledged(params[0].toUByte())
            }
    }
}
