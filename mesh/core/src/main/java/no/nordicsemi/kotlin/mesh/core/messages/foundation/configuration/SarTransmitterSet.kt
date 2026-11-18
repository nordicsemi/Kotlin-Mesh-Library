package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * SarTransmitterSet is used to set the SAR Transmitter state of a node. The response to this
 * message is a [SarTransmitterStatus].
 *
 * @property state SAR Transmitter state to set.
 */
class SarTransmitterSet(val state: SarTransmitterState) : AcknowledgedConfigMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SarTransmitterStatus.opCode
    override val parameters: ByteArray = state.encode()

    override fun toString() = "SarTransmitterSet(state: $state)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x806Du

        override fun init(parameters: ByteArray?) =
            SarTransmitterState.decode(parameters)?.let { SarTransmitterSet(it) }
    }
}
