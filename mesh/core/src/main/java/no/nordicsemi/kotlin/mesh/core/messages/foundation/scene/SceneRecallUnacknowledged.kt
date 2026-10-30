package no.nordicsemi.kotlin.mesh.core.messages.foundation.scene

import no.nordicsemi.kotlin.mesh.core.messages.SceneMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransactionMessage
import no.nordicsemi.kotlin.mesh.core.messages.TransitionMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters

/**
 * A Scene Recall Unacknowledged message is an unacknowledged message used to recall the current
 * state of an element from a previously stored Scene.
 *
 * @property tid              Defines a unique Transaction identifier that each message must have.
 * @property sceneNumber      Scene Number of the scene to be recalled. Must not be 0.
 * @property transitionTime   Defines the time interval an element will take to transition to the
 *                            target state from the present state.
 * @property delay            Message execution delay in 5 millisecond steps.
 */
class SceneRecallUnacknowledged(
    override var tid: UByte?,
    val sceneNumber: SceneNumber,
    transitionParams: TransitionParameters? = null,
) : UnacknowledgedMeshMessage, TransactionMessage, TransitionMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() {
            val data = sceneNumber.toBytes() + tid!!.toByte()
            return if (transitionTime != null && delay != null) {
                data + byteArrayOf(transitionTime.rawValue.toByte(), delay.toByte())
            } else data
        }
    override val transitionTime = transitionParams?.transitionTime
    override val delay = transitionParams?.delay

    init {
        require(sceneNumber != 0.toUShort()) { "Scene number must not be 0!" }
    }

    /**
     * Convenience constructor to create a SceneRecallUnacknowledged message without any transition
     * time, tid or delay.
     *
     * @param sceneNumber Scene Number of the scene to be recalled.
     */
    @Suppress("unused")
    constructor(sceneNumber: SceneNumber) : this(tid = null, sceneNumber = sceneNumber)

    override fun toString() = "SceneRecallUnacknowledged(tid: $tid, sceneNumber: $sceneNumber" +
            if (transitionTime != null && delay != null) {
                ", transitionTime: $transitionTime, delay: ${delay.toInt() * 5} ms)"
            } else ")"

    companion object Initializer : SceneMessageInitializer {
        override val opCode = 0x8243u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 3 || it.size == 5 }
            ?.let { params ->
                params.sceneNumberAt(0).takeIf { it != 0.toUShort() }?.let { scene ->
                    SceneRecallUnacknowledged(
                        tid = params[2].toUByte(),
                        sceneNumber = scene,
                        transitionParams = if (params.size == 5) TransitionParameters(
                            transitionTime = TransitionTime(rawValue = params[3].toUByte()),
                            delay = params[4].toUByte()
                        ) else null
                    )
                }
            }
    }
}
