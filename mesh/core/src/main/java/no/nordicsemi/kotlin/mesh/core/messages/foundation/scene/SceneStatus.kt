package no.nordicsemi.kotlin.mesh.core.messages.foundation.scene

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.SceneMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SceneMessageStatus
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
    val status: SceneMessageStatus,
    val currentScene: SceneNumber,
    val targetScene: SceneNumber?,
    override val remainingTime: TransitionTime?,
) : MeshResponse, UnacknowledgedMeshMessage, TransitionStatusMessage, StatusMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() {
            val data = status.value.toByteArray() +
                    currentScene.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
            return if (targetScene != null && remainingTime != null) {
                data + targetScene.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                        remainingTime.rawValue.toByte()
            } else data
        }
    override val isSuccess: Boolean
        get() = status == SceneMessageStatus.SUCCESS
    override val message: String
        get() = status.description

    /**
     * Convenience constructor to create a SceneStatus without a target scene and remaining time.
     *
     * @param status       Status code.
     * @param currentScene Current scene.
     */
    @Suppress("unused")
    constructor(status: SceneMessageStatus, currentScene: SceneNumber) : this(
        status = status,
        currentScene = currentScene,
        targetScene = null,
        remainingTime = null
    )

    override fun toString() = "SceneStatus(status: $status, currentScene: ${
        currentScene.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )
    }" +
            if (targetScene != null && remainingTime != null) {
                ", targetScene: ${
                    targetScene.toHexString(
                        format = HexFormat {
                            number.prefix = "0x"
                            upperCase = true
                        }
                    )
                }, remainingTime: $remainingTime)"
            } else ")"

    companion object Initializer : SceneMessageInitializer {
        override val opCode = 0x5Eu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 3 || it.size == 6 }
            ?.let { params ->
                SceneMessageStatus.from(params[0].toUByte())?.let { status ->
                    SceneStatus(
                        status = status,
                        currentScene = params.getUShort(
                            offset = 1,
                            order = ByteOrder.LITTLE_ENDIAN
                        ),
                        targetScene = if (params.size == 6) params.getUShort(
                            offset = 3,
                            order = ByteOrder.LITTLE_ENDIAN
                        ) else null,
                        remainingTime = if (params.size == 6)
                            TransitionTime(rawValue = params[5].toUByte())
                        else null
                    )
                }
            }
    }
}
