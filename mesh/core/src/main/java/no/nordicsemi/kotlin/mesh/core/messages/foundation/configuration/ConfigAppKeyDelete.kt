@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetAndAppKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetAndAppKeyMessage.Companion.decodeNetAndAppKeyIndex
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetAndAppKeyMessage.Companion.encodeNetAndAppKeyIndex
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex

/**
 * This message is used to delete an application key from a mesh node.
 *
 * @property applicationKeyIndex  Index of the application key to be deleted.
 * @property networkKeyIndex      Index of the bound network key.
 * @property opCode               Message op code.
 * @property parameters           Message parameters.
 * @property responseOpCode       Op Code of the response message.
 * @constructor Constructs the ConfigAppKeyAdd message.
 */
class ConfigAppKeyDelete(
    override val applicationKeyIndex: KeyIndex,
    override val networkKeyIndex: KeyIndex
) : AcknowledgedConfigMessage, ConfigNetAndAppKeyMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = ConfigAppKeyStatus.opCode
    override val parameters = encodeNetAndAppKeyIndex(
        appKeyIndex = applicationKeyIndex,
        netKeyIndex = networkKeyIndex
    )

    /**
     * Convenience constructor to create a [ConfigAppKeyDelete] message.
     *
     * @param key Application key to be added.
     * @constructor Constructs the [ConfigAppKeyDelete] message.
     */
    constructor(key: ApplicationKey) : this(
        applicationKeyIndex = key.index,
        networkKeyIndex = key.boundNetKeyIndex
    )

    override fun toString() = "ConfigAppKeyDelete(applicationKeyIndex: $applicationKeyIndex, " +
            "networkKeyIndex: $networkKeyIndex)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8000u

        /**
         * Initializes the [ConfigAppKeyDelete] based on the given parameters.
         *
         * @param parameters Message parameters.
         * @return ConfigAppKeyAdd or null if the parameters are invalid.
         */
        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 3
        }?.let {
            val decodedIndexes = decodeNetAndAppKeyIndex(data = it, offset = 0)
            ConfigAppKeyDelete(
                networkKeyIndex = decodedIndexes.networkKeyIndex,
                applicationKeyIndex = decodedIndexes.applicationKeyIndex
            )
        }
    }
}