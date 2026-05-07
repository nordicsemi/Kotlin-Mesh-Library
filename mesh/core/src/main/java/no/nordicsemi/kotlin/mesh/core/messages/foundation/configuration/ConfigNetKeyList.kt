@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey

/**
 * This message returns the list of network key indexes added to the mesh node.
 *
 * This is a response to the [ConfigNetKeyGet] message.
 *
 * @param networkKeyIndexes    List of network key indexes added to the mesh node.
 */
class ConfigNetKeyList(
    val networkKeyIndexes: List<KeyIndex>
) : ConfigResponse {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray
        get() = ConfigMessage.encode(indexes = networkKeyIndexes)

    override fun toString() =
        "ConfigNetKeyList(networkKeyIndexes: $networkKeyIndexes)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8043u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                ConfigNetKeyList(ConfigMessage.decode(data = it, offset = 0))
            }
    }
}