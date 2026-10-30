package no.nordicsemi.kotlin.mesh.core.messages.foundation.scene

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.SceneMessageInitializer
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber

/**
 * A Scene Delete message is an acknowledged message used to delete a Scene from the Scene Register
 * of a Scene Server. The response to this message is a [SceneRegisterStatus].
 *
 * @property sceneNumber Scene Number of the scene to be deleted. Must not be 0.
 */
class SceneDelete(val sceneNumber: SceneNumber) : AcknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SceneRegisterStatus.opCode
    override val parameters = sceneNumber.toBytes()

    init {
        require(sceneNumber != 0.toUShort()) { "Scene number must not be 0!" }
    }

    override fun toString() = "SceneDelete(sceneNumber: $sceneNumber)"

    companion object Initializer : SceneMessageInitializer {
        override val opCode = 0x829Eu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let { it.sceneNumberAt(0) }
            ?.takeIf { it != 0.toUShort() }
            ?.let { SceneDelete(sceneNumber = it) }
    }
}
