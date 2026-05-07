@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage.Companion.decodeNetKeyIndex
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey

/**
 * This message is used to request the [no.nordicsemi.kotlin.mesh.core.model.KeyRefreshPhaseTransition] for a given Network Key.
 *
 * @property networkKeyIndex The index of the Network Key.
 */
class ConfigKeyRefreshPhaseGet(override val networkKeyIndex: KeyIndex) : AcknowledgedConfigMessage,
    ConfigNetKeyMessage {
    override val opCode: UInt = Initializer.opCode

    override val parameters: ByteArray? = encodeNetKeyIndex()

    override val responseOpCode = ConfigKeyRefreshPhaseStatus.opCode

    /**
     * Constructs a [ConfigKeyRefreshPhaseGet] message using the given [NetworkKey].
     */
    constructor(networkKey: NetworkKey) : this(networkKeyIndex = networkKey.index)

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "ConfigKeyRefreshPhaseGet(networkKeyIndex: $networkKeyIndex)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8015u

        /**
         * Initializes the [ConfigKeyRefreshPhaseGet] based on the given parameters.
         *
         * @param parameters Message parameters.
         * @return ConfigKeyRefreshPhaseGet or null if the parameters are invalid.
         */
        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let { ConfigKeyRefreshPhaseGet(networkKeyIndex = decodeNetKeyIndex(data = it, offset = 0)) }
    }
}