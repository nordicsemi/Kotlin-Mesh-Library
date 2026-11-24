package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage.Companion.decodeNetKeyIndex
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex

/**
 * Defines message sent to request the current Private Node Identity state.
 * [PrivateNodeIdentityStatus] will be the response to this message.
 *
 * @property networkKeyIndex Network Key Index of the Private Node Identity state.
 */
class PrivateNodeIdentityGet(
    override val networkKeyIndex: KeyIndex,
) : AcknowledgedConfigMessage, ConfigNetKeyMessage {
    override val opCode = Initializer.opCode
    override val parameters = encodeNetKeyIndex()
    override val responseOpCode: UInt = PrivateNodeIdentityStatus.opCode

    override fun toString() = "PrivateNodeIdentityGet(networkKeyIndex: $networkKeyIndex)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8066u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let { PrivateNodeIdentityGet(decodeNetKeyIndex(data = it, offset = 0)) }
    }
}
