@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.model.FeatureState
import no.nordicsemi.kotlin.mesh.core.model.Node

/**
 * Defines the response to a [ConfigGattProxyGet] or [ConfigGattProxySet] message.
 *
 * @property state The state of the GATT Proxy feature.
 */
class ConfigGattProxyStatus(val state: FeatureState) : ConfigResponse {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = byteArrayOf(state.value.toByte())

    constructor(node: Node) : this(node.features.proxy?.state ?: FeatureState.Unsupported)

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() =
        "ConfigGattProxyStatus(state: $state)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8014u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { ConfigGattProxyStatus(state = FeatureState.from(it[0].toUInt().toInt())) }

        /**
         * The status reporting that the GATT Proxy feature is not supported.
         */
        val unsupported = ConfigGattProxyStatus(state = FeatureState.Unsupported)
    }
}