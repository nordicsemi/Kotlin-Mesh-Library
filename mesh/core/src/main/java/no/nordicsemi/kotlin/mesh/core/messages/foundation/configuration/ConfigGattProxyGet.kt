package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * Defines message sent to request the Current GATT Proxy state of the Node. [ConfigGattProxyStatus]
 * will be the response to this message.
 */
class ConfigGattProxyGet : AcknowledgedConfigMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray ? = null
    override val responseOpCode: UInt = ConfigGattProxyStatus.opCode

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = "ConfigGattProxyGet"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8012u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { ConfigGattProxyGet() }
    }
}