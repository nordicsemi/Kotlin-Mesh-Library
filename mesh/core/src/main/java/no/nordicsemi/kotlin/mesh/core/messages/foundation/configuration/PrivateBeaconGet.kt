package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * Defines message sent to request the current Private Beacon state of the Node.
 * [PrivateBeaconStatus] will be the response to this message.
 */
class PrivateBeaconGet : AcknowledgedConfigMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray? = null
    override val responseOpCode: UInt = PrivateBeaconStatus.opCode

    override fun toString(): String = "PrivateBeaconGet"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8060u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { PrivateBeaconGet() }
    }
}
