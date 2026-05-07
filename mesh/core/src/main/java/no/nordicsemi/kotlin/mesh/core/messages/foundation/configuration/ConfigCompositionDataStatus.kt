@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.Features
import no.nordicsemi.kotlin.mesh.core.model.Location
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import no.nordicsemi.kotlin.mesh.core.model.composition
import java.nio.ByteOrder

/**
 * Base interface for a Composition Data Page.
 *
 * Composition Data state contains information about the composition of a give Node, the Elements it
 * includes and the models that are supported by each element.
 *
 * @property page Page number of the Composition Data.
 * @property parameters The parameters of the page.
 */
sealed interface CompositionDataPage {
    val page: UByte
    val parameters: ByteArray?
}

/**
 * This message is the response received when requesting the composition data of a Node using
 * [ConfigCompositionDataGet] message.
 *
 * @property page Page containing the composition of a node.
 * @constructor Creates a ConfigCompositionDataStatus message.
 */
class ConfigCompositionDataStatus(val page: CompositionDataPage) : ConfigResponse {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray? = page.parameters

    override fun toString() = "ConfigCompositionDataStatus(page: $page)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x02u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.isNotEmpty()
        }?.let {
            if (it[0] == 0.toByte())
                Page0.init(it)?.let { page0 ->
                    ConfigCompositionDataStatus(page = page0)
                }
            else null
        }
    }
}

/**
 * Composition Data Page 0 shall be present on a Node.
 *
 * Composition Data Page 0 shall not change during a term of Node in the network.
 *
 * @property page                                    Page number of the Composition Data.
 * @property companyIdentifier                       16-bit Company Identifier (CID) assigned by
 *                                                   Bluetooth SIG. CIDs can be found on
 *                                                   [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/company-identifiers/)
 * @property productIdentifier                       16-bit vendor-assigned Product Identifier (PID).
 * @property versionIdentifier                       16-bit vendor-assigned Version Identifier (VID).
 * @property minimumNumberOfReplayProtectionList     Minimum number of entries in the Replay
 *                                                   Protection List for a given node.
 * @property features                                Features supported by the node. Page 0 of the
 *                                                   Composition Data does not provide information
 *                                                   whether a feature is enabled or disabled, just
 *                                                   whether it is supported or not. Read the state
 *                                                   of each feature using corresponding Config
 *                                                   message.
 * @property elements                                List of elements that are present on the node.
 * @constructor Creates a Page 0 of the Composition Data.
 */
