package no.nordicsemi.kotlin.mesh.core.layers.foundation

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionGet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionSet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionStatus
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthCurrentStatus
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultClear
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultClearUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultGet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultStatus
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultTest
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultTestUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthPeriodGet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthPeriodSet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthPeriodSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthPeriodStatus
import no.nordicsemi.kotlin.mesh.core.model.Address

class HealthClientHandler : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        HealthCurrentStatus.opCode to HealthCurrentStatus,
        HealthFaultStatus.opCode to HealthFaultStatus,
        HealthPeriodStatus.opCode to HealthPeriodStatus,
        HealthAttentionStatus.opCode to HealthAttentionStatus,
    )
    override val isSubscriptionSupported: Boolean = false
    override val publicationMessageComposer: MessageComposer? = null

    override suspend fun handle(event: ModelEvent): MeshResponse? {
        when (event) {
            is ModelEvent.AcknowledgedMessageReceived -> throw ModelError.InvalidMessage(
                msg = event.request
            )

            is ModelEvent.ResponseReceived -> handleResponse(
                response = event.response,
                request = event.request,
                source = event.source
            )

            is ModelEvent.UnacknowledgedMessageReceived -> {
                // Ignore do nothing
            }
        }
        return null
    }

    private fun handleResponse(
        response: MeshResponse,
        request: AcknowledgedMeshMessage,
        source: Address,
    ) {
        // We do nothing here because there are no CDB matching these parameters
        when (response) {
            is HealthCurrentStatus -> {

            }

            is HealthFaultStatus -> {

            }

            is HealthPeriodStatus -> {

            }

            is HealthAttentionStatus -> {

            }

        }
    }
}