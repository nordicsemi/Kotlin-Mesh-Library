package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * Reports the Generic OnPowerUp state of a server.
 *
 * This message is sent in response to a [GenericOnPowerUpGet] or [GenericOnPowerUpSet] message.
 *
 * @property state Present value of the Generic OnPowerUp state.
 */
class GenericOnPowerUpStatus(val state: OnPowerUp) : ApplicationResponse {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = state.value.toByteArray()

    override fun toString() = "GenericOnPowerUpStatus(state: $state)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8212u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 1
        }?.let { params ->
            OnPowerUp.from(value = params[0].toUByte())?.let { state ->
                GenericOnPowerUpStatus(state = state)
            }
        }
    }
}
