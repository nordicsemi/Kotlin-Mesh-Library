package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage

/**
 * Sets the Generic Power Default state of a Generic Power Setup Server without requesting a
 * response.
 *
 * This is an unacknowledged message, so no response is expected.
 *
 * @property power Value of the Generic Power Default state.
 */
class GenericPowerDefaultSetUnacknowledged(val power: UShort) : UnacknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = power.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    override fun toString() = "GenericPowerDefaultSetUnacknowledged(power: $power)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8220u

        override fun init(parameters: ByteArray?) = parameters?.takeIf { it.size == 2 }?.let {
            GenericPowerDefaultSetUnacknowledged(
                power = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
            )
        }
    }
}
