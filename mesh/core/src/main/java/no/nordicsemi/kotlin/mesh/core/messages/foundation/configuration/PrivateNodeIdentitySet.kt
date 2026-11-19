package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage.Companion.decodeNetKeyIndex
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex

/**
 * Defines message sent to set the Private Node Identity state. [PrivateNodeIdentityStatus] will be
 * the response to this message.
 *
 * @property networkKeyIndex Network Key Index of the Private Node Identity state.
 * @property enabled         True to start advertising with the Private Node Identity.
 */
class PrivateNodeIdentitySet(
    override val networkKeyIndex: KeyIndex,
    val enabled: Boolean,
) : AcknowledgedConfigMessage, ConfigNetKeyMessage {
    override val opCode = Initializer.opCode
    override val parameters = encodeNetKeyIndex() + byteArrayOf(if (enabled) 0x01 else 0x00)
    override val responseOpCode: UInt = PrivateNodeIdentityStatus.opCode

    override fun toString() =
        "PrivateNodeIdentitySet(networkKeyIndex: $networkKeyIndex, enabled: $enabled)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8067u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 3 && it[2] in 0..1 }
            ?.let {
                PrivateNodeIdentitySet(
                    networkKeyIndex = decodeNetKeyIndex(data = it, offset = 0),
                    enabled = it[2].toInt() == 0x01
                )
            }
    }
}
