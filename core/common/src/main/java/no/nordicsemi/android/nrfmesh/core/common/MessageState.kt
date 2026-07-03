package no.nordicsemi.android.nrfmesh.core.common

import no.nordicsemi.kotlin.mesh.core.messages.BaseMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage

/**
 * Defines the message state of a mesh message.
 */
sealed class MessageState(
    open val message: BaseMeshMessage? = null,
    open val response: MeshMessage? = null,
    open val error: Throwable? = null,
) {

    fun isInProgress(): Boolean = this is Sending

    fun didFail(): Boolean = this is Failed

    fun didSucceed(): Boolean = this is Completed
}

/**
 * Defines a state where the message sending has not started yet.
 */
data object NotStarted : MessageState()

/**
 * Defines a state where the message sending has begun.
 *
 * @param message Message that is being sent.
 */
data class Sending(override val message: BaseMeshMessage) : MessageState()

/**
 * Defines a state when a message sending has been completed successfully.
 *
 * @param message   Message that was sent.
 * @param response  Response received from the mesh node.
 */
data class Completed(
    override val message: BaseMeshMessage,
    override val response: MeshMessage? = null,
) : MessageState(message = message, response = response)

/**
 * Define a state when hen a message sending has failed.
 *
 * @param message   Message that was sent.
 * @param error     Error that occurred while sending the message.
 */
data class Failed(
    override val message: BaseMeshMessage?,
    override val error: Throwable,
) : MessageState(message = message, error = error)
