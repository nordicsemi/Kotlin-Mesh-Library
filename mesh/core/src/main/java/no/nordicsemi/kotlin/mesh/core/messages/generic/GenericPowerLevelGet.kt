package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * Requests the current Power state of a Generic Power Server.
 *
 * The response to this message is a [GenericPowerLevelStatus].
 */
class GenericPowerLevelGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericPowerLevelStatus.opCode
    override val parameters = null

    override fun toString() = "GenericPowerLevelGet"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8215u

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            GenericPowerLevelGet()
        else null
    }
}
