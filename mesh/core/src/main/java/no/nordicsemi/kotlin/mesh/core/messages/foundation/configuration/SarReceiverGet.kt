package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * SarReceiverGet is used to get the SAR Receiver state of a node. The response to this message is
 * a [SarReceiverStatus].
 */
class SarReceiverGet : AcknowledgedConfigMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SarReceiverStatus.opCode
    override val parameters: ByteArray? = null

    override fun toString() = "SarReceiverGet"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x806Fu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { SarReceiverGet() }
    }
}
