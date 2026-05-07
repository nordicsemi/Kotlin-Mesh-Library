package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * This message is used to request the current status of a GenericLevelServer model
 */
class GenericLevelGet : AcknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericLevelStatus.opCode
    override val parameters = null

    override fun toString() = "GenericLevelGet"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8205u

        override fun init(parameters: ByteArray?) = if (parameters == null)
            GenericLevelGet()
        else null
    }
}