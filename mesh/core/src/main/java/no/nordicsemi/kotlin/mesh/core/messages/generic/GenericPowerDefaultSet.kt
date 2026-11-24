package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * Sets the Generic Power Default state of a Generic Power Setup Server.
 *
 * The response to this message is a [GenericPowerDefaultStatus].
 *
 * @property power Value of the Generic Power Default state.
 */
class GenericPowerDefaultSet(val power: UShort) : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericPowerDefaultStatus.opCode
    override val parameters: ByteArray = power.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    override fun toString() = "GenericPowerDefaultSet(power: $power)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x821Fu

        override fun init(parameters: ByteArray?) = parameters?.takeIf { it.size == 2 }?.let {
            GenericPowerDefaultSet(
                power = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
            )
        }
    }
}
