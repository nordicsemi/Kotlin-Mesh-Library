package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * Defines message sent to set the Private GATT Proxy state of the Node. [PrivateGattProxyStatus]
 * will be the response to this message.
 *
 * @property enabled True to enable the Private GATT Proxy feature or false to disable it.
 */
class PrivateGattProxySet(val enabled: Boolean) : AcknowledgedConfigMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = byteArrayOf(if (enabled) 0x01 else 0x00)
    override val responseOpCode: UInt = PrivateGattProxyStatus.opCode

    override fun toString() = "PrivateGattProxySet(enabled: $enabled)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8064u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 && it[0] in 0..1 }
            ?.let { PrivateGattProxySet(enabled = it[0].toInt() == 0x01) }
    }
}
