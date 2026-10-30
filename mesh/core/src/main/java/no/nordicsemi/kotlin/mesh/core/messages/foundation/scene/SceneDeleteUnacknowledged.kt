package no.nordicsemi.kotlin.mesh.core.messages.foundation.scene

import no.nordicsemi.kotlin.mesh.core.messages.SceneMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber

/**
 * A Scene Delete Unacknowledged message is an unacknowledged message used to delete a Scene from
 * the Scene Register of a Scene Server.
 *
 * @property sceneNumber Scene Number of the scene to be deleted. Must not be 0.
 */
class SceneDeleteUnacknowledged(val sceneNumber: SceneNumber) : UnacknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val parameters = sceneNumber.toBytes()

    init {
        require(sceneNumber != 0.toUShort()) { "Scene number must not be 0!" }
    }

    override fun toString() = "SceneDeleteUnacknowledged(sceneNumber: $sceneNumber)"

    companion object Initializer : SceneMessageInitializer {
        override val opCode = 0x829Fu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let { it.sceneNumberAt(0) }
            ?.takeIf { it != 0.toUShort() }
            ?.let { SceneDeleteUnacknowledged(sceneNumber = it) }
    }
}
