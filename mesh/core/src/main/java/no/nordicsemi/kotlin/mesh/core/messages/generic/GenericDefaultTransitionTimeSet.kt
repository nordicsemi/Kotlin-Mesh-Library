package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * Sets the Generic Default Transition Time state of a server.
 *
 * The response to this message is a [GenericDefaultTransitionTimeStatus].
 *
 * @property transitionTime New value of the Generic Default Transition Time state.
 */
class GenericDefaultTransitionTimeSet(
    val transitionTime: TransitionTime,
) : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericDefaultTransitionTimeStatus.opCode
    override val parameters: ByteArray = transitionTime.rawValue.toByteArray()

    override fun toString() = "GenericDefaultTransitionTimeSet(transitionTime: $transitionTime)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x820Eu

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 1
        }?.let { params ->
            GenericDefaultTransitionTimeSet(
                transitionTime = TransitionTime(rawValue = params[0].toUByte())
            )
        }
    }
}
