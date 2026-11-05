package no.nordicsemi.kotlin.mesh.core.messages.foundation.scene

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.SceneMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber

/**
 * A Scene Store Unacknowledged message is an unacknowledged message used to store the current state
 * of an element as a Scene in the Scene Register of a Scene Server.
 *
 * @property sceneNumber Scene Number of the scene to be stored. Must not be 0.
 */
class SceneStoreUnacknowledged(val sceneNumber: SceneNumber) : UnacknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val parameters = sceneNumber.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    init {
        require(sceneNumber != 0.toUShort()) { "Scene number must not be 0!" }
    }

    override fun toString() = "SceneStoreUnacknowledged(sceneNumber: ${
        sceneNumber.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )
    })"

    companion object Initializer : SceneMessageInitializer {
        override val opCode = 0x8247u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
            ?.takeIf { it != 0.toUShort() }
            ?.let { SceneStoreUnacknowledged(sceneNumber = it) }
    }
}
