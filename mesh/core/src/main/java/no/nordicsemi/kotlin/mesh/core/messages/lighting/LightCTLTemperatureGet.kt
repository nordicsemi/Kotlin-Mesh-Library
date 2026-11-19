package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Requests the current Light CTL Temperature and Delta UV state of a Light CTL Temperature Server.
 *
 * The response to this message is a [LightCTLTemperatureStatus].
 */
class LightCTLTemperatureGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightCTLTemperatureStatus.opCode
    override val parameters = null

    override fun toString() = "LightCTLTemperatureGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8261u

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            LightCTLTemperatureGet()
        else null
    }
}
