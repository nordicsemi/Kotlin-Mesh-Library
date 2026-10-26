package no.nordicsemi.kotlin.mesh.core.messages.foundation.scene

import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.SceneMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.StatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber

/**
 * A Scene Register Status message is an unacknowledged message used to report the status of the
 * Scene Register of a Scene Server. It is a response to [SceneRegisterGet], [SceneStore] and
 * [SceneDelete] messages.
 *
 * @property status        Status code of the last operation.
 * @property currentScene  Scene Number of the current scene, 0 if no scene is active.
 * @property scenes        List of scenes stored within the element.
 */
class SceneRegisterStatus(
    val status: SceneStatusCode,
    val currentScene: SceneNumber,
    val scenes: List<SceneNumber>,
) : MeshResponse, UnacknowledgedMeshMessage, StatusMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() = scenes.fold(byteArrayOf(status.value.toByte()) + currentScene.toBytes()) { data, scene ->
            data + scene.toBytes()
        }
    override val isSuccess: Boolean
        get() = status == SceneStatusCode.SUCCESS
    override val message: String
        get() = status.description

    override fun toString() =
        "SceneRegisterStatus(status: $status, currentScene: $currentScene, scenes: $scenes)"

    companion object Initializer : SceneMessageInitializer {
        override val opCode = 0x8245u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size >= 3 && (it.size - 3) % 2 == 0 }
            ?.let { params ->
                SceneStatusCode.from(params[0].toUByte())?.let { status ->
                    SceneRegisterStatus(
                        status = status,
                        currentScene = params.sceneNumberAt(1),
                        scenes = (3 until params.size step 2).map { params.sceneNumberAt(it) }
                    )
                }
            }
    }
}
