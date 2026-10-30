package no.nordicsemi.kotlin.mesh.core.messages.foundation.scene

import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.SceneMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.StatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.TransitionStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * A Scene Status message is an unacknowledged message used to report the current status of a Scene
 * Server. It is a response to [SceneGet], [SceneRecall] messages.
 *
 * @property status         Status code of the last operation.
 * @property currentScene   Scene Number of the current scene, 0 if no scene is active.
 * @property targetScene    Scene Number of the target scene, present only during a transition.
 * @property remainingTime  Time that an element will take to transition to the target scene.
 */
class SceneStatus(
    val status: SceneStatusCode,
    val currentScene: SceneNumber,
    val targetScene: SceneNumber?,
    override val remainingTime: TransitionTime?,
) : MeshResponse, UnacknowledgedMeshMessage, TransitionStatusMessage, StatusMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() {
            val data = byteArrayOf(status.value.toByte()) + currentScene.toBytes()
            return if (targetScene != null && remainingTime != null) {
                data + targetScene.toBytes() + remainingTime.rawValue.toByte()
            } else data
        }
    override val isSuccess: Boolean
        get() = status == SceneStatusCode.SUCCESS
    override val message: String
        get() = status.description

    /**
     * Convenience constructor to create a SceneStatus without a target scene and remaining time.
     *
     * @param status       Status code.
     * @param currentScene Current scene.
     */
    @Suppress("unused")
    constructor(status: SceneStatusCode, currentScene: SceneNumber) : this(
        status = status,
        currentScene = currentScene,
        targetScene = null,
        remainingTime = null
    )

    override fun toString() = "SceneStatus(status: $status, currentScene: $currentScene" +
            if (targetScene != null && remainingTime != null) {
                ", targetScene: $targetScene, remainingTime: $remainingTime)"
            } else ")"

    companion object Initializer : SceneMessageInitializer {
        override val opCode = 0x5Eu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 3 || it.size == 6 }
            ?.let { params ->
                SceneStatusCode.from(params[0].toUByte())?.let { status ->
                    SceneStatus(
                        status = status,
                        currentScene = params.sceneNumberAt(1),
                        targetScene = if (params.size == 6) params.sceneNumberAt(3) else null,
                        remainingTime = if (params.size == 6)
                            TransitionTime(rawValue = params[5].toUByte())
                        else null
                    )
                }
            }
    }
}

/**
 * Encodes a scene number as 2 bytes in little endian order.
 */
internal fun SceneNumber.toBytes() = byteArrayOf(
    (toInt() and 0xFF).toByte(),
    (toInt() shr 8).toByte()
)

/**
 * Decodes a little endian scene number starting at the given offset.
 */
internal fun ByteArray.sceneNumberAt(offset: Int): SceneNumber =
    ((this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)).toUShort()
