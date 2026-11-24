package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * Requests the current Generic Power Default state of a Generic Power Server.
 *
 * The response to this message is a [GenericPowerDefaultStatus].
 */
class GenericPowerDefaultGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericPowerDefaultStatus.opCode
    override val parameters = null

    override fun toString() = "GenericPowerDefaultGet"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x821Bu

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            GenericPowerDefaultGet()
        else null
    }
}
