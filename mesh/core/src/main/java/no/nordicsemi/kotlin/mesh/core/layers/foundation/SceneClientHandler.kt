package no.nordicsemi.kotlin.mesh.core.layers.foundation

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRegisterStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStatus
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.Scene
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress

/**
 * Scene Client model of the local node.
 *
 * The handler keeps the scenes of the mesh network up to date with the Scene Register of the
 * nodes. Whenever a Scene Status or a Scene Register Status is received, the address of the node
 * is added to the scenes that the node confirmed to have stored, see [Scene.addresses].
 */
class SceneClientHandler : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        // A Scene Server shall send the Scene Status message as a response to a Scene Get and
        // Scene Recall message, or as an unsolicited message.
        SceneStatus.opCode to SceneStatus,
        // A Scene Server shall send the Scene Register Status message either as a response to a
        // Scene Store message or a Scene Register Get message, or as an unsolicited message.
        SceneRegisterStatus.opCode to SceneRegisterStatus,
    )
    override val isSubscriptionSupported: Boolean = true
    // TODO: Implement Scene Client publications.
    override val publicationMessageComposer: MessageComposer? = null

    override suspend fun handle(event: ModelEvent): MeshResponse? {
        when (event) {
            // No acknowledged messages are supported by this model.
            is ModelEvent.AcknowledgedMessageReceived -> throw ModelError.InvalidMessage(
                msg = event.request
            )

            is ModelEvent.ResponseReceived -> handle(
                message = event.response,
                source = event.source
            )

            is ModelEvent.UnacknowledgedMessageReceived -> handle(
                message = event.message,
                source = event.source
            )
        }
        return null
    }

    private fun handle(message: MeshMessage, source: Address) {
        val address = UnicastAddress(address = source)
        when (message) {
            // Response to Scene Get and Scene Recall.
            is SceneStatus -> {
                // Ensure the current and the target scenes are updated with the node address.
                listOfNotNull(message.currentScene, message.targetScene)
                    .filter { Scene.isValid(sceneNumber = it) }
                    .forEach { sceneOf(number = it).add(address = address) }
            }

            // Response to Scene Register Get, Scene Store and Scene Delete.
            is SceneRegisterStatus -> {
                // Scenes confirmed to be in the Scene Register on the node.
                val confirmed = message.scenes.filter { Scene.isValid(sceneNumber = it) }
                confirmed.forEach { sceneOf(number = it).add(address = address) }
                // Remove the node from the scenes that it is confirmed not to have stored.
                meshNetwork.scenes
                    .filter { it.number !in confirmed }
                    .forEach { it.remove(address = address) }
            }
        }
    }

    /**
     * Returns the scene with the given number, or adds a new one to the network if it does not
     * exist.
     */
    private fun sceneOf(number: SceneNumber): Scene = meshNetwork.scene(number = number)
        ?: meshNetwork.add(name = "New Scene", number = number)
}
