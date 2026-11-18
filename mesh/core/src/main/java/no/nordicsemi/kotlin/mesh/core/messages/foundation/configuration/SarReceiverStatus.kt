package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse

/**
 * SarReceiverStatus is the response to [SarReceiverGet] and [SarReceiverSet].
 *
 * @property state Current SAR Receiver state of the node.
 */
class SarReceiverStatus(val state: SarReceiverState) : ConfigResponse {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = state.encode()

    override fun toString() = "SarReceiverStatus(state: $state)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8071u

        override fun init(parameters: ByteArray?) =
            SarReceiverState.decode(parameters)?.let { SarReceiverStatus(it) }
    }
}
