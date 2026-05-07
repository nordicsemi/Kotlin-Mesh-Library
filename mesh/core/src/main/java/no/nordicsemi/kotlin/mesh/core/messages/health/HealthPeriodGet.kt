@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.health

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.HealthMessageInitializer

/**
 * A Health Period Get is an acknowledged message used to get the current Health Fast Period Divisor
 * state of an Element.
 */
class HealthPeriodGet : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = HealthPeriodStatus.opCode
    override val parameters: ByteArray? = null

    override fun toString() = "HealthPeriodGet"

    companion object Initializer : HealthMessageInitializer {
        override val opCode = 0x8034u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { HealthPeriodGet() }
    }
}
