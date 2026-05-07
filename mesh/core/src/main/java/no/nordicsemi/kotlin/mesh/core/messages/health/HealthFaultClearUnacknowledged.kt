@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.health

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.HealthMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import java.nio.ByteOrder

/**
 * A Health Fault Clear Unacknowledged is an unacknowledged message used to clear the
 * current Registered Fault state of an element.
 *
 * @property companyIdentifier 16-bit Bluetooth assigned Company Identifier.
 */
class HealthFaultClearUnacknowledged(
    val companyIdentifier: UShort
) : UnacknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray = companyIdentifier.toByteArray(ByteOrder.LITTLE_ENDIAN)

    override fun toString() = "HealthFaultClearUnacknowledged(companyIdentifier: $companyIdentifier)"

    companion object Initializer : HealthMessageInitializer {
        override val opCode = 0x8030u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let { params ->
                HealthFaultClearUnacknowledged(params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN))
            }
    }
}
