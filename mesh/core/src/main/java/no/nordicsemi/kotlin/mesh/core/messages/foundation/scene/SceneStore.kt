package no.nordicsemi.kotlin.mesh.core.messages.foundation.scene

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.SceneMessageInitializer
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber

/**
 * A Scene Store message is an acknowledged message used to store the current state of an element as
 * a Scene in the Scene Register of a Scene Server. The response to this message is a
 * [SceneRegisterStatus].
 *
 * @property sceneNumber Scene Number of the scene to be stored. Must not be 0.
 */
class SceneStore(val sceneNumber: SceneNumber) : AcknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SceneRegisterStatus.opCode
    override val parameters = sceneNumber.toBytes()

    init {
        require(sceneNumber != 0.toUShort()) { "Scene number must not be 0!" }
    }

    override fun toString() = "SceneStore(sceneNumber: $sceneNumber)"

    companion object Initializer : SceneMessageInitializer {
        override val opCode = 0x8246u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let { it.sceneNumberAt(0) }
            ?.takeIf { it != 0.toUShort() }
            ?.let { SceneStore(sceneNumber = it) }
    }
}
