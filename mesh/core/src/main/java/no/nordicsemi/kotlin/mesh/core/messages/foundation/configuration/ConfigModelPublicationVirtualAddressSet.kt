@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.getUuid
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.data.ushr
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigAnyModelMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.model.Credentials
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Publish
import no.nordicsemi.kotlin.mesh.core.model.PublishPeriod
import no.nordicsemi.kotlin.mesh.core.model.Retransmit
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.StepResolution
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import java.nio.ByteOrder
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.uuid.ExperimentalUuidApi

/**
 * This message is used to set the publication state of a model.
 *
 * @property companyIdentifier     The company identifier, as registered at Bluetooth SIG.
 * @property modelIdentifier       The model identifier.
 * @property elementAddress        The target element address.
 * @property publish               Contains the publication state with a VirtualAddress.
 */
@OptIn(ExperimentalUuidApi::class)
data class ConfigModelPublicationVirtualAddressSet(
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
                    (publish.address as VirtualAddress).uuid.toByteArray()

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
                ?: modelIdentifier.toByteArray()
            return data
        }

    /**
     * Convenience constructor to create the ConfigModelPublicationSet message.
     *
     * @param model Model to get the publication state for.
     * @throws IllegalArgumentException if the element address is not set.
     */
    @Throws(IllegalArgumentException::class)
    constructor(publish: Publish, model: Model) : this(
        publish = publish,
        elementAddress = model.parentElement?.unicastAddress
            ?: throw IllegalArgumentException("Element address cannot be null"),
        modelIdentifier = when (model.modelId) {
            is SigModelId -> model.modelId.modelIdentifier
            is VendorModelId -> model.modelId.modelIdentifier
        },
        companyIdentifier = (model.modelId as? VendorModelId)?.companyIdentifier
    )

    init {
        // Ensures that the address is a VirtualAddress
        // If not, consider sending [ConfigModelPublicationSet]
        require(publish.address is VirtualAddress) {
            "Address must be VirtualAddress or consider sending ConfigModelPublicationSet"
        }
    }

    override fun toString() = "ConfigModelPublicationVirtualAddressSet(" +
            "elementAddress: ${elementAddress.address}, " +
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
        override val opCode: UInt = 0x801Au

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 25 || it.size == 27 }
            ?.let { params ->
                val elementAddress = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                val label = VirtualAddress(uuid = params.getUuid(offset = 2))
                val index =
                    params.getUShort(offset = 18, order = ByteOrder.LITTLE_ENDIAN) and 0x0FFFu
                val flag = (params[19] and 0x10).toInt() shr 4
                val ttl = params[20].toUByte()
                val periodSteps = (params[21] and 0x3F).toUByte()
                val periodResolution = StepResolution.from(value = (params[21] ushr 6).toUByte())
                val count = (params[22] and 0x07).toUByte()
                val intervalSteps = (params[22] ushr 3).toUByte()
                val publish = Publish(
                    address = label,
                    index = index,
                    credentials = Credentials.from(credential = flag),
                    ttl = ttl,
                    period = PublishPeriod(steps = periodSteps, resolution = periodResolution),
                    retransmit = Retransmit(count = count, intervalSteps = intervalSteps)
                )
                ConfigModelPublicationVirtualAddressSet(
                    publish = publish,
                    companyIdentifier = if (params.size == 27)
                        params.getUShort(offset = 23, order = ByteOrder.LITTLE_ENDIAN)
                    else null,
                    modelIdentifier = params.getUShort(
                        offset = if (params.size == 27) 25 else 23,
                        order = ByteOrder.LITTLE_ENDIAN
                    ),
                    elementAddress = UnicastAddress(address = elementAddress)
                )
            }
    }
}