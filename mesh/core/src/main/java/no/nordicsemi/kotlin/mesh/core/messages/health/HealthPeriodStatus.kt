@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.health

import no.nordicsemi.kotlin.mesh.core.messages.HealthMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage

/**
 * A Health Period Status is an unacknowledged message used to report the
 * Health Fast Period Divisor state of an element.
 *
 * @property fastPeriodDivisor The Health Fast Period Divisor state.
 */
class HealthPeriodStatus(val fastPeriodDivisor: UByte) : ApplicationResponse, UnacknowledgedApplicationMessage {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray = byteArrayOf(fastPeriodDivisor.toByte())

    init {
        require(fastPeriodDivisor <= 15u) { "Fast Period Divisor must be between 0 and 15" }
    }

    override fun toString() = "HealthPeriodStatus(fastPeriodDivisor: $fastPeriodDivisor)"

    companion object Initializer : HealthMessageInitializer {
        override val opCode = 0x8037u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { params ->
                HealthPeriodStatus(params[0].toUByte())
            }
    }
}