data class Page0(
    override val page: UByte,
    val companyIdentifier: UShort,
    val productIdentifier: UShort,
    val versionIdentifier: UShort,
    val minimumNumberOfReplayProtectionList: UShort,
    val features: Features,
    val elements: List<Element>,
) : CompositionDataPage {

    override val parameters: ByteArray = byteArrayOf(page.toByte()) +
            companyIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            productIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            versionIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            minimumNumberOfReplayProtectionList.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            features.rawValue.toByteArray(ByteOrder.LITTLE_ENDIAN) +
            elements.composition()

    /**
     * Constructs the parameters of the Composition Data Page 0.
     *
     * @param node Node to get the composition data from.
     */
    constructor(node: Node) : this(
        page = 0u,
        companyIdentifier = node.companyIdentifier ?: 0u,
        productIdentifier = node.productIdentifier ?: 0u,
        versionIdentifier = node.versionIdentifier ?: 0u,
        minimumNumberOfReplayProtectionList = node.replayProtectionCount ?: 0u,
        features = node.features,
        elements = node.elements
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        return "Page0(" +
                "page: $page, " +
                "cid: ${
                    companyIdentifier.toHexString(
                        format = HexFormat {
                            number {
                                prefix = "0x"
                                minLength = 4
                                upperCase = true
                            }
                        }
                    )
                }, " +
                "pid: ${
                    productIdentifier.toHexString(
                        format = HexFormat {
                            number {
                                prefix = "0x"
                                minLength = 4
                                upperCase = true
                            }
                        }
                    )
                }, " +
                "vid: ${
                    versionIdentifier.toHexString(
                        format = HexFormat {
                            number {
                                prefix = "0x"
                                minLength = 4
                                upperCase = true
                            }
                        }
                    )
                }, " +
                "crpl: $minimumNumberOfReplayProtectionList, " +
                "features: [$features], " +
                "elements: [${elements.map {  element ->
                    "Element(location: ${element.location}, models: ${element.models.joinToString { it.modelId.toString() }})"
                }}])"
    }

    companion object {

        /**
         * Constructs the Composition Data Page 0 from the given parameters.
         *
         * @param parameters The parameters of the page.
         * @return The Composition Data Page 0 or null otherwise.
         */
        fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size >= 11 && it[0].toUByte().toInt() == 0
        }?.let { compositionData ->
            val page = 0
            val companyIdentifier = compositionData.getUShort(
                offset = 1,
                order = ByteOrder.LITTLE_ENDIAN
            )
            val productIdentifier = compositionData.getUShort(
                offset = 3,
                order = ByteOrder.LITTLE_ENDIAN
            )
            val versionIdentifier = compositionData.getUShort(
                offset = 5,
                order = ByteOrder.LITTLE_ENDIAN
            )
            val minimumNumberOfReplayProtectionList = compositionData.getUShort(
                offset = 7,
                order = ByteOrder.LITTLE_ENDIAN
            )
            val features = Features.init(
                mask = compositionData.getUShort(
                    offset = 9,
                    order = ByteOrder.LITTLE_ENDIAN
                )
            )
            val elements = mutableListOf<Element>()
            var offset = 11
            while (offset < compositionData.size) {
                require(compositionData.size >= offset + 4) {
                    return null
                }
                val rawValue = compositionData.getUShort(
                    offset = offset,
                    order = ByteOrder.LITTLE_ENDIAN
                )
                val location = Location.from(value = rawValue)
                val sigModelsByteCount = compositionData.getInt(
                    offset = offset + 2,
                    format = IntFormat.UINT8
                ) * 2
                val vendorModelsByteCount = compositionData.getInt(
                    offset = offset + 3,
                    format = IntFormat.UINT8
                ) * 4

                require(compositionData.size >= (offset + 3 + sigModelsByteCount + vendorModelsByteCount)) {
                    return null
                }

                // 4 bytes have been read.
                offset += 4

                // Set temporary index.
                // Final index will be set when Element is added to the Node.
                val index = 0

                // Read models.
                val element = Element(_name = "Element ${elements.size + 1}", location = location)
                    .apply { this.index = index }

                for (i in offset until offset + sigModelsByteCount step 2) {
                    val sigModelId = compositionData.getUShort(
                        offset = i,
                        order = ByteOrder.LITTLE_ENDIAN
                    )
                    element.add(model = Model(modelId = SigModelId(sigModelId)))
                }
                offset += sigModelsByteCount

                for (i in offset until offset + vendorModelsByteCount step 4) {
                    element.add(
                        Model(
                            modelId = VendorModelId(
                                companyIdentifier = compositionData.getUShort(
                                    offset = i,
                                    order = ByteOrder.LITTLE_ENDIAN
                                ),
                                modelIdentifier = compositionData.getUShort(
                                    offset = i + 2,
                                    order = ByteOrder.LITTLE_ENDIAN
                                )
                            )
                        )
                    )
                }
                offset += vendorModelsByteCount
                elements.add(element = element)
            }
            Page0(
                page = page.toUByte(),
                companyIdentifier = companyIdentifier,
                productIdentifier = productIdentifier,
                versionIdentifier = versionIdentifier,
                minimumNumberOfReplayProtectionList = minimumNumberOfReplayProtectionList,
                features = features,
                elements = elements
            )
        }
    }
}