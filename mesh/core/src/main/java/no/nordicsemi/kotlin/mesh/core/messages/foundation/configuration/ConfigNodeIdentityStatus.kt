package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage.Companion.decodeNetKeyIndex
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.NodeIdentityState

/**
 * Defines message sent to request the current Node Identity state. [ConfigNodeIdentityStatus]
 * will be the response to this message.
 *
 * @property identity Node Identity state.
 * @property networkKeyIndex Network key index.
 * @property status Status of the message.
 */
class ConfigNodeIdentityStatus(
    override val status: ConfigMessageStatus,
    override val networkKeyIndex: KeyIndex,
    val identity: NodeIdentityState,
) : ConfigResponse, ConfigStatusMessage, ConfigNetKeyMessage {
    override val opCode = Initializer.opCode
    override val parameters = status.value.toByteArray() +
            encodeNetKeyIndex() +
            identity.value.toByte()

    constructor(request: ConfigNetKeyMessage) : this(
        status = ConfigMessageStatus.SUCCESS,
        networkKeyIndex = request.networkKeyIndex,
        // TODO Add support for Node Identity state
        identity = NodeIdentityState.NOT_SUPPORTED
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String =
        "ConfigNodeIdentityStatus(status: $status, networkKeyIndex: $networkKeyIndex, identity: $identity)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8048u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 4 }
            ?.let { params ->
                ConfigMessageStatus.from(params.first().toUByte())?.let {
                    ConfigNodeIdentityStatus(
                        status = it,
                        networkKeyIndex = decodeNetKeyIndex(params, 1),
                        identity = NodeIdentityState.from(params[3].toUByte())
                    )
                }
            }
    }
}