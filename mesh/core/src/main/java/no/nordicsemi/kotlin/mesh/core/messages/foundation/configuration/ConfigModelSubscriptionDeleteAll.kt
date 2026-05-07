package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigAnyModelMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import java.nio.ByteOrder

/**
 * This message is used to delete all subscriptions from a model.
 *
 * @property elementAddress    Element address of the model.
 * @property modelIdentifier   Model identifier.
 * @property companyIdentifier Company identifier, if the model is a vendor model.
 */
class ConfigModelSubscriptionDeleteAll(
    override val elementAddress: UnicastAddress,
    override val modelIdentifier: UShort,
    override val companyIdentifier: UShort?,
) : AcknowledgedConfigMessage, ConfigAnyModelMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = ConfigModelSubscriptionStatus.opCode
    override val parameters= elementAddress.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            (companyIdentifier?.toByteArray(order = ByteOrder.LITTLE_ENDIAN) ?: byteArrayOf()) +
            modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    /**
     * Convenience constructor to create a ConfigModelSubscriptionAdd message.
     *
     * @param model Model to add the subscription to.
     * @throws IllegalArgumentException If the model does not have a parent element.
     */
    @Throws(IllegalArgumentException::class)
    constructor(model: Model) : this(
        elementAddress = model.parentElement?.unicastAddress
            ?: throw IllegalArgumentException("Element address cannot be null"),
        modelIdentifier = when (model.modelId) {
            is SigModelId -> model.modelId.modelIdentifier
            is VendorModelId -> model.modelId.modelIdentifier
        },
        companyIdentifier = (model.modelId as? VendorModelId)?.companyIdentifier,
    )

    override fun toString() = "ConfigModelSubscriptionDeleteAll(" +
            "elementAddress: $elementAddress, " +
            "modelIdentifier: ${
                modelIdentifier.toHexString(
                    format = HexFormat {
                        number {
                            prefix = "0x"
                            upperCase = true
                        }
                    }
                )
            }" +
            if (companyIdentifier != null) {
                ", companyIdentifier: ${
                    companyIdentifier.toHexString(
                        format = HexFormat {
                            number {
                                prefix = "0x"
                                upperCase = true
                            }
                        }
                    )
                }"
            } else { "" } +
            ")"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x801Du

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 4 || it.size == 6
        }?.let { params ->
            ConfigModelSubscriptionDeleteAll(
                elementAddress = UnicastAddress(
                    address = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                ),
                companyIdentifier = if (params.size == 6) params.getUShort(
                    offset = 2,
                    order = ByteOrder.LITTLE_ENDIAN
                ) else null,
                modelIdentifier = if (params.size == 6) params.getUShort(
                    offset = 4,
                    order = ByteOrder.LITTLE_ENDIAN
                ) else params.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN)
            )
        }
    }
}