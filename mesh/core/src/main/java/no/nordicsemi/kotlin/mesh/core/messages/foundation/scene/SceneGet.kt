package no.nordicsemi.kotlin.mesh.core.messages.foundation.scene

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.SceneMessageInitializer

/**
 * A Scene Get message is an acknowledged message used to get the current status of a Scene Server.
 * The response to this message is a [SceneStatus].
 */
class SceneGet : AcknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SceneStatus.opCode
    override val parameters = null

    override fun toString() = "SceneGet"

    companion object Initializer : SceneMessageInitializer {
        override val opCode = 0x8241u

        override fun init(parameters: ByteArray?) = if (parameters == null) SceneGet() else null
    }
}
