package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage.Companion.decodeNetKeyIndex
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.FeatureState
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex

/**
 * Defines the response to a [PrivateNodeIdentityGet] or [PrivateNodeIdentitySet] message.
 *
 * @property status          Status of the message.
 * @property networkKeyIndex Network Key Index.
 * @property state           Private Node Identity state.
 */
class PrivateNodeIdentityStatus(
    override val status: ConfigMessageStatus,
    override val networkKeyIndex: KeyIndex,
    val state: FeatureState,
) : ConfigResponse, ConfigStatusMessage, ConfigNetKeyMessage {
    override val opCode = Initializer.opCode
    override val parameters =
        byteArrayOf(status.value.toByte()) + encodeNetKeyIndex() + byteArrayOf(state.value.toByte())

    /**
     * Constructs a status responding to the given request, reporting the feature as unsupported.
     *
     * @param request Request containing the Network Key Index.
     */
    constructor(request: ConfigNetKeyMessage) : this(
        status = ConfigMessageStatus.SUCCESS,
        networkKeyIndex = request.networkKeyIndex,
        state = FeatureState.Unsupported
    )

    override fun toString() = "PrivateNodeIdentityStatus(status: $status, " +
            "networkKeyIndex: $networkKeyIndex, state: $state)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8068u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 4 && it[3] in 0..2 }
            ?.let { params ->
                ConfigMessageStatus.from(params[0].toUByte())?.let {
                    PrivateNodeIdentityStatus(
                        status = it,
                        networkKeyIndex = decodeNetKeyIndex(data = params, offset = 1),
                        state = FeatureState.from(params[3].toInt())
                    )
                }
            }
    }
}
