@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.health

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.HealthMessageInitializer
import java.nio.ByteOrder

/**
 * A Health Fault Clear is an acknowledged message used to clear the current Registered Fault
 * state identified by Company ID of an element.
 *
 * @property companyIdentifier 16-bit Bluetooth assigned Company Identifier.
 */
class HealthFaultClear(val companyIdentifier: UShort) : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = HealthFaultStatus.opCode
    override val parameters: ByteArray = companyIdentifier.toByteArray(ByteOrder.LITTLE_ENDIAN)

    override fun toString() = "HealthFaultClear(companyIdentifier: ${
        companyIdentifier.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )
    })"

    companion object Initializer : HealthMessageInitializer {
        override val opCode = 0x802Fu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let { params ->
                HealthFaultClear(params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN))
            }
    }
}
