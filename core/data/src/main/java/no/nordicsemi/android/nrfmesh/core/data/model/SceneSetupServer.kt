package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneDelete
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneDeleteUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRegisterStatus
import no.nordicsemi.kotlin.mesh.core.messages.SceneMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStore
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStoreUnacknowledged

/**
 * Scene Setup Server model of the local node.
 *
 * The Scene Setup Server extends the [SceneServer] and handles storing and deleting scenes in the
 * Scene Register.
 *
 * @param server Scene Server model, which this model extends.
 */
class SceneSetupServer(private val server: SceneServer) : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        SceneStore.opCode to SceneStore,
        SceneStoreUnacknowledged.opCode to SceneStoreUnacknowledged,
        SceneDelete.opCode to SceneDelete,
        SceneDeleteUnacknowledged.opCode to SceneDeleteUnacknowledged,
    )
    override val isSubscriptionSupported = true
    override val publicationMessageComposer: MessageComposer? = null

    override suspend fun handle(event: ModelEvent): MeshResponse? = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> handleRequest(event = event)

        is ModelEvent.ResponseReceived -> throw ModelError.InvalidMessage(msg = event.request)

        is ModelEvent.UnacknowledgedMessageReceived -> {
            handleRequest(event = event)
            null
        }
    }

    private suspend fun handleRequest(
        event: ModelEvent.AcknowledgedMessageReceived,
    ): SceneRegisterStatus = when (val request = event.request) {
        is SceneStore -> when (server.store(scene = request.sceneNumber)) {
            true -> server.registerStatus()
            // Scene Register may contain up to 16 stored Scenes.
            false -> server.registerStatus(status = SceneMessageStatus.SCENE_REGISTER_FULL)
        }

        is SceneDelete -> when (server.delete(scene = request.sceneNumber)) {
            true -> server.registerStatus()
            false -> server.registerStatus(status = SceneMessageStatus.SCENE_NOT_FOUND)
        }

        else -> throw ModelError.InvalidMessage(msg = request)
    }

    private suspend fun handleRequest(event: ModelEvent.UnacknowledgedMessageReceived) {
        when (val message = event.message) {
            is SceneStoreUnacknowledged -> server.store(scene = message.sceneNumber)

            is SceneDeleteUnacknowledged -> server.delete(scene = message.sceneNumber)

            // The status message may be received here if the model has been configured to
            // publish. Ignore it.
            is SceneRegisterStatus -> Unit

            else -> throw ModelError.InvalidMessage(msg = message)
        }
    }
}
