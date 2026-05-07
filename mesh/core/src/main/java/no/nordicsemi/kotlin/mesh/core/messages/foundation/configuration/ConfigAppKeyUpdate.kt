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
 * This message is used to update an Application Key value on the AppKey List on a mesh node.
 *
 * This message initiates a Key Refresh Procedure. The message can be sent to remote nodes which are
 * not scheduled for exclusion.
 *
 * To transition to the next phases of the Key Refresh Procedure use [ConfigKeyRefreshPhaseSet].
 *
 * @property applicationKeyIndex  Index of the application key to be added.
 * @property networkKeyIndex     Index of the bound network key.
 * @property key                  The application key to be added.
 * @property opCode               Message op code.
 * @property parameters           Message parameters.
 * @property responseOpCode       Op Code of the response message.
 * @constructor Constructs the ConfigAppKeyAdd message.
 */
class ConfigAppKeyUpdate(
    override val applicationKeyIndex: KeyIndex,
    override val networkKeyIndex: KeyIndex,
    val key: ByteArray
) : AcknowledgedConfigMessage, ConfigNetAndAppKeyMessage {
    override val opCode: UInt = Initializer.opCode

    override val parameters = encodeNetAndAppKeyIndex(
        appKeyIndex = applicationKeyIndex,
        netKeyIndex = networkKeyIndex
    ) + key

    override val responseOpCode = ConfigAppKeyStatus.opCode

    /**
     * Convenience constructor to create a [ConfigAppKeyUpdate] message.
     *
     * @param applicationKey Application key to be updated.
     * @param newKey         New key value to be updated with.
     * @constructor Constructs the ConfigAppKeyAdd message.
     */
    constructor(applicationKey: ApplicationKey, newKey: ByteArray) : this(
        applicationKeyIndex = applicationKey.index,
        key = newKey,
        networkKeyIndex = applicationKey.boundNetKeyIndex
    )

    init {
        require(key.size == 16) { throw InvalidKeyLength() }
    }

    override fun toString() = "ConfigAppKeyUpdate(networkKeyIndex: $networkKeyIndex, " +
            "applicationKeyIndex: $applicationKeyIndex, key: 0x${key.toHexString(HexFormat.UpperCase)})"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x01u

        /**
         * Initializes the [ConfigAppKeyUpdate] based on the given parameters.
         *
         * @param parameters Message parameters.
         * @return ConfigAppKeyAdd or null if the parameters are invalid.
         */
        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 19
        }?.let {
            val decodedIndexes = decodeNetAndAppKeyIndex(data = it, offset = 0)
            ConfigAppKeyUpdate(
                networkKeyIndex = decodedIndexes.networkKeyIndex,
                applicationKeyIndex = decodedIndexes.applicationKeyIndex,
                key = it.copyOfRange(3, 19)
            )
        }
    }
}