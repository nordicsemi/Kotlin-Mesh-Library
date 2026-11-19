package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse

/**
 * SarTransmitterStatus is the response to [SarTransmitterGet] and [SarTransmitterSet].
 *
 * @property state Current SAR Transmitter state of the node.
 */
class SarTransmitterStatus(val state: SarTransmitterState) : ConfigResponse {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = state.encode()

    override fun toString() = "SarTransmitterStatus(state: $state)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x806Eu

        override fun init(parameters: ByteArray?) =
            SarTransmitterState.decode(parameters)?.let { SarTransmitterStatus(it) }
    }
}
