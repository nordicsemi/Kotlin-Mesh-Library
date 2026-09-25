package no.nordicsemi.android.nrfmesh.feature.nodes.node

import no.nordicsemi.android.nrfmesh.core.data.ui.ModelData
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.Features
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.Security
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Defines a data object that is used to display the ui state of the Node Info List.
 * @property name                          Name of the node.\
 * @property elements                      List of elements in the node.
 * @property companyIdentifier             Company identifier of the node.
 * @property productIdentifier             Product identifier of the node.
 * @property versionIdentifier             Version identifier of the node.
 * @property replayProtectionCount         Minimum number of replay protection list elements.
 * @property security                      Security of the node.
 * @property defaultTtl                    Time to live of the node.
 * @property features                      Features of the node.
 * @property excluded                      True if the node is excluded from the network.
 */
@OptIn(ExperimentalUuidApi::class)
data class NodeInfoListData(
    val uuid: Uuid,
    val name: String,
    val address: UnicastAddress,
    val deviceKey: String?,
    val netKeys: List<NetworkKey>,
    val appKeys: List<ApplicationKey>,
    val elements: List<ElementListData>,
    val companyIdentifier: UShort?,
    val productIdentifier: UShort?,
    val versionIdentifier: UShort?,
    val replayProtectionCount: UShort?,
    val security: Security,
    val defaultTtl: UByte?,
    val features: Features,
    val excluded: Boolean,
) {
    constructor(node: Node) : this(
        uuid = node.uuid,
        name = node.name,
        address = node.primaryUnicastAddress,
        deviceKey = node.deviceKey?.toHexString(format = HexFormat.UpperCase),
        netKeys = node.networkKeys.toList(),
        appKeys = node.applicationKeys.toList(),
        elements = node.elements.map { ElementListData(element = it) },
        companyIdentifier = node.companyIdentifier,
        productIdentifier = node.productIdentifier,
        versionIdentifier = node.versionIdentifier,
        replayProtectionCount = node.replayProtectionCount,
        security = node.security,
        defaultTtl = node.defaultTTL,
        features = node.features,
        excluded = node.excluded
    )
}

/**
 * Defines a data object that is used to display the ui state of the Element List.
 *
 * @property name                Name of the element.
 * @property unicastAddress      Unicast address of the element.
 * @property models              Models in the element.
 */
data class ElementListData(
    val name: String?,
    val index: Int,
    val unicastAddress: UnicastAddress,
    val models: List<ModelData>,
) {
    constructor(element: Element) : this(
        name = element.name,
        index = element.index,
        unicastAddress = element.unicastAddress,
        models = element.models.map { ModelData(it) },
    )
}