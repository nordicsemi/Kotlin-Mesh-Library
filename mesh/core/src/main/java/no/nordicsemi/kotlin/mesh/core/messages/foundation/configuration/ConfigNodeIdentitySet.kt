package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage.Companion.decodeNetKeyIndex
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.NodeIdentityState

/**
 * Defines message sent to set the current Node Identity state. [ConfigNodeIdentityStatus]
 * will be the response to this message.
 *
 * @property networkKeyIndex   Network Key Index of the Node Identity state.
 * @property identityState     Expected node Identity state [NodeIdentityState].
 */
class ConfigNodeIdentitySet(
    override val networkKeyIndex: KeyIndex,
    val identityState: NodeIdentityState
) : AcknowledgedConfigMessage, ConfigNetKeyMessage {
    override val opCode = Initializer.opCode
    override val parameters = encodeNetKeyIndex() + byteArrayOf(identityState.value.toByte())
    override val responseOpCode: UInt = ConfigNodeIdentityStatus.opCode

    /**
     * Convenience constructor
     *
     * @param networkKeyIndex Network key index
     * @param start           If true, will set to [NodeIdentityState.RUNNING] or
     *                        [NodeIdentityState.STOPPED] otherwise.
     */
    constructor(networkKeyIndex: KeyIndex, start: Boolean) : this(
        networkKeyIndex = networkKeyIndex,
        identityState = if (start) NodeIdentityState.RUNNING else NodeIdentityState.STOPPED
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = "ConfigNodeIdentityGet(index: $networkKeyIndex, " +
            "nodeIdentityState: $identityState})"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8047u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 3 }
            ?.let {
                ConfigNodeIdentitySet(
                    networkKeyIndex = decodeNetKeyIndex(data = it, offset = 0),
                    identityState = NodeIdentityState.from(it[2].toUByte())
                )
            }
    }
}