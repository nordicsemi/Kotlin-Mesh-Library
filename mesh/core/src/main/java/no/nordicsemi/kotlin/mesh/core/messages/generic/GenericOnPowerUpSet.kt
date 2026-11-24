package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * Sets the Generic OnPowerUp state of a server.
 *
 * The response to this message is a [GenericOnPowerUpStatus].
 *
 * @property state New value of the Generic OnPowerUp state.
 */
class GenericOnPowerUpSet(val state: OnPowerUp) : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericOnPowerUpStatus.opCode
    override val parameters: ByteArray = state.value.toByteArray()

    override fun toString() = "GenericOnPowerUpSet(state: $state)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8213u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 1
        }?.let { params ->
            OnPowerUp.from(value = params[0].toUByte())?.let { state ->
                GenericOnPowerUpSet(state = state)
            }
        }
    }
}
