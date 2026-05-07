@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.model.FeatureState
import no.nordicsemi.kotlin.mesh.core.model.Node

/**
 * Defines the response to a [ConfigFriendGet] or [ConfigFriendSet] message.
 *
 * @property state The state of the Friend feature.
 */
data class ConfigFriendStatus(val state: FeatureState) : ConfigResponse {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = byteArrayOf(state.value.toByte())

    constructor(node: Node) : this(node.features.friend?.state ?: FeatureState.Unsupported)

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "ConfigFriendStatus(state: $state)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8011u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { ConfigFriendStatus(state = FeatureState.from(it[0].toUInt().toInt())) }

        /**
         * The status reporting that the Friend feature is not supported.
         */
        val unsupported = ConfigFriendStatus(state = FeatureState.Unsupported)
    }
}