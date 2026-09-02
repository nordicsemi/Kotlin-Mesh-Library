package no.nordicsemi.android.nrfmesh.core.data.modeleventhandlers

import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.sensor.SensorCadenceStatus
import no.nordicsemi.kotlin.mesh.core.messages.sensor.SensorColumnStatus
import no.nordicsemi.kotlin.mesh.core.messages.sensor.SensorDescriptorStatus
import no.nordicsemi.kotlin.mesh.core.messages.sensor.SensorSeriesStatus
import no.nordicsemi.kotlin.mesh.core.messages.sensor.SensorSettingStatus
import no.nordicsemi.kotlin.mesh.core.messages.sensor.SensorSettingsStatus
import no.nordicsemi.kotlin.mesh.core.messages.sensor.SensorStatus

/**
 * Sensor client event handler.
 */
class SensorClientEventHandler : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        SensorDescriptorStatus.opCode to SensorDescriptorStatus.Initializer,
        SensorCadenceStatus.opCode to SensorCadenceStatus.Initializer,
        SensorSettingsStatus.opCode to SensorSettingsStatus.Initializer,
        SensorSettingStatus.opCode to SensorSettingStatus.Initializer,
        SensorStatus.opCode to SensorStatus.Initializer,
        SensorColumnStatus.opCode to SensorColumnStatus.Initializer,
        SensorSeriesStatus.opCode to SensorSeriesStatus.Initializer,
    )
    override val isSubscriptionSupported = true
    override val publicationMessageComposer = null

    override suspend fun handle(event: ModelEvent) = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> throw ModelError.InvalidMessage(
            msg = event.request
        )

        is ModelEvent.ResponseReceived -> {
            // Ignore do nothing
            null
        }

        is ModelEvent.UnacknowledgedMessageReceived -> {
            // The status message may be received here if the Generic OnOff Server model
            // has been configured to publish. Ignore this message.
            null
        }
    }
}