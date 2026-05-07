package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.ConfigModelSubscriptionList
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import java.nio.ByteOrder

/**
 * This message is the status message to the [ConfigSigModelSubscriptionGet] message. This lists
 * the subscription list of a Bluetooth SIG model.
 */
class ConfigSigModelSubscriptionList(
    override val status: ConfigMessageStatus,
    override val elementAddress: UnicastAddress,
    override val modelId: SigModelId,
    override val addresses: List<Address>,
) : ConfigResponse, ConfigStatusMessage, ConfigModelSubscriptionList {
    override val opCode = Initializer.opCode
    override val modelIdentifier = modelId.modelIdentifier
    override val parameters: ByteArray
        get() = status.value.toByteArray() +
                elementAddress.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                addresses.fold(byteArrayOf()) { acc, address ->
                    acc + address.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
                }

    /**
     * Convenience constructor to create a ConfigSigModelSubscriptionList message.
     *
     * @param request   [ConfigSigModelSubscriptionGet] message that this is a response to.
     * @param addresses List of addresses of the subscription.
     */
    constructor(request: ConfigSigModelSubscriptionGet, addresses: List<Address>) : this(
        status = ConfigMessageStatus.SUCCESS,
        elementAddress = request.elementAddress,
        modelId = request.modelId,
        addresses = addresses
    )

    /**
     * Convenience constructor to create a ConfigSigModelSubscriptionList message.
     *
     * @param request The [ConfigSigModelSubscriptionGet] message that this is a response to.
     * @param status The status of the message.
     */
    constructor(request: ConfigSigModelSubscriptionGet, status: ConfigMessageStatus) : this(
        status = status,
        elementAddress = request.elementAddress,
        modelId = request.modelId,
        addresses = emptyList()
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "ConfigSigModelSubscriptionList(status: $status, " +
            "elementAddress: $elementAddress, modelId: $modelId, " +
            "addresses: [${addresses.joinToString { 
                it.toHexString(
                    format = HexFormat {
                        number { 
                            prefix = "0x"
                            upperCase = true
                        }
                    }
                )
            }}])"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x802Au

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size >= 5
        }?.let { params ->
            ConfigMessageStatus.from(params[0].toUByte())?.let { status ->
                ConfigSigModelSubscriptionList(
                    status = status,
                    elementAddress = UnicastAddress(
                        address = params.getUShort(offset = 1, order = ByteOrder.LITTLE_ENDIAN)
                    ),
                    modelId = SigModelId(
                        modelIdentifier = params.getUShort(3, ByteOrder.LITTLE_ENDIAN)
                    ),
                    addresses = mutableListOf<Address>().apply {
                        var index = 5
                        while (index < params.size) {
                            add(params.getUShort(offset = index, order = ByteOrder.LITTLE_ENDIAN))
                            index += 2
                        }
                    }
                )
            }
        }
    }
}