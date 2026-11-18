package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * SarReceiverSet is used to set the SAR Receiver state of a node. The response to this message is
 * a [SarReceiverStatus].
 *
 * @property state SAR Receiver state to set.
 */
class SarReceiverSet(val state: SarReceiverState) : AcknowledgedConfigMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SarReceiverStatus.opCode
    override val parameters: ByteArray = state.encode()

    override fun toString() = "SarReceiverSet(state: $state)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8070u

        override fun init(parameters: ByteArray?) =
            SarReceiverState.decode(parameters)?.let { SarReceiverSet(it) }
    }
}
