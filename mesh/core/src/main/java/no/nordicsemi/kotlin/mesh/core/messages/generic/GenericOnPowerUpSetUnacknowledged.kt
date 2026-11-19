package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage

/**
 * Sets the Generic OnPowerUp state of a server.
 *
 * This message does not expect a response.
 *
 * @property state New value of the Generic OnPowerUp state.
 */
class GenericOnPowerUpSetUnacknowledged(val state: OnPowerUp) : UnacknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = state.value.toByteArray()

    override fun toString() = "GenericOnPowerUpSetUnacknowledged(state: $state)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8214u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 1
        }?.let { params ->
            OnPowerUp.from(value = params[0].toUByte())?.let { state ->
                GenericOnPowerUpSetUnacknowledged(state = state)
            }
        }
    }
}
