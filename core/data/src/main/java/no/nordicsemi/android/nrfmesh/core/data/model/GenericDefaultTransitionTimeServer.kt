package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.android.nrfmesh.core.data.storage.GenericDefaultTransitionTimeStorage
import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeStatus
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import kotlin.uuid.Uuid

/**
 * Handles the Generic Default Transition Time Server model of the local node. The value is used
 * by the other local servers when a message does not specify a transition time.
 *
 * @property storage    Storage used to keep the value across app restarts, or null to keep it in
 *                      memory only.
 * @property uuid       UUID of the network the value belongs to.
 */
class GenericDefaultTransitionTimeServer(
    private val storage: GenericDefaultTransitionTimeStorage? = null,
    private val uuid: Uuid? = null,
) : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        GenericDefaultTransitionTimeGet.opCode to GenericDefaultTransitionTimeGet.Initializer,
        GenericDefaultTransitionTimeSet.opCode to GenericDefaultTransitionTimeSet.Initializer,
        GenericDefaultTransitionTimeSetUnacknowledged.opCode to
                GenericDefaultTransitionTimeSetUnacknowledged.Initializer
    )
    override val isSubscriptionSupported: Boolean = true
    override val publicationMessageComposer: MessageComposer? = null

    var defaultTransitionTime: TransitionTime = TransitionTime(rawValue = 0u)
        set(value) {
            field = if (value.isKnown) value else TransitionTime(rawValue = 0u)
            if (storage != null && uuid != null) {
                storage.storeDefaultTransitionTime(uuid = uuid, rawValue = field.rawValue)
            }
        }

    /**
     * Restores the value that was stored with the network, if any.
     */
    suspend fun restore() {
        val rawValue = uuid?.let { storage?.readDefaultTransitionTime(uuid = it) } ?: return
        TransitionTime(rawValue = rawValue).takeIf { it.isKnown }?.let { defaultTransitionTime = it }
    }

    override suspend fun handle(event: ModelEvent): MeshResponse? = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> when (val request = event.request) {
            is GenericDefaultTransitionTimeSet -> {
                // The state cannot be set to the unknown value.
                if (!request.transitionTime.isKnown) throw ModelError.InvalidMessage(msg = request)
                defaultTransitionTime = request.transitionTime
                GenericDefaultTransitionTimeStatus(transitionTime = defaultTransitionTime)
            }

            is GenericDefaultTransitionTimeGet ->
                GenericDefaultTransitionTimeStatus(transitionTime = defaultTransitionTime)

            else -> throw ModelError.InvalidMessage(msg = request)
        }

        is ModelEvent.ResponseReceived -> throw ModelError.InvalidMessage(msg = event.request)

        is ModelEvent.UnacknowledgedMessageReceived -> {
            (event.message as? GenericDefaultTransitionTimeSetUnacknowledged)
                ?.transitionTime
                ?.takeIf { it.isKnown }
                ?.let { defaultTransitionTime = it }
            null
        }
    }
}
