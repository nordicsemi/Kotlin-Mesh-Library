@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * This message is used to get the Heartbeat Publication state of an element.
 *
 * @property page          Page number of the Composition Data.
 * @constructor Creates a ConfigHeartbeatPublicationGet message.
 */
class ConfigCompositionDataGet(val page: UByte) : AcknowledgedConfigMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = ConfigCompositionDataStatus.opCode
    override val parameters = byteArrayOf(page.toByte())

    override fun toString() = "ConfigCompositionDataGet(page: $page)"
    
    companion object Initializer : ConfigMessageInitializer {
        override val opCode: UInt = 0x8008u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 1
        }?.let {
            ConfigCompositionDataGet(it[0].toUByte())
        }
    }
}