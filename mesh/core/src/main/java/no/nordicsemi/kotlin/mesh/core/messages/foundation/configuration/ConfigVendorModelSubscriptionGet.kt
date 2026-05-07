package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigVendorModelMessage
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import java.nio.ByteOrder

/**
 * This message is used to get the subscription list of a vendor model.
 */
class ConfigVendorModelSubscriptionGet(
    override val elementAddress: UnicastAddress,
    override val modelId: VendorModelId,
) : AcknowledgedConfigMessage, ConfigVendorModelMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = ConfigSigModelSubscriptionList.opCode
    override val modelIdentifier = modelId.modelIdentifier
    override val companyIdentifier = modelId.companyIdentifier
    override val parameters = byteArrayOf() +
            elementAddress.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    /**
     * Convenience constructor to create a ConfigVendorModelSubscriptionGet message.
     *
     * @param model Model to get the subscription list from.
     * @throws IllegalArgumentException If the model does not have a parent element.
     * @throws IllegalArgumentException If the model is not a vendor model.
     */
    constructor(model: Model) : this(
        elementAddress = model.parentElement?.unicastAddress
            ?: throw IllegalArgumentException("Element address cannot be null"),
        modelId = when (model.modelId) {
            is VendorModelId -> model.modelId
            else -> throw IllegalArgumentException("Only Vendor models are supported")
        }
    )

    override fun toString() = "ConfigVendorModelSubscriptionGet(" +
            "elementAddress: $elementAddress, modelId: $modelId)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x802Bu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 6 }
            ?.let { params ->
                ConfigVendorModelSubscriptionGet(
                    elementAddress = UnicastAddress(
                        address = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                    ),
                    modelId = VendorModelId(
                        companyIdentifier = params.getUShort(2, ByteOrder.LITTLE_ENDIAN),
                        modelIdentifier = params.getUShort(4, ByteOrder.LITTLE_ENDIAN)
                    )
                )
            }
    }
}