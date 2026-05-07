@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.data.ushr
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigAnyModelMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.model.Credentials
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.PublicationAddress
import no.nordicsemi.kotlin.mesh.core.model.Publish
import no.nordicsemi.kotlin.mesh.core.model.PublishPeriod
import no.nordicsemi.kotlin.mesh.core.model.Retransmit
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.StepResolution
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import java.nio.ByteOrder
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * This message is used to set the publication state of a model.
 *
 * @property companyIdentifier     The company identifier, as registered at Bluetooth SIG.
 * @property modelIdentifier       The model identifier.
 * @property elementAddress        The target element address.
 * @property publish               Contains the publication state.
 */
class ConfigModelPublicationSet(
    override val elementAddress: UnicastAddress,
    override val companyIdentifier: UShort?,
    override val modelIdentifier: UShort,
    val publish: Publish,
) : AcknowledgedConfigMessage, ConfigAnyModelMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = ConfigModelPublicationStatus.opCode

    override val parameters: ByteArray
        get() {
            var data = elementAddress.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    publish.address.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
            data += (publish.index and 0xFFu).toByte()
            data += (publish.index.toInt() shr 8).toByte() or
                    (publish.credentials.credential shl 4).toByte()
            data += publish.ttl.toByte()
            data += (publish.period.steps and 0x3Fu).toByte() or
                    (publish.period.resolution.value.toInt() shl 6).toByte()
            data += (publish.retransmit.count.toInt() and 0x07).toByte() or
                    (publish.retransmit.steps.toInt() shl 3).toByte()
            data += companyIdentifier?.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
                ?.plus(modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN))
                ?: modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
            return data
        }

    /**
     * Convenience constructor to create the ConfigModelPublicationSet message.
     *
     * @param publish Publish state to set.
     * @param model Model to get the publication state for.
     * @throws IllegalArgumentException if the element address is not set.
     */
    @Throws(IllegalArgumentException::class)
    constructor(publish: Publish, model: Model) : this(
        elementAddress = model.parentElement?.unicastAddress
            ?: throw IllegalArgumentException("Element address cannot be null"),
        modelIdentifier = when (model.modelId) {
            is SigModelId -> model.modelId.modelIdentifier
            is VendorModelId -> model.modelId.modelIdentifier
        },
        companyIdentifier = (model.modelId as? VendorModelId)?.companyIdentifier,
        publish = publish
    )

    /**
     * Convenience constructor to create the ConfigModelPublicationSet message to remove the
     * publication.
     *
     * @param model Model from which the publication should be removed from.
     * @throws IllegalArgumentException if the element address is not set.
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
        publish = Publish()
    )

    override fun toString() = "ConfigModelPublicationSet(" +
            "elementAddress: ${elementAddress.address.toHexString(
                format = HexFormat {
                    number {
                        prefix = "0x"
                        upperCase = true
                    }
                }
            )}, " +
            "modelIdentifier: ${
                modelIdentifier.toHexString(
                    format = HexFormat {
                        number {
                            prefix = "0x"
                            upperCase = true
                        }
                    }
                )
            }, " +
            if (companyIdentifier != null) {
                "companyIdentifier: ${
                    companyIdentifier.toHexString(
                        format = HexFormat {
                            number {
                                prefix = "0x"
                                upperCase = true
                            }
                        }
                    )
                }, "
            } else { "" } +
            "publish: $publish)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode: UInt = 0x03u

        /**
         * Constructs the ConfigModelPublicationSet message using the given model.
         *
         * @param model The model to set the publication for.
         * @return A ConfigModelPublicationSet message or null if parameters are invalid.
         */
        fun init(model: Model): ConfigModelPublicationSet? = model.takeIf {
            it.parentElement?.unicastAddress != null
        }?.let {
            val modelId = model.modelId
            ConfigModelPublicationSet(
                publish = Publish(),
                companyIdentifier = when (modelId) {
                    is VendorModelId -> modelId.companyIdentifier
                    else -> null
                },
                modelIdentifier = when (modelId) {
                    is SigModelId -> modelId.modelIdentifier
                    is VendorModelId -> modelId.modelIdentifier
                },
                elementAddress = it.parentElement!!.unicastAddress
            )
        }

        /**
         * Constructs the ConfigModelPublicationSet message using the given parameters.
         *
         * @param parameters The message parameters.
         * @return A ConfigModelPublicationSet message or null if parameters are invalid.
         */
        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 11 || it.size == 13
        }?.let { params ->
            val elementAddress = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
            val address = MeshAddress.create(
                address = params.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN)
            )
            val index = params.getUShort(offset = 4, order = ByteOrder.LITTLE_ENDIAN) and 0x0FFFu
            val flag = (params[5] and 0x10).toInt() shr 4
            val ttl = params[6].toUByte()
            val periodSteps = (params[7] and 0x3F).toUByte()
            val periodResolution = StepResolution.from(value = (params[7] ushr 6).toUByte())
            val period = PublishPeriod(steps = periodSteps, resolution = periodResolution)
            val count = (params[8] and 0x07).toUByte()
            val intervalSteps = (params[8] ushr 3).toUByte()
            val retransmit = Retransmit(count = count, intervalSteps = intervalSteps)
            val publish = Publish(
                address = address as PublicationAddress,
                index = index,
                credentials = Credentials.from(credential = flag),
                ttl = ttl,
                period = period,
                retransmit = retransmit
            )
            ConfigModelPublicationSet(
                publish = publish,
                companyIdentifier = if (params.size == 13)
                    params.getUShort(offset = 9, order = ByteOrder.LITTLE_ENDIAN)
                else null,
                modelIdentifier = params.getUShort(
                    offset = if (params.size == 13) 11 else 9,
                    order = ByteOrder.LITTLE_ENDIAN
                ),
                elementAddress = UnicastAddress(address = elementAddress)
            )
        }
    }
}