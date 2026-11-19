package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * Requests the current Generic Power Range state of a Generic Power Server.
 *
 * The response to this message is a [GenericPowerRangeStatus].
 */
class GenericPowerRangeGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericPowerRangeStatus.opCode
    override val parameters = null

    override fun toString() = "GenericPowerRangeGet"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x821Du

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            GenericPowerRangeGet()
        else null
    }
}
