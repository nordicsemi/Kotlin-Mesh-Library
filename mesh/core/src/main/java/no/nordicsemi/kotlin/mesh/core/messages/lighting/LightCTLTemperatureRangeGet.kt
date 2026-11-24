package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Requests the Light CTL Temperature Range state of a Light CTL Server.
 *
 * The response to this message is a [LightCTLTemperatureRangeStatus].
 */
class LightCTLTemperatureRangeGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightCTLTemperatureRangeStatus.opCode
    override val parameters = null

    override fun toString() = "LightCTLTemperatureRangeGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8262u

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            LightCTLTemperatureRangeGet()
        else null
    }
}
