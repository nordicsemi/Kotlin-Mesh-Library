package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Requests the current Light HSL Hue state of a Light HSL Server.
 *
 * The response to this message is a [LightHSLHueStatus].
 */
class LightHSLHueGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightHSLHueStatus.opCode
    override val parameters = null

    override fun toString() = "LightHSLHueGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x826Eu

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            LightHSLHueGet()
        else null
    }
}
