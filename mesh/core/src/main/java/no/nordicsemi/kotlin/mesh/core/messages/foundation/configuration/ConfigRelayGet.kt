package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * Defines message sent to request the current Relay state of the Node. [ConfigRelayStatus]
 * will be the response to this message.
 */
class ConfigRelayGet : AcknowledgedConfigMessage {
    override val opCode = Initializer.opCode
    override val parameters = null
    override val responseOpCode: UInt = ConfigRelayStatus.opCode

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = "ConfigRelayGet"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8026u

        override fun init(parameters: ByteArray?)  = parameters?.takeIf {
            it.isEmpty()
        }?.let {
            ConfigRelayGet()
        }
    }
}