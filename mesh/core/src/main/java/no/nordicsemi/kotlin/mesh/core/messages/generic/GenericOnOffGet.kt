package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * This message is used to request the current status of a GenericOnOffServer model
 */
class GenericOnOffGet : AcknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericOnOffStatus.opCode
    override val parameters = null

    override fun toString() = "GenericOnOffGet"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8201u

        override fun init(parameters: ByteArray?) = if (parameters == null)
            GenericOnOffGet()
        else null
    }
}