package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.model.FeatureState

/**
 * Defines message sent to set the Current GATT Proxy state of the Node. [ConfigGattProxyStatus]
 * will be the response to this message.
 *
 * @property state Feature state of the GATT Proxy feature.
 */
data class ConfigGattProxySet(val state: FeatureState) : AcknowledgedConfigMessage {
    override val opCode = Initializer.opCode
    override val parameters = byteArrayOf(state.value.toByte())
    override val responseOpCode: UInt = ConfigGattProxyStatus.opCode

    /**
     * Constructs a ConfigGattProxySet message.
     *
     * @param enable Boolean value to enable or disable the GATT Proxy feature.
     */
    constructor(enable: Boolean) :
            this(state = if (enable) FeatureState.Enabled else FeatureState.Disabled)

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "ConfigGattProxySet(state: $state)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8013u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let {
                ConfigGattProxySet(FeatureState.from(it[0].toUByte().toInt()))
            }
    }
}