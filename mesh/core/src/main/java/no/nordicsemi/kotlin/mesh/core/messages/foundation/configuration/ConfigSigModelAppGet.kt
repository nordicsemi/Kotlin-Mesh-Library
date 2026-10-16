package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigModelMessage
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.data.ByteOrder

/**
 * This message is used to get the list of application keys bound to a given  Bluetooth SIG model.
 * The response to this message is a [ConfigSigModelAppList]
 *
 * @property modelId         Bluetooth SIG model identifier.
 * @property elementAddress  Address of the Element containing the model.
 * @constructor Constructs the ConfigSigModelAppGet message.
 */
class ConfigSigModelAppGet(
    override val modelId: SigModelId,
    override val elementAddress: UnicastAddress,
) : AcknowledgedConfigMessage, ConfigModelMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = ConfigSigModelAppList.opCode
    override val modelIdentifier: UShort = modelId.modelIdentifier
    override val parameters: ByteArray
        get() = elementAddress.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    override fun toString() = "ConfigSigModelAppGet(elementAddress: $elementAddress, " +
            "modelId: $modelId)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode: UInt = 0x804Bu

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 4
        }?.let { params ->
            ConfigSigModelAppGet(
                modelId = SigModelId(params.getUShort(0, ByteOrder.LITTLE_ENDIAN)),
                elementAddress = UnicastAddress(params.getUShort(2, ByteOrder.LITTLE_ENDIAN)),
            )
        }
    }
}