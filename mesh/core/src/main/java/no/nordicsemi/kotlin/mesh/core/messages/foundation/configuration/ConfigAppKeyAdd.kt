@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.exception.InvalidKeyLength
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetAndAppKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetAndAppKeyMessage.Companion.decodeNetAndAppKeyIndex
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetAndAppKeyMessage.Companion.encodeNetAndAppKeyIndex
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex

/**
 * This message is used to add an Application Key to a mesh node.
 *
 * @property applicationKeyIndex             Index of the application key to be added.
 * @property networkKeyIndex                Index of the bound network key.
 * @property key                  The application key to be added.
 * @property opCode               Message op code.
 * @property parameters           Message parameters.
 * @property responseOpCode       Op Code of the response message.
 * @constructor Constructs the ConfigAppKeyAdd message.
 */
class ConfigAppKeyAdd(
    override val applicationKeyIndex: KeyIndex,
    val key: ByteArray,
    override val networkKeyIndex: KeyIndex,
) : AcknowledgedConfigMessage, ConfigNetAndAppKeyMessage {
    override val opCode: UInt = Initializer.opCode

    override val parameters = encodeNetAndAppKeyIndex(
        appKeyIndex = applicationKeyIndex,
        netKeyIndex = networkKeyIndex
    ) + key

    override val responseOpCode = ConfigAppKeyStatus.opCode

    /**
     * Convenience constructor to create a [ConfigAppKeyAdd] message.
     *
     * @param key Application key to be added.
     * @constructor Constructs the ConfigAppKeyAdd message.
     */
    constructor(key: ApplicationKey) : this(
        applicationKeyIndex = key.index,
        key = key.key,
        networkKeyIndex = key.boundNetKeyIndex
    )

    init {
        require(key.size == 16) { throw InvalidKeyLength() }
    }

    override fun toString() = "ConfigAppKeyAdd(networkKeyIndex: $networkKeyIndex, " +
            "applicationKeyIndex: $applicationKeyIndex, key: 0x${key.toHexString(HexFormat.UpperCase)})"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x00u

        /**
         * Initializes the [ConfigAppKeyAdd] based on the given parameters.
         *
         * @param parameters Message parameters.
         * @return ConfigAppKeyAdd or null if the parameters are invalid.
         */
        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 19
        }?.let {
            val decodedIndexes = decodeNetAndAppKeyIndex(data = it, offset = 0)
            ConfigAppKeyAdd(
                networkKeyIndex = decodedIndexes.networkKeyIndex,
                applicationKeyIndex = decodedIndexes.applicationKeyIndex,
                key = it.copyOfRange(3, 19)
            )
        }
    }
}