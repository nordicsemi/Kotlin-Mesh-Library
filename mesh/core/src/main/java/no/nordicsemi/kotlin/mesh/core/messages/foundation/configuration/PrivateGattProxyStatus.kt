package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.model.FeatureState

/**
 * Defines the response to a [PrivateGattProxyGet] or [PrivateGattProxySet] message.
 *
 * @property state The state of the Private GATT Proxy feature.
 */
class PrivateGattProxyStatus(val state: FeatureState) : ConfigResponse {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = byteArrayOf(state.value.toByte())

    override fun toString() = "PrivateGattProxyStatus(state: $state)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8065u

        /**
         * The status reporting that the Private GATT Proxy feature is not supported.
         */
        val unsupported get() = PrivateGattProxyStatus(state = FeatureState.Unsupported)

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 && it[0] in 0..2 }
            ?.let { PrivateGattProxyStatus(state = FeatureState.from(it[0].toInt())) }
    }
}
