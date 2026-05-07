@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * This message is used to get the Heartbeat Publication state of an element.
 *
 * @property parameters Message parameters.
 * @constructor Creates a ConfigHeartbeatPublicationGet message.
 */
class ConfigHeartbeatPublicationGet : AcknowledgedConfigMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = ConfigHeartbeatPublicationStatus.opCode
    override val parameters: ByteArray? = null

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "ConfigHeartbeatPublicationGet"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode: UInt = 0x8038u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { ConfigHeartbeatPublicationGet() }
    }
}