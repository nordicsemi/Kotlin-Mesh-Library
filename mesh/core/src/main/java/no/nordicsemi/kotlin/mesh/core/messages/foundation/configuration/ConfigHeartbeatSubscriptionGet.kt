@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * This message is used to get the Heartbeat Subscription state of an element. The response received
 * to this message will be a [ConfigHeartbeatSubscriptionStatus] message.
 *
 * @constructor Creates a ConfigHeartbeatPublicationGet message.
 */
class ConfigHeartbeatSubscriptionGet : AcknowledgedConfigMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = ConfigHeartbeatSubscriptionStatus.opCode
    override val parameters: ByteArray? = null

    override fun toString() = "ConfigHeartbeatSubscriptionGet opCode ${opCode.toHexString()}"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode: UInt = 0x803Au

        override fun init(parameters: ByteArray?) = if(parameters == null || parameters.isEmpty()) {
            ConfigHeartbeatSubscriptionGet()
        } else {
            null
        }
    }
}