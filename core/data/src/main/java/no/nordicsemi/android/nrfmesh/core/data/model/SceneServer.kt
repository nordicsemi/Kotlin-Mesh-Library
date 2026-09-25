package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.SceneServerModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse

class SceneServer : SceneServerModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf()
    override val isSubscriptionSupported: Boolean = false
    override val publicationMessageComposer: MessageComposer? = null

    override suspend fun handle(event: ModelEvent): MeshResponse? {
        TODO("Not yet implemented")
    }

    override fun networkDidExitStoredWithSceneState() {
        TODO("Not yet implemented")
    }
}