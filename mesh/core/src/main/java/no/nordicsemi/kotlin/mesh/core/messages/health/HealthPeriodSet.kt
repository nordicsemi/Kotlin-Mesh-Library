@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.health

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.HealthMessageInitializer

/**
 * A Health Period Set is an acknowledged message used to set the current Health Fast Period Divisor
 * state of an Element.
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
class HealthPeriodSet(
    val fastPeriodDivisor: UByte
) : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = HealthPeriodStatus.opCode
    override val parameters: ByteArray = byteArrayOf(fastPeriodDivisor.toByte())

    init {
        require(fastPeriodDivisor <= 15u) { "Fast Period Divisor must be between 0 and 15" }
    }

    override fun toString() = "HealthPeriodSet(fastPeriodDivisor: $fastPeriodDivisor)"

    companion object Initializer : HealthMessageInitializer {
        override val opCode = 0x8035u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { params ->
                HealthPeriodSet(params[0].toUByte())
            }
    }
}
