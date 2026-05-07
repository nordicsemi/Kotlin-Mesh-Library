package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessage.ConfigMessageUtils.decode
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessage.ConfigMessageUtils.encode
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.ConfigModelAppList
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import java.nio.ByteOrder

/**
 * This message is the status message to the [ConfigSigModelAppGet] message. This lists the
 * Application Keys bound to a Bluetooth SIG model.
 */
class ConfigSigModelAppList(
    override val status: ConfigMessageStatus,
    override val modelId: SigModelId,
    override val elementAddress: UnicastAddress,
    override val applicationKeyIndexes: List<KeyIndex>,
) : ConfigResponse, ConfigStatusMessage, ConfigModelAppList {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray
        get() = byteArrayOf(status.value.toByte()) +
                elementAddress.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                modelId.modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                encode(indexes = applicationKeyIndexes)
    override val modelIdentifier: UShort = modelId.modelIdentifier

    /**
     * Convenience constructor to create the ConfigSigModelAppList message.
     *
     * @param request [ConfigSigModelAppGet] message that this is a response to.
     * @param keys    List of application keys bound to the model.
     */
    @Suppress("unused")
    constructor(request: ConfigSigModelAppGet, keys: List<ApplicationKey>) : this(
        status = ConfigMessageStatus.SUCCESS,
        modelId = request.modelId,
        elementAddress = request.elementAddress,
        applicationKeyIndexes = keys.map { it.index }
    )

    /**
     * Convenience constructor to create the ConfigSigModelAppList message.
     *
     * @param request [ConfigSigModelAppGet] message that this is a response to.
     * @param status  Status of the request.
     */
    @Suppress("unused")
    constructor(request: ConfigSigModelAppGet, status: ConfigMessageStatus) : this(
        status = status,
        modelId = request.modelId,
        elementAddress = request.elementAddress,
        applicationKeyIndexes = listOf()
    )

    override fun toString() = "ConfigSigModelAppList(status: $status, " +
            "elementAddress: $elementAddress, modelId: $modelId, " +
            "applicationKeyIndexes: $applicationKeyIndexes)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode: UInt = 0x804Cu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size >= 5 }
            ?.let { params ->
                ConfigMessageStatus.from(params[0].toUByte())?.let { status ->
                    ConfigSigModelAppList(
                        status = status,
                        elementAddress = UnicastAddress(
                            address = params.getUShort(
                                offset = 1,
                                ByteOrder.LITTLE_ENDIAN
                            )
                        ),
                        modelId = SigModelId(
                            modelIdentifier = params.getUShort(
                                offset = 3,
                                order = ByteOrder.LITTLE_ENDIAN
                            )
                        ),
                        applicationKeyIndexes = decode(
                            data = params,
                            offset = 5
                        ).toList()
                    )
                }
            }
    }
}