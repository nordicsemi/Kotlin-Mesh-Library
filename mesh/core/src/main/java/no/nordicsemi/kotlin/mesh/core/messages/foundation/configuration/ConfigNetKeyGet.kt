@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * This message is used to list the network keys from a given node.
 *
 * @property opCode               Message op code.
 * @property parameters           Message parameters.
 * @property responseOpCode       Op Code of the response message.
 * @constructor Constructs the ConfigNetKeyGet message.
 */
class ConfigNetKeyGet : AcknowledgedConfigMessage {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray? = null

    override val responseOpCode = ConfigNetKeyList.opCode

    override fun toString() = "ConfigNetKeyGet"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8042u
        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.isEmpty()
        }?.let {
            ConfigNetKeyGet()
        }
    }
}