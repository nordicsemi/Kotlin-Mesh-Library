package no.nordicsemi.kotlin.mesh.core.messages.foundation.scene

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.SceneMessageInitializer

/**
 * A Scene Register Get message is an acknowledged message used to get the current status of the
 * Scene Register of a Scene Server. The response to this message is a [SceneRegisterStatus].
 */
class SceneRegisterGet : AcknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SceneRegisterStatus.opCode
    override val parameters = null

    override fun toString() = "SceneRegisterGet"

    companion object Initializer : SceneMessageInitializer {
        override val opCode = 0x8244u

        override fun init(parameters: ByteArray?) =
            if (parameters == null) SceneRegisterGet() else null
    }
}
