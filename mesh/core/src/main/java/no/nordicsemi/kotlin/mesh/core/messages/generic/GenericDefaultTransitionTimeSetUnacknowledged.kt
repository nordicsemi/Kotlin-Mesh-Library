package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * Sets the Generic Default Transition Time state of a server.
 *
 * This message does not expect a response.
 *
 * @property transitionTime New value of the Generic Default Transition Time state.
 */
class GenericDefaultTransitionTimeSetUnacknowledged(
    val transitionTime: TransitionTime,
) : UnacknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = transitionTime.rawValue.toByteArray()

    override fun toString() =
        "GenericDefaultTransitionTimeSetUnacknowledged(transitionTime: $transitionTime)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x820Fu

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 1
        }?.let { params ->
            GenericDefaultTransitionTimeSetUnacknowledged(
                transitionTime = TransitionTime(rawValue = params[0].toUByte())
            )
        }
    }
}
