@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage.Companion.decodeNetKeyIndex
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.KeyRefreshPhaseTransition
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey

/**
 * This message is used to set the [KeyRefreshPhaseTransition] for a given Network Key.
 *
 * @property transition New Key Refresh Phase Transition to be set.
 */
class ConfigKeyRefreshPhaseSet(
    override val networkKeyIndex: KeyIndex,
    val transition: KeyRefreshPhaseTransition,
) : AcknowledgedConfigMessage, ConfigNetKeyMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = ConfigKeyRefreshPhaseStatus.opCode
    override val parameters: ByteArray = encodeNetKeyIndex() +
            transition
                .rawValue
                .toByte()
                .toByteArray()

    /**
     * Constructs a [ConfigKeyRefreshPhaseSet] message using the given [NetworkKey].
     *
     * @param networkKey The [NetworkKey] to be used.
     * @param transition New Key Refresh Phase Transition to be set.
     */
    constructor(
        networkKey: NetworkKey,
        transition: KeyRefreshPhaseTransition,
    ) : this(networkKeyIndex = networkKey.index, transition = transition)

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "ConfigKeyRefreshPhaseSet(networkKeyIndex: ${networkKeyIndex}, " +
            "transition: $transition)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8016u

        /**
         * Initializes the [ConfigKeyRefreshPhaseSet] based on the given parameters.
         *
         * @param parameters Message parameters.
         * @return ConfigKeyRefreshPhaseGet or null if the parameters are invalid.
         */
        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 3 }
            ?.let {
                ConfigKeyRefreshPhaseSet(
                    networkKeyIndex = decodeNetKeyIndex(data = it, offset = 0),
                    transition = KeyRefreshPhaseTransition.init(it[2].toInt())
                )
            }
    }
}