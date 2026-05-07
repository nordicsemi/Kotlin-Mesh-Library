package no.nordicsemi.android.nrfmesh.core.data.meshnetwork.simpleonoff

import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.meshnetwork.simpleonoff.messages.SimpleOnOffGet
import no.nordicsemi.android.nrfmesh.core.data.meshnetwork.simpleonoff.messages.SimpleOnOffSetUnacknowledged
import no.nordicsemi.android.nrfmesh.core.data.meshnetwork.simpleonoff.messages.SimpleOnOffStatus
import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.LogLevel

class SimpleOnOffClientHandler(private val repository: CoreDataRepository) : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        SimpleOnOffStatus.opCode to SimpleOnOffStatus.Initializer,
    )
    override val isSubscriptionSupported = true
    override val publicationMessageComposer: MessageComposer
        get() = { SimpleOnOffSetUnacknowledged(isOn = state) }
    var state: Boolean = false
        set(value) {
            field = value
            // Publishes the state change to the model.
            publish()
        }

    override suspend fun handle(event: ModelEvent) = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> throw ModelError.InvalidMessage(
            msg = event.request
        )

        is ModelEvent.ResponseReceived -> {
            val unchangedState = event.request is SimpleOnOffGet
            when (event.response) {
                // The status message may be received here if the Simple OnOff Server model
                // has been configured to publish. Ignore this message.
                is SimpleOnOffStatus -> {
                    val element = event.model.parentElement ?: return null
                    val network = element.parentNode?.network ?: return null
                    val node = network.node(address = event.source)
                    val nodeName = node?.name ?: "Unknown Device"
                    val elementName = element.name ?: "Element 0x${element.index + 1}"
                    val string = if(unchangedState) "is" else "changed to"
                    repository.logger.log(
                        message = { "Status of Simple OnOff on $elementName in $nodeName $string : " +
                                "${(event.response as SimpleOnOffStatus).isOn}" },
                        category = LogCategory.MODEL,
                        level = LogLevel.APPLICATION
                    )
                    (event.response as SimpleOnOffStatus)
                }
                // Other message types will not be delivered here, as the `messageTypes` map
                // declares only the above one.
                else -> null
            }
        }

        is ModelEvent.UnacknowledgedMessageReceived -> when (event.message) {
            // The status message may be received here if the Simple OnOff Server model
            // has been configured to publish. Ignore this message.
            is SimpleOnOffStatus -> {
                val element = event.model.parentElement ?: return null
                val network = element.parentNode?.network ?: return null
                val node = network.node(address = event.source)
                val nodeName = node?.name ?: "Unknown Device"
                val elementName = element.name ?: "Element 0x${element.index + 1}"
                repository.logger.log(
                    message = { "Status of Simple OnOff on $elementName in $nodeName, isOn: " +
                            "${(event.message as SimpleOnOffStatus).isOn}" },
                    category = LogCategory.MODEL,
                    level = LogLevel.APPLICATION
                )
                (event.message as SimpleOnOffStatus)
            }
            // Other message types will not be delivered here, as the `messageTypes` map declares
            // only the above one.
            else -> null
        }
    }
}

