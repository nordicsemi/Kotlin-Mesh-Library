@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * This message is used to request the default ttl of a given node.
 */
class ConfigDefaultTtlGet : AcknowledgedConfigMessage {
    override val opCode: UInt = Initializer.opCode

    override val parameters = null

    override val responseOpCode = ConfigDefaultTtlStatus.opCode

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "ConfigDefaultTtlGet"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x800Cu

        /**
         * Initializes the [ConfigDefaultTtlGet] based on the given parameters.
         *
         * @param parameters Message parameters.
         * @return ConfigAppKeyAdd or null if the parameters are invalid.
         */
        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { ConfigDefaultTtlGet() }
    }
}