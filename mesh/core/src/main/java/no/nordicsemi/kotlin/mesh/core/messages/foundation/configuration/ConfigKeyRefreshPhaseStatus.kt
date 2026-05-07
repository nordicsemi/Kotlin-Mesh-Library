@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage.Companion.decodeNetKeyIndex
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.KeyRefreshPhase
import no.nordicsemi.kotlin.mesh.core.model.KeyRefreshPhaseTransition
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.NormalOperation

/**
 * This message is used to set the [KeyRefreshPhaseTransition] for a given Network Key.
 *
 * @property refreshPhase Current Key Refresh Phase.
 */
class ConfigKeyRefreshPhaseStatus(
    override val status: ConfigMessageStatus,
    override val networkKeyIndex: KeyIndex,
    val refreshPhase: KeyRefreshPhase,
) : ConfigResponse, ConfigStatusMessage, ConfigNetKeyMessage {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray = status.value.toByteArray() +
            encodeNetKeyIndex() +
            refreshPhase.phase
                .toByte()
                .toByteArray()

    /**
     * Constructs a [ConfigKeyRefreshPhaseStatus] message using the given [NetworkKey].
     *
     * @param networkKey The [NetworkKey] to be used.
     */
    constructor(networkKey: NetworkKey) : this(
        status = ConfigMessageStatus.SUCCESS,
        networkKeyIndex = networkKey.index,
        refreshPhase = NormalOperation
    )

    /**
     * Convenience constructor to create the [ConfigKeyRefreshPhaseStatus] message.
     *
     * @param request  [ConfigNetKeyMessage] message that this is a response to.
     * @param status   status of the message.
     */
    constructor(request: ConfigNetKeyMessage, status: ConfigMessageStatus) : this(
        status = status,
        networkKeyIndex = request.networkKeyIndex,
        refreshPhase = NormalOperation
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "ConfigKeyRefreshPhaseStatus(status: $status, " +
            "networkKeyIndex: $networkKeyIndex, refreshPhase: $refreshPhase)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8017u

        /**
         * Initializes the [ConfigKeyRefreshPhaseStatus] based on the given parameters.
         *
         * @param parameters Message parameters.
         * @return ConfigKeyRefreshPhaseStatus or null if the parameters are invalid.
         */
        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 3 }
            ?.let { params ->
                ConfigMessageStatus.from(params.first().toUByte())?.let { status ->
                    ConfigKeyRefreshPhaseStatus(
                        status = status,
                        networkKeyIndex = decodeNetKeyIndex(data = params, offset = 0),
                        refreshPhase = KeyRefreshPhase.from(params[2].toInt())
                    )
                }
            }
    }
}