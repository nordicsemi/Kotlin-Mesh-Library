package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

class GenericDefaultTransitionTimeServer : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf()
    override val isSubscriptionSupported: Boolean = true
    override val publicationMessageComposer: MessageComposer? = null
    var defaultTransitionTime: TransitionTime = TransitionTime(rawValue = 0u)
        set(value) {
            field = if (value.isKnown) value else TransitionTime(rawValue = 0u)
        }

    override suspend fun handle(event: ModelEvent): MeshResponse? {
        TODO("Not yet implemented")
    }
}