package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.shr
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.data.ushr
import no.nordicsemi.kotlin.mesh.core.messages.ConfigAnyModelMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
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
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import java.nio.ByteOrder
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * This message is used to set the publication state of a model.
 *
 * @property status                Status of the request.
 * @property companyIdentifier     The company identifier, as registered at Bluetooth SIG.
 * @property modelIdentifier       The model identifier.
 * @property elementAddress        The target element address.
 * @property publish               Contains the publication state.
 */
class ConfigModelPublicationStatus(
    override val status: ConfigMessageStatus = ConfigMessageStatus.SUCCESS,
    override val elementAddress: UnicastAddress,
    override val modelIdentifier: UShort,
    override val companyIdentifier: UShort?,
    val publish: Publish,
) : ConfigResponse, ConfigStatusMessage, ConfigAnyModelMessage {
    override val opCode: UInt = Initializer.opCode

    override val parameters: ByteArray
        get() {
            var data = status.value.toByteArray() +
                    elementAddress.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    publish.address.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
            data += (publish.index and 0xFFu).toByte()
            data += (publish.index.toInt() shr 8).toByte() or
                    (publish.credentials.credential shl 4).toByte()
            data += publish.ttl.toByte()
            data += (publish.period.steps and 0x3Fu).toByte() or
                    (publish.period.resolution.value.toInt() shl 6).toByte()
            data += (publish.retransmit.count.toInt() shl 3).toByte() or
                    (publish.retransmit.steps.toInt() shl 3).toByte()
            data += companyIdentifier?.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
                ?.plus(modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN))
                ?: modelIdentifier.toByteArray(ByteOrder.LITTLE_ENDIAN)
            return data
        }

    /**
     * Convenience constructor to create the ConfigModelPublicationStatus message that reports that
     * there is no publication.
     *
     * @param request The [ConfigAnyModelMessage] message that this is a response to.
     */
    @Suppress("unused")
    constructor(request: ConfigAnyModelMessage) : this(request = request, publish = null)

    /**
     * Convenience constructor to create the ConfigModelPublicationStatus message.
     *
     * @param request [ConfigAnyModelMessage] message that this is a response to.
     * @param publish Publication state. Setting a null value will clear the publication state.
     */
    constructor(request: ConfigAnyModelMessage, publish: Publish?) : this(
        status = ConfigMessageStatus.SUCCESS,
        elementAddress = request.elementAddress,
        modelIdentifier = request.modelIdentifier,
        companyIdentifier = request.companyIdentifier,
        publish = publish ?: Publish()
    )

    /**
     * Constructs the ConfigModelPublicationStatus message using the given parameters.
     *
     * @param request [ConfigAnyModelMessage] message that this is a response to.
     * @param status  Status of the request.
     */
    constructor(request: ConfigAnyModelMessage, status: ConfigMessageStatus) : this(
        status = status,
        elementAddress = request.elementAddress,
        modelIdentifier = request.modelIdentifier,
        companyIdentifier = request.companyIdentifier,
        publish = Publish()
    )

    /**
     * Convenience constructor to create the ConfigModelPublicationStatus message.
     *
     * @param request ConfigModelPublicationSet is the given request from the remote node.
     */
    constructor(request: ConfigModelPublicationSet) : this(
        request = request,
        publish = request.publish
    )

    /**
     * Convenience constructor to create the ConfigModelPublicationStatus message.
     *
     * @param request ConfigModelPublicationVirtualAddressSet is the given request from the remote
     *                node.
     */
    constructor(request: ConfigModelPublicationVirtualAddressSet) : this(
        request = request,
        publish = request.publish
    )

    override fun toString() = "ConfigModelPublicationStatus(" +
            "status: $status, " +
            "elementAddress: ${elementAddress.toHexString()}, " +
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
        override val opCode: UInt = 0x8019u
        fun init(publish: Publish, model: Model): ConfigModelPublicationStatus? {
            require(publish.address !is VirtualAddress) { return null }
            val elementAddress = model.parentElement?.unicastAddress ?: return null
            val modelId = model.modelId
            return ConfigModelPublicationStatus(
                publish = publish,
                companyIdentifier = when (modelId) {
                    is VendorModelId -> modelId.companyIdentifier
                    else -> null
                },
                modelIdentifier = when (modelId) {
                    is SigModelId -> modelId.modelIdentifier
                    is VendorModelId -> modelId.modelIdentifier
                },
                elementAddress = elementAddress
            )
        }

        /**
         * Constructs the ConfigModelPublicationSet message using the given model.
         *
         * @param model The model to set the publication for.
         * @return A ConfigModelPublicationSet message or null if parameters are invalid.
         */
        fun init(model: Model): ConfigModelPublicationStatus? = model.takeIf {
            it.parentElement?.unicastAddress != null
        }?.let {
            val modelId = model.modelId
            ConfigModelPublicationStatus(
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
            it.size == 12 || it.size == 14
        }?.let { params ->
            ConfigMessageStatus.from(value = params[0].toUByte())
                ?.let { status ->
                    val elementAddress = params
                        .getUShort(offset = 1, order = ByteOrder.LITTLE_ENDIAN)
                    val address = MeshAddress
                        .create(
                            address = params
                                .getUShort(offset = 3, order = ByteOrder.LITTLE_ENDIAN)
                        )
                    val index = params
                        .getUShort(offset = 5, order = ByteOrder.LITTLE_ENDIAN) and 0x0FFFu
                    val flag = (params[6] and 0x10) shr 4
                    val ttl = params[7].toUByte()
                    val periodSteps = params[8].toUByte() and 0x3Fu
                    val periodResolution = StepResolution.from(value = (params[8] ushr 6).toUByte())
                    val period = PublishPeriod(steps = periodSteps, resolution = periodResolution)
                    val count = params[9].toUByte() and 0x07u
                    val intervalSteps = (params[9] ushr 3).toUByte()
                    val retransmit = Retransmit(count = count, intervalSteps = intervalSteps)
                    val publish = Publish(
                        address = address as PublicationAddress,
                        index = index,
                        credentials = Credentials.from(credential = flag.toInt()),
                        ttl = ttl,
                        period = period,
                        retransmit = retransmit
                    )
                    ConfigModelPublicationStatus(
                        status = status,
                        publish = publish,
                        companyIdentifier = if (params.size == 14) {
                            params.getUShort(offset = 10, order = ByteOrder.LITTLE_ENDIAN)
                        } else null,
                        modelIdentifier = params
                            .getUShort(
                                offset = if (params.size == 14) 12 else 10,
                                order = ByteOrder.LITTLE_ENDIAN
                            ),
                        elementAddress = UnicastAddress(address = elementAddress)
                    )
                }
        }
    }
}