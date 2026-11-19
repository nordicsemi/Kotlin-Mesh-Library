package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * SarTransmitterGet is used to get the SAR Transmitter state of a node. The response to this
 * message is a [SarTransmitterStatus].
 */
class SarTransmitterGet : AcknowledgedConfigMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SarTransmitterStatus.opCode
    override val parameters: ByteArray? = null

    override fun toString() = "SarTransmitterGet"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x806Cu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { SarTransmitterGet() }
    }
}
