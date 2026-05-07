@file:Suppress("ConvertSecondaryConstructorToPrimary", "unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.BaseMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * This message is used to reset the node and will be reset to the unprovisioned state.
 */
class ConfigNodeReset : AcknowledgedConfigMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = ConfigNodeResetStatus.opCode
    override val parameters = null

    override fun toString() = "ConfigNodeReset"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode: UInt = 0x8049u

        override fun init(parameters: ByteArray?): BaseMeshMessage? = parameters.takeIf {
            it != null && it.isEmpty()
        }?.let {
            ConfigNodeReset()
        }
    }
}