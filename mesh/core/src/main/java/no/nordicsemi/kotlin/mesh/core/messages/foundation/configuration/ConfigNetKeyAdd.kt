@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.exception.InvalidKeyLength
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage.Companion.decodeNetKeyIndex
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey

/**
 * This message is used to add a network key to a mesh node.
 *
 * @property networkKeyIndex      Index of the network key to be added.
 * @property key                  The network key to be added.
 * @property opCode               Message op code.
 * @property parameters           Message parameters.
 * @property responseOpCode       Op Code of the response message.
 * @constructor Constructs the ConfigNetKeyDelete message.
 */
class ConfigNetKeyAdd(
    override val networkKeyIndex: KeyIndex,
    val key: ByteArray,
) : AcknowledgedConfigMessage, ConfigNetKeyMessage {
    override val opCode: UInt = Initializer.opCode
    override val parameters = encodeNetKeyIndex() + key
    override val responseOpCode = ConfigNetKeyStatus.opCode

    /**
     * Convenience constructor to create a ConfigNetKeyAdd message.
     *
     * @param key Network key to be added.
     */
    constructor(key: NetworkKey) : this(networkKeyIndex = key.index, key = key.key)

    init {
        require(key.size == 16) { throw InvalidKeyLength() }
    }

    override fun toString(): String =
        "ConfigNetKeyAdd(networkKeyIndex: $networkKeyIndex, " +
        "key: 0x${key.toHexString(HexFormat.UpperCase)})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConfigNetKeyAdd

        if (networkKeyIndex != other.networkKeyIndex) return false
        if (!key.contentEquals(other.key)) return false
        if (opCode != other.opCode) return false
        if (!parameters.contentEquals(other.parameters)) return false
        if (responseOpCode != other.responseOpCode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = networkKeyIndex.hashCode()
        result = 31 * result + key.contentHashCode()
        result = 31 * result + opCode.hashCode()
        result = 31 * result + parameters.contentHashCode()
        result = 31 * result + responseOpCode.hashCode()
        return result
    }

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8040u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 18
        }?.let {
            val netKeyIndex = decodeNetKeyIndex(data = it, offset = 0)
            val key = it.copyOfRange(2, 18)
            ConfigNetKeyAdd(networkKeyIndex = netKeyIndex, key = key)
        }
    }
}