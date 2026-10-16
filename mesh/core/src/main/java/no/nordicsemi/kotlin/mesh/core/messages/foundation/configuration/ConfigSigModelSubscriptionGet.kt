package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getShort
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigModelMessage
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.data.ByteOrder

/**
 * This message is used to get the subscription list of a Bluetooth SIG model.
 */
class ConfigSigModelSubscriptionGet(
    override val elementAddress: UnicastAddress,
    override val modelId: SigModelId,
) : AcknowledgedConfigMessage, ConfigModelMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = ConfigSigModelSubscriptionList.opCode
    override val modelIdentifier = modelId.modelIdentifier
    override val parameters: ByteArray
        get() = elementAddress.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    /**
     * Convenience constructor to create a ConfigSigModelSubscriptionGet message.
     *
     * @param model Model to get the subscription list from.
     * @throws IllegalArgumentException If the model does not have a parent element.
     * @throws IllegalArgumentException If the model is not a Sig Model.
     */
    constructor(model: Model) : this(
        elementAddress = model.parentElement?.unicastAddress
            ?: throw IllegalArgumentException("Element address cannot be null"),
        modelId = when (model.modelId) {
            is SigModelId -> model.modelId
            else -> throw IllegalArgumentException("Only SIG models are supported")
        }
    )

    override fun toString() = "ConfigSigModelSubscriptionGet(elementAddress: " +
            "${elementAddress.toHexString()} modelId: ${modelId.toHex()})"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8029u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 4
        }?.let { params ->
            ConfigSigModelSubscriptionGet(
                elementAddress = UnicastAddress(
                    address = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                ),
                modelId = SigModelId(
                    modelIdentifier = params.getShort(2, ByteOrder.LITTLE_ENDIAN).toUShort()
                )
            )
        }
    }
}