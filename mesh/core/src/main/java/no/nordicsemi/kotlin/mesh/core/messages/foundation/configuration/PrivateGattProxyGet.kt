package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * Defines message sent to request the current Private GATT Proxy state of the Node.
 * [PrivateGattProxyStatus] will be the response to this message.
 */
class PrivateGattProxyGet : AcknowledgedConfigMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray? = null
    override val responseOpCode: UInt = PrivateGattProxyStatus.opCode

    override fun toString(): String = "PrivateGattProxyGet"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8063u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { PrivateGattProxyGet() }
    }
}
