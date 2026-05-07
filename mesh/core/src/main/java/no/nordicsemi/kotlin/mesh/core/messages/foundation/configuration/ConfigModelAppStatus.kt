@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.BaseMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigAnyAppKeyModelMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigAnyModelMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigAppKeyMessage.Companion.decodeAppKeyIndex
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.ModelId
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import java.nio.ByteOrder

/**
 * Status declaring if the [ConfigModelAppStatus] operation succeeded or not.
 *
 * @constructor Constructs the ConfigAppKeyStatus message.
 */
class ConfigModelAppStatus(
    override val status: ConfigMessageStatus,
    override val applicationKeyIndex: KeyIndex,
    override val elementAddress: UnicastAddress,
    override val modelId: ModelId,
) : ConfigResponse, ConfigStatusMessage, ConfigAnyAppKeyModelMessage, ConfigAnyModelMessage {
    override val opCode = Initializer.opCode

    override val parameters: ByteArray
        get() {
            var data = status.value.toByteArray() +
                    elementAddress.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    encodeAppKeyIndex(applicationKeyIndex = applicationKeyIndex)
            data += companyIdentifier?.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
                ?.plus(modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN))
                ?: modelIdentifier.toByteArray(ByteOrder.LITTLE_ENDIAN)
            return data
        }

    override val modelIdentifier: UShort = when {
        modelId.isBluetoothSigAssigned -> (modelId as SigModelId).modelIdentifier
        else -> (modelId as VendorModelId).modelIdentifier
    }

    override val companyIdentifier: UShort? = (modelId as? VendorModelId)?.companyIdentifier

    /**
     * Constructs the ConfigAppBindStatus message.
     *
     * @param request [ConfigAnyAppKeyModelMessage] operation that was sent to the mesh node.
     */
    constructor(request: ConfigAnyAppKeyModelMessage) : this(
        request = request,
        status = ConfigMessageStatus.SUCCESS
    )

    /**
     * Constructs the ConfigAppBindStatus message with the given request and the status
     *
     * @param request [ConfigAnyAppKeyModelMessage] operation that was sent to the mesh node.
     * @param status  [ConfigMessageStatus] for a given [request].
     */
    constructor(request: ConfigAnyAppKeyModelMessage, status: ConfigMessageStatus) : this(
        status = status,
        applicationKeyIndex = request.applicationKeyIndex,
        elementAddress = request.elementAddress,
        modelId = request.modelId
    )

    override fun toString() = "ConfigModelAppStatus(status: ${status}, " +
            "applicationKeyIndex: $applicationKeyIndex, " +
            "elementAddress: ${elementAddress.address.toHexString(
                format = HexFormat { 
                    number.prefix = "0x"
                    upperCase = true
                }
            )}, " +
            "modelId: $modelId)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x803Eu

        /**
         * Initializes the ConfigAppKeyStatus message.
         *
         * @param parameters Message parameters.
         * @return ConfigAppKeyStatus or null if the parameters are invalid.
         */
        override fun init(parameters: ByteArray?): BaseMeshMessage? = parameters
            ?.takeIf { it.size == 7 || it.size == 9 }
            ?.let { params ->
                val status = ConfigMessageStatus
                    .from(value = params.first().toUByte()) ?: return null
                ConfigModelAppStatus(
                    status = status,
                    elementAddress = UnicastAddress(
                        address = params.getUShort(
                            offset = 1,
                            order = ByteOrder.LITTLE_ENDIAN
                        )
                    ),
                    applicationKeyIndex = decodeAppKeyIndex(data = params, offset = 3),
                    modelId = when (params.size) {
                        9 -> VendorModelId(
                            companyIdentifier = params.getUShort(
                                offset = 5,
                                order = ByteOrder.LITTLE_ENDIAN
                            ),
                            modelIdentifier = params.getUShort(
                                offset = 7,
                                order = ByteOrder.LITTLE_ENDIAN
                            )
                        )

                        else -> SigModelId(
                            modelIdentifier = params.getUShort(
                                offset = 5,
                                order = ByteOrder.LITTLE_ENDIAN
                            )
                        )
                    }
                )
            }
    }
}