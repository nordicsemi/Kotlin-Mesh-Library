package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigModelMessage
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import java.nio.ByteOrder

/**
 * This message is used to get the list of Application Keys bound to a Vendor Model.
 */
class ConfigVendorModelAppGet(
    override val modelId: VendorModelId,
    override val elementAddress: UnicastAddress,
) : AcknowledgedConfigMessage, ConfigModelMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = ConfigVendorModelAppList.opCode
    override val modelIdentifier: UShort = modelId.modelIdentifier
    override val parameters: ByteArray
        get() = elementAddress.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                modelId.companyIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    override fun toString() = "ConfigVendorModelAppGet(elementAddress: $elementAddress, " +
            "modelId: $modelId)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode: UInt = 0x804Du

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 6
        }?.let { params ->
            ConfigVendorModelAppGet(
                elementAddress = UnicastAddress(params.getUShort(0, ByteOrder.LITTLE_ENDIAN)),
                modelId = VendorModelId(
                    companyIdentifier = params.getUShort(2, ByteOrder.LITTLE_ENDIAN),
                    modelIdentifier = params.getUShort(4, ByteOrder.LITTLE_ENDIAN)
                )
            )
        }
    }
}
