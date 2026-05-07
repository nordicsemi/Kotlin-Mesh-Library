package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.toHexString
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage.Companion.decodeNetKeyIndex
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex

/**
 * Defines message sent to request the current Node Identity state. [ConfigNodeIdentityStatus]
 * will be the response to this message.
 */
class ConfigNodeIdentityGet(
    override val networkKeyIndex: KeyIndex
) : AcknowledgedConfigMessage, ConfigNetKeyMessage {
    override val opCode = Initializer.opCode
    override val parameters = encodeNetKeyIndex()
    override val responseOpCode: UInt = ConfigNodeIdentityStatus.opCode

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = "ConfigNodeIdentityGet(networkKeyIndex: $networkKeyIndex)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8046u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let { ConfigNodeIdentityGet(decodeNetKeyIndex(data = it, offset = 0)) }
    }
}