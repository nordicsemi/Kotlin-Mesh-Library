@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage.Companion.decodeNetKeyIndex
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey

/**
 * This message is used to request all the application key indexes that are bound to a given network
 * key.
 *
 * @param networkKeyIndex Index of the bound network key.
 */
class ConfigAppKeyGet(
    override val networkKeyIndex: KeyIndex,
) : AcknowledgedConfigMessage, ConfigNetKeyMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = ConfigAppKeyList.opCode
    override val parameters = encodeNetKeyIndex()

    /**
     * Convenience constructor to create a [ConfigAppKeyGet] message.
     *
     * @param key Network key for which the application keys are requested.
     * @constructor Constructs the ConfigAppKeyAdd message.
     */
    constructor(key: NetworkKey) : this(
        networkKeyIndex = key.index
    )

    override fun toString() = "ConfigAppKeyGet(networkKeyIndex: $networkKeyIndex)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8001u

        /**
         * Initializes the [ConfigAppKeyGet] based on the given parameters.
         *
         * @param parameters Message parameters.
         * @return ConfigAppKeyAdd or null if the parameters are invalid.
         */
        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let { params ->
                ConfigAppKeyGet(networkKeyIndex = decodeNetKeyIndex(params, 0))
            }
    }
}