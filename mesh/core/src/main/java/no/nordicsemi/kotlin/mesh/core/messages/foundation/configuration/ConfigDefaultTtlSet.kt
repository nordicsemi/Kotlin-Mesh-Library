@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * This message is used to set the default ttl of a given node.
 *
 * @property ttl The default TTL value.
 */
class ConfigDefaultTtlSet(val ttl: UByte) : AcknowledgedConfigMessage {
    override val opCode: UInt = Initializer.opCode

    override val parameters: ByteArray = ttl.toByteArray()

    override val responseOpCode = ConfigDefaultTtlStatus.opCode

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "ConfigDefaultTtlSet(ttl: $ttl)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x800Du

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { ConfigDefaultTtlSet(parameters[0].toUByte()) }
    }
}