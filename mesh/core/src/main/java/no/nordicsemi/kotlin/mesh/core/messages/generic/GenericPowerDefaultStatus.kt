package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * Reports the current Generic Power Default state of a Generic Power Server.
 *
 * This message is sent in response to a [GenericPowerDefaultGet], [GenericPowerDefaultSet] or
 * [GenericPowerDefaultSetUnacknowledged] message.
 *
 * @property power Value of the Generic Power Default state.
 */
class GenericPowerDefaultStatus(val power: UShort) : ApplicationResponse {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = power.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    override fun toString() = "GenericPowerDefaultStatus(power: $power)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x821Cu

        override fun init(parameters: ByteArray?) = parameters?.takeIf { it.size == 2 }?.let {
            GenericPowerDefaultStatus(
                power = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
            )
        }
    }
}
