@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigAnyAppKeyModelMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigAnyModelMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigAppKeyMessage.Companion.decodeAppKeyIndex
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.ModelId
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import java.nio.ByteOrder

/**
 * This message is used to unbind an Application Key to a model.
 *
 * @constructor Constructs the ConfigAppKeyAdd message.
 */
class ConfigModelAppUnbind(
    override val applicationKeyIndex: KeyIndex,
    override val elementAddress: UnicastAddress,
    override val modelId: ModelId,
) : AcknowledgedConfigMessage, ConfigAnyAppKeyModelMessage, ConfigAnyModelMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = ConfigModelAppStatus.opCode

    override val parameters : ByteArray
        get() {
            var data = elementAddress.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    encodeAppKeyIndex(applicationKeyIndex = applicationKeyIndex)
            data += companyIdentifier?.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
                ?.plus(modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN))
                ?: modelIdentifier.toByteArray(ByteOrder.LITTLE_ENDIAN)
            return data
        }

    override val modelIdentifier: UShort = when(modelId is SigModelId) {
        true -> modelId.modelIdentifier
        false -> (modelId as VendorModelId).modelIdentifier
    }

    override val companyIdentifier: UShort? = (modelId as? VendorModelId)?.companyIdentifier

    /**
     * Convenience constructor to create a [ConfigModelAppUnbind] message.
     *
     * @param model           Model to unbind the application key from.
     * @param applicationKey  Application key to be added.
     * @constructor Constructs the ConfigModelAppUnbind message.
     */
    constructor(model: Model, applicationKey: ApplicationKey) : this(
        applicationKeyIndex = applicationKey.index,
        elementAddress = model.parentElement?.unicastAddress
            ?: throw IllegalArgumentException("Parent element address is null"),
        modelId = model.modelId
    )

    override fun toString() = "ConfigModelAppUnbind(applicationKeyIndex: $applicationKeyIndex, " +
            "elementAddress: ${elementAddress.address.toHexString(
                format = HexFormat {
                    number.prefix = "0x"
                    upperCase = true
                }
            )} " +
            "modelIdentifier: ${modelIdentifier.toHexString(
                format = HexFormat {
                    number.prefix = "0x"
                    upperCase = true
                }
            )}), " +
            "optional companyIdentifier: ${companyIdentifier?.toHexString(
                format = HexFormat {
                    number.prefix = "0x"
                    upperCase = true
                }
            )})"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x803Fu

        /**
         * Initializes the [ConfigModelAppUnbind] based on the given parameters.
         *
         * @param parameters Message parameters.
         * @return [ConfigModelAppUnbind] or null if the parameters are invalid.
         */
        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 6 || it.size == 8
        }?.let {
            val elementAddress = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
            val appKeyIndex = decodeAppKeyIndex(data = it, offset = 2)
            val modelId = when (it.size == 8) {
                true -> VendorModelId(
                    companyIdentifier = it.getUShort(
                        offset = 4,
                        order = ByteOrder.LITTLE_ENDIAN
                    ),
                    modelIdentifier = it.getUShort(
                        offset = 6,
                        order = ByteOrder.LITTLE_ENDIAN
                    )
                )
                else -> SigModelId(
                    modelIdentifier = it.getUShort(
                        offset = 4,
                        order = ByteOrder.LITTLE_ENDIAN
                    )
                )
            }
            ConfigModelAppUnbind(
                applicationKeyIndex = appKeyIndex,
                elementAddress = UnicastAddress(elementAddress),
                modelId = modelId
            )
        }
    }
}