package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * Retrieves the Generic OnPowerUp state of a server.
 *
 * The response to this message is a [GenericOnPowerUpStatus].
 */
class GenericOnPowerUpGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericOnPowerUpStatus.opCode
    override val parameters = null

    override fun toString() = "GenericOnPowerUpGet"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8211u

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            GenericOnPowerUpGet()
        else null
    }
}
