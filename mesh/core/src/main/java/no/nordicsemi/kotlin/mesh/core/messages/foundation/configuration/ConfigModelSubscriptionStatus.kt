package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ConfigAddressMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigAnyModelAddressMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigAnyModelMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigVirtualLabelMessage
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import java.nio.ByteOrder
import kotlin.uuid.ExperimentalUuidApi

/**
 * Status declaring if the the [ConfigModelSubscriptionStatus] operation succeeded or not.
 * This message is sent as a response to [ConfigModelSubscriptionAdd],
 * [ConfigModelSubscriptionDelete], [ConfigModelSubscriptionDeleteAll]
 * and [ConfigModelSubscriptionOverwrite].
 *
 * @property status           Status of the message.
 * @property address          Address of the group.
 * @property elementAddress   Element address of the model.
 * @property modelIdentifier  Model identifier.
 * @property companyIdentifier Company identifier, if the model is a vendor model.
 *
 */
class ConfigModelSubscriptionStatus(
    override val status: ConfigMessageStatus,
    override val address: Address,
    override val elementAddress: UnicastAddress,
    override val modelIdentifier: UShort,
    override val companyIdentifier: UShort?,
) : ConfigResponse, ConfigStatusMessage, ConfigAnyModelAddressMessage {
    override val opCode = Initializer.opCode
    override val parameters= status.value.toByteArray() +
            elementAddress.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            (companyIdentifier?.toByteArray(order = ByteOrder.LITTLE_ENDIAN) ?: byteArrayOf()) +
            modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    /**
     * Convenience constructor to create a ConfigModelSubscriptionStatus message.
     *
     * @param status            Status of the message.
     * @param address           Address of the group.
     * @param elementAddress    Element address of the model.
     * @param modelIdentifier   Model identifier.
     * @param companyIdentifier Company identifier, if the model is a vendor model.
     */
    constructor(
        status: ConfigMessageStatus,
        address: VirtualAddress,
        elementAddress: UnicastAddress,
        modelIdentifier: UShort,
        companyIdentifier: UShort? = null,
    ) : this(
        status = status,
        address = address.address,
        elementAddress = elementAddress,
        modelIdentifier = modelIdentifier,
        companyIdentifier = companyIdentifier
    )

    /**
     * Convenience constructor to create a ConfigModelSubscriptionStatus message.
     *
     * @param request   [ConfigModelSubscriptionDeleteAll] message that this is a response to.
     * @param status    Status of the message.
     */
    constructor(request: ConfigModelSubscriptionDeleteAll, status: ConfigMessageStatus) : this(
        status = status,
        address = UnassignedAddress.address,
        elementAddress = request.elementAddress,
        modelIdentifier = request.modelIdentifier,
        companyIdentifier = request.companyIdentifier
    )

    /**
     * Convenience constructor to create a ConfigModelSubscriptionStatus message.
     *
     * @param group Group to which the model is subscribed.
     * @param model Model that should subscribe.
     */
    constructor(group: Group, model: Model) : this(
        status = ConfigMessageStatus.SUCCESS,
        address = group.address.address,
        elementAddress = model.parentElement?.unicastAddress
            ?: throw IllegalArgumentException("Element address cannot be null"),
        modelIdentifier = when (model.modelId) {
            is SigModelId -> model.modelId.modelIdentifier
            is VendorModelId -> model.modelId.modelIdentifier
        },
        companyIdentifier = when (model.modelId) {
            is SigModelId -> null
            is VendorModelId -> model.modelId.companyIdentifier
        }
    )

    /**
     * Convenience constructor to create a ConfigModelSubscriptionStatus message.
     *
     * @param address Group address to which the model is subscribed.
     * @param model   Model that should subscribe.
     */
    constructor(address: GroupAddress, model: Model) : this(
        status = ConfigMessageStatus.SUCCESS,
        address = address.address,
        elementAddress = model.parentElement?.unicastAddress
            ?: throw IllegalArgumentException("Element address cannot be null"),
        modelIdentifier = when (model.modelId) {
            is SigModelId -> model.modelId.modelIdentifier
            is VendorModelId -> model.modelId.modelIdentifier
        },
        companyIdentifier = when (model.modelId) {
            is SigModelId -> null
            is VendorModelId -> model.modelId.companyIdentifier
        }
    )

    /**
     * Convenience constructor to create a ConfigModelSubscriptionStatus message.
     *
     * @param address Group address to which the model is subscribed.
     * @param model   Model that should subscribe.
     */
    constructor(address: VirtualAddress, model: Model) : this(
        status = ConfigMessageStatus.SUCCESS,
        address = address.address,
        elementAddress = model.parentElement?.unicastAddress
            ?: throw IllegalArgumentException("Element address cannot be null"),
        modelIdentifier = when (model.modelId) {
            is SigModelId -> model.modelId.modelIdentifier
            is VendorModelId -> model.modelId.modelIdentifier
        },
        companyIdentifier = when (model.modelId) {
            is SigModelId -> null
            is VendorModelId -> model.modelId.companyIdentifier
        }
    )

    /**
     * Convenience constructor to create a ConfigModelSubscriptionStatus message.
     *
     * @param model Model that should subscribe.
     */
    constructor(model: Model) : this(
        status = ConfigMessageStatus.SUCCESS,
        address = UnassignedAddress.address,
        elementAddress = model.parentElement?.unicastAddress
            ?: throw IllegalArgumentException("Element address cannot be null"),
        modelIdentifier = when (model.modelId) {
            is SigModelId -> model.modelId.modelIdentifier
            is VendorModelId -> model.modelId.modelIdentifier
        },
        companyIdentifier = when (model.modelId) {
            is SigModelId -> null
            is VendorModelId -> model.modelId.companyIdentifier
        }
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "ConfigModelSubscriptionStatus(" +
            "status: $status, " +
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
        override val opCode = 0x801Fu

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            parameters.size == 7 || parameters.size == 9
        }?.let { params ->
            ConfigMessageStatus.from(value = params[0].toUByte())?.let { status ->
                ConfigModelSubscriptionStatus(
                    status = status,
                    elementAddress = UnicastAddress(
                        address = params.getUShort(offset = 1, order = ByteOrder.LITTLE_ENDIAN)
                    ),
                    address = params.getUShort(offset = 3, order = ByteOrder.LITTLE_ENDIAN),
                    companyIdentifier = if (params.size == 9) {
                        params.getUShort(offset = 5, order = ByteOrder.LITTLE_ENDIAN)
                    } else null,
                    modelIdentifier = if (params.size == 9) {
                        params.getUShort(offset = 7, order = ByteOrder.LITTLE_ENDIAN)
                    } else params.getUShort(offset = 5, order = ByteOrder.LITTLE_ENDIAN),
                )
            }
        }

        /**
         * Initialises ConfigModelSubscriptionStatus message.
         *
         * @param request The request message that this is a response to.
         * @param status  Status of the message.
         * @return ConfigModelSubscriptionStatus message.
         */
        fun <T> init(request: T, status: ConfigMessageStatus): ConfigModelSubscriptionStatus
                where T : ConfigAddressMessage, T : ConfigAnyModelMessage {
            return ConfigModelSubscriptionStatus(
                address = request.address,
                elementAddress = request.elementAddress,
                modelIdentifier = request.modelIdentifier,
                companyIdentifier = request.companyIdentifier,
                status = status
            )
        }

        /**
         * Initialises ConfigModelSubscriptionStatus message.
         *
         * @param request The request message that this is a response to.
         * @param status  Status of the message.
         * @return ConfigModelSubscriptionStatus message.
         */
        @OptIn(ExperimentalUuidApi::class)
        fun <T> init(request: T, status: ConfigMessageStatus): ConfigModelSubscriptionStatus
                where T : ConfigVirtualLabelMessage, T : ConfigAnyModelMessage {
            return ConfigModelSubscriptionStatus(
                address = MeshAddress.create(uuid = request.virtualLabel),
                elementAddress = request.elementAddress,
                modelIdentifier = request.modelIdentifier,
                companyIdentifier = request.companyIdentifier,
                status = status
            )
        }
    }
}