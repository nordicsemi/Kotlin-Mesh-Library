@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.BaseMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage.Companion.decodeNetKeyIndex
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage.Companion.encodeNetKeyIndex
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey

/**
 * Status declaring if the the [ConfigNetKeyMessage] operation succeeded or not.
 *
 * @property opCode           Message op code.
 * @property networkKeyIndex  Index of the network key.
 * @property parameters       Message parameters.
 * @property status           Status of the message.
 * @constructor Constructs the ConfigNetKeyStatus message.
 */
class ConfigNetKeyStatus(
    override val networkKeyIndex: KeyIndex,
    override val status: ConfigMessageStatus
) : ConfigResponse, ConfigStatusMessage, ConfigNetKeyMessage {

    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() = status.value.toByteArray() + encodeNetKeyIndex(networkKeyIndex)

    /**
     * Constructs the ConfigNetKeyStatus message.
     *
     * @param request ConfigNetKeyMessage operation that was sent to the mesh node.
     * @param status  Status of the message.
     * @constructor Constructs the ConfigNetKeyStatus message.
     */
    constructor(
        request: ConfigNetKeyMessage,
        status: ConfigMessageStatus
    ) : this(request.networkKeyIndex, status)

    constructor(networkKey: NetworkKey) : this(networkKey.index, ConfigMessageStatus.SUCCESS)

    override fun toString(): String = "ConfigNetKeyStatus(networkKeyIndex: $networkKeyIndex, status: $status)"

    companion object Initializer : ConfigMessageInitializer {

        override val opCode = 0x8044u

        override fun init(parameters: ByteArray?): BaseMeshMessage? = parameters?.takeIf {
            it.size == 3
        }?.let { params ->
            ConfigMessageStatus.from(params.first().toUByte())?.let {
                ConfigNetKeyStatus(networkKeyIndex = decodeNetKeyIndex(params, 1), status = it)
            }
        }
    }
}