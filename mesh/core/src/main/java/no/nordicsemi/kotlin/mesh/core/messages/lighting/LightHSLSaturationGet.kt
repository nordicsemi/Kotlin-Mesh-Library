package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Requests the current Light HSL Saturation state of a Light HSL Server.
 *
 * The response to this message is a [LightHSLSaturationStatus].
 */
class LightHSLSaturationGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightHSLSaturationStatus.opCode
    override val parameters = null

    override fun toString() = "LightHSLSaturationGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8272u

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            LightHSLSaturationGet()
        else null
    }
}
