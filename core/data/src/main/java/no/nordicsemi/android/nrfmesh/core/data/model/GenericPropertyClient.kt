package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertyStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericClientPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertyStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertyStatus

/**
 * Handles the Generic Property Client model. It allows the Generic Manufacturer, Admin, User and
 * Client Property status messages to be decoded and delivered as responses.
 */
class GenericPropertyClient : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        GenericManufacturerPropertiesStatus.opCode to GenericManufacturerPropertiesStatus.Initializer,
        GenericManufacturerPropertyStatus.opCode to GenericManufacturerPropertyStatus.Initializer,
        GenericAdminPropertiesStatus.opCode to GenericAdminPropertiesStatus.Initializer,
        GenericAdminPropertyStatus.opCode to GenericAdminPropertyStatus.Initializer,
        GenericUserPropertiesStatus.opCode to GenericUserPropertiesStatus.Initializer,
        GenericUserPropertyStatus.opCode to GenericUserPropertyStatus.Initializer,
        GenericClientPropertiesStatus.opCode to GenericClientPropertiesStatus.Initializer,
    )
    override val isSubscriptionSupported = true
    override val publicationMessageComposer: MessageComposer? = null

    override suspend fun handle(event: ModelEvent) = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> throw ModelError.InvalidMessage(
            msg = event.request
        )

        // Responses are delivered to the caller; status messages published by a server are ignored.
        is ModelEvent.ResponseReceived,
        is ModelEvent.UnacknowledgedMessageReceived,
        -> null
    }
}
