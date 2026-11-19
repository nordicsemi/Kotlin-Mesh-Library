package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * Reports the Generic Default Transition Time state of a server.
 *
 * This message is sent in response to a [GenericDefaultTransitionTimeGet] or
 * [GenericDefaultTransitionTimeSet] message.
 *
 * @property transitionTime Value of the Generic Default Transition Time state.
 */
class GenericDefaultTransitionTimeStatus(val transitionTime: TransitionTime) : ApplicationResponse {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = transitionTime.rawValue.toByteArray()

    override fun toString() = "GenericDefaultTransitionTimeStatus(transitionTime: $transitionTime)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8210u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 1
        }?.let { params ->
            GenericDefaultTransitionTimeStatus(
                transitionTime = TransitionTime(rawValue = params[0].toUByte())
            )
        }
    }
}
