package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.getUuid
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigAnyModelMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigVirtualLabelMessage
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import java.nio.ByteOrder
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * This message is used to add a virtual address to the model subscriptions list.
 *
 * @property virtualLabel      Virtual address of the group.
 * @property elementAddress    Element address of the model.
 * @property modelIdentifier   Model identifier.
 * @property companyIdentifier Company identifier, if the model is a vendor model.
 */
@OptIn(ExperimentalUuidApi::class)
@Suppress("unused")
class ConfigModelSubscriptionVirtualAddressAdd(
    override val virtualLabel: Uuid,
    override val elementAddress: UnicastAddress,
    override val modelIdentifier: UShort,
    override val companyIdentifier: UShort?,
) : AcknowledgedConfigMessage, ConfigVirtualLabelMessage, ConfigAnyModelMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = ConfigModelSubscriptionStatus.opCode
    override val parameters= elementAddress.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            virtualLabel.toByteArray() +
            (companyIdentifier?.toByteArray(order = ByteOrder.LITTLE_ENDIAN) ?: byteArrayOf()) +
            modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    /**
     * Convenience constructor to create a ConfigModelSubscriptionAdd message.
     *
     * @param virtualAddress Virtual address to add the model subscription to.
     * @param model Model to add the subscription to.
     * @throws IllegalArgumentException If the model does not have a parent element.
     */
    @Throws(IllegalArgumentException::class)
    constructor(virtualAddress: VirtualAddress, model: Model) : this(
        virtualLabel = virtualAddress.uuid,
        model = model
    )

    /**
     * Convenience constructor to create a ConfigModelSubscriptionAdd message.
     *
     * @param virtualLabel Virtual label to add the model subscription to.
     * @param model Model to add the subscription to.
     * @throws IllegalArgumentException If the model does not have a parent element.
     */
    @Throws(IllegalArgumentException::class)
    constructor(virtualLabel: Uuid, model: Model) : this(
        virtualLabel = virtualLabel,
        elementAddress = model.parentElement?.unicastAddress
            ?: throw IllegalArgumentException("Element address cannot be null"),
        modelIdentifier = when (model.modelId) {
            is SigModelId -> model.modelId.modelIdentifier
            is VendorModelId -> model.modelId.modelIdentifier
        },
        companyIdentifier = (model.modelId as? VendorModelId)?.companyIdentifier,
    )

    /**
     * Convenience constructor to create a ConfigModelSubscriptionAdd message.
     *
     * @param group Group to add the model subscription to.
     * @param model Model to add the subscription to.
     * @throws IllegalArgumentException If the model does not have a parent element.
     */
    @Throws(IllegalArgumentException::class)
    constructor(group: Group, model: Model) : this(
        virtualLabel = (group.address as? VirtualAddress)?.uuid
            ?: throw IllegalArgumentException("Group address must be a virtual address"),
        elementAddress = model.parentElement?.unicastAddress
            ?: throw IllegalArgumentException("Element address cannot be null"),
        modelIdentifier = when (model.modelId) {
            is SigModelId -> model.modelId.modelIdentifier
            is VendorModelId -> model.modelId.modelIdentifier
        },
        companyIdentifier = (model.modelId as? VendorModelId)?.companyIdentifier,
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "ConfigModelSubscriptionVirtualAddressAdd(" +
            "virtualLabel: ${virtualLabel}, " +
            "elementAddress: $elementAddress, " +
            "modelIdentifier: ${
                modelIdentifier.toHexString(
                    format = HexFormat {
                        number {
                            prefix = "0x"
                            minLength = 4
                            upperCase = true
                        }
                    }
                )
            }" +
            companyIdentifier?.let {
                ", companyIdentifier: ${
                    it.toHexString(
                        format = HexFormat {
                            number {
                                prefix = "0x"
                                minLength = 4
                                upperCase = true
                            }
                        }
                    )
                }"
            } +
            ")"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8020u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 20 || it.size == 22
        }?.let { params ->
            ConfigModelSubscriptionVirtualAddressAdd(
                elementAddress = UnicastAddress(
                    address = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                ),
                virtualLabel = params.getUuid(offset = 2),
                companyIdentifier = if (params.size == 24) params.getUShort(
                    offset = 18,
                    order = ByteOrder.LITTLE_ENDIAN
                ) else null,
                modelIdentifier = if (params.size == 24) params.getUShort(
                    offset = 20,
                    order = ByteOrder.LITTLE_ENDIAN
                ) else params.getUShort(offset = 18, order = ByteOrder.LITTLE_ENDIAN)
            )
        }
    }
}