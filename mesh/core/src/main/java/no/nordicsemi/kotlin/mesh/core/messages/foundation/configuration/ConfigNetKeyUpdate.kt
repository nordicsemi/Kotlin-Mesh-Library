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
 * @property networkKeyIndex                Index of the network key to be added.
 * @property newKey               The new network key to be added.
 * @property opCode               Message op code.
 * @property parameters           Message parameters.
 * @property responseOpCode       Op Code of the response message.
 * @constructor Constructs the ConfigNetKeyUpdate message.
 */
class ConfigNetKeyUpdate(
    override val networkKeyIndex: KeyIndex,
    val newKey: ByteArray
) : AcknowledgedConfigMessage, ConfigNetKeyMessage {
    override val opCode: UInt = Initializer.opCode

    override val parameters: ByteArray
        get() = encodeNetKeyIndex() + newKey

    override val responseOpCode = ConfigNetKeyStatus.opCode

    /**
     * Convenience constructor to create a ConfigNetKeyUpdate message.
     *
     * @param networkKey Network key to be added.
     */
    constructor(networkKey: NetworkKey, newKey: ByteArray) : this(
        networkKeyIndex = networkKey.index,
        newKey = newKey
    )

    init {
        require(newKey.size == 16) { throw InvalidKeyLength() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConfigNetKeyUpdate

        if (networkKeyIndex != other.networkKeyIndex) return false
        if (!newKey.contentEquals(other.newKey)) return false
        if (opCode != other.opCode) return false
        if (!parameters.contentEquals(other.parameters)) return false
        if (responseOpCode != other.responseOpCode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = networkKeyIndex.hashCode()
        result = 31 * result + newKey.contentHashCode()
        result = 31 * result + opCode.hashCode()
        result = 31 * result + parameters.contentHashCode()
        result = 31 * result + responseOpCode.hashCode()
        return result
    }

    override fun toString(): String = "ConfigNetKeyUpdate(networkKeyIndex: $networkKeyIndex, " +
            "key: 0x${newKey.toHexString(HexFormat.UpperCase)})"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8045u

        /**
         * Initializes the ConfigNetKeyUpdate message based on the given parameters.
         *
         * @param parameters Byte array containing the message parameters.
         */
        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 18
        }?.let {
            val netKeyIndex = decodeNetKeyIndex(data = it, offset = 0)
            val key = it.copyOfRange(2, 18)
            ConfigNetKeyUpdate(networkKeyIndex = netKeyIndex, newKey = key)
        }
    }
}