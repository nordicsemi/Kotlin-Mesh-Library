package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * Requests the current Generic Power Last state of a Generic Power Server.
 *
 * The response to this message is a [GenericPowerLastStatus].
 */
class GenericPowerLastGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericPowerLastStatus.opCode
    override val parameters = null

    override fun toString() = "GenericPowerLastGet"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8219u

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            GenericPowerLastGet()
        else null
    }
}
