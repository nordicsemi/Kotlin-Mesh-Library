package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigAnyModelAddressMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import java.nio.ByteOrder

/**
 * This message is used to override a subscription of a model.
 *
 * @property address           Group address to be overwritten in subscriptions.
 * @property elementAddress    Element address of the model.
 * @property modelIdentifier   Model identifier.
 * @property companyIdentifier Company identifier, if the model is a vendor model.
 */
class ConfigModelSubscriptionOverwrite(
    override val address: Address,
    override val elementAddress: UnicastAddress,
    override val modelIdentifier: UShort,
    override val companyIdentifier: UShort?,
) : AcknowledgedConfigMessage, ConfigAnyModelAddressMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = ConfigModelSubscriptionStatus.opCode
    override val parameters= elementAddress.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            (companyIdentifier?.toByteArray(order = ByteOrder.LITTLE_ENDIAN) ?: byteArrayOf()) +
            modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    /**
     * Convenience constructor to create a ConfigModelSubscriptionAdd message.
     *
     * @param group Group to add the model subscription to.
     * @param model Model to add the subscription to.
     * @throws IllegalArgumentException If the model does not have a parent element.
     */
    @Suppress("unused")
    @Throws(IllegalArgumentException::class)
    constructor(group: Group, model: Model) : this(
        address = group.address.address,
        elementAddress = model.parentElement?.unicastAddress
            ?: throw IllegalArgumentException("Element address cannot be null"),
        modelIdentifier = when (model.modelId) {
            is SigModelId -> model.modelId.modelIdentifier
            is VendorModelId -> model.modelId.modelIdentifier
        },
        companyIdentifier = (model.modelId as? VendorModelId)?.companyIdentifier,
    )

    override fun toString() = "ConfigModelSubscriptionOverwrite(" +
            "address: ${
                address.toHexString(
                    format = HexFormat {
                        number {
                            prefix = "0x"
                            minLength = 4
                            upperCase = true
                        }
                    }
                )
            }, " +
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
        override val opCode = 0x801Eu

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 6 || it.size == 8
        }?.let { params ->
            ConfigModelSubscriptionOverwrite(
                elementAddress = UnicastAddress(
                    address = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                ),
                address = params.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN),
                companyIdentifier = if (params.size == 8) params.getUShort(
                    offset = 4,
                    order = ByteOrder.LITTLE_ENDIAN
                ) else null,
                modelIdentifier = if (params.size == 8) params.getUShort(
                    offset = 6,
                    order = ByteOrder.LITTLE_ENDIAN
                ) else params.getUShort(offset = 4, order = ByteOrder.LITTLE_ENDIAN)
            )
        }
    }
}