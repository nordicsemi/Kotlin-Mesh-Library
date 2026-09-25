@file:Suppress("MemberVisibilityCanBePrivate", "unused", "PropertyName")

package no.nordicsemi.android.nrfmesh.core.data.ui

import no.nordicsemi.kotlin.mesh.core.model.Features
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatPublication
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatSubscription
import no.nordicsemi.kotlin.mesh.core.model.NetworkTransmit
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.NodeKey
import no.nordicsemi.kotlin.mesh.core.model.RelayRetransmit
import no.nordicsemi.kotlin.mesh.core.model.Security
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastRange
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The node represents a configured state of a mesh node.
 *
 * @property uuid                       Unique 128-bit UUID of the node.
 * @property deviceKey                  128-bit device key. When importing a partially exported
 *                                      network configuration, the device key might not be present
 *                                      in the Mesh Network Configuration Database.
 * @property security                   Represents the level of [Security] for the subnet on which
 *                                      the node has been originally provisioned.
 * @property netKeys                    Array of [NodeKey] that includes information about the
 *                                      network keys known to this node.
 * @property configComplete             True if the Mesh Manager determines that this node’s
 *                                      configuration process is completed; otherwise,
 *                                      the property’s value is set to false.
 * @property name                       Human-readable name that can identify this node within the
 *                                      mesh network.
 * @property companyIdentifier          16-bit Company Identifier (CID) assigned by the Bluetooth
 *                                      SIG. The value of this property is obtained from node
 *                                      composition data.
 * @property productIdentifier          16-bit, vendor-assigned Product Identifier (PID). The value
 *                                      of this property is obtained from node composition data.
 * @property versionIdentifier          16-bit, vendor-assigned product Version Identifier (VID).
 *                                      The value of this property is obtained from node composition
 *                                      data
 * @property replayProtectionCount      16-bit value indicating the minimum number of Replay
 *                                      Protection List (RPL) entries for this node. The value of
 *                                      this property is obtained from node composition data. RPL
 *                                      implementation handles a multi-segment message transaction
 *                                      which is under a replay attack. The sequence number of the
 *                                      last segment that has been received for this message is
 *                                      stored for that peer node in the replay protection list.
 * @property features                   [Features] supported by the node.
 * @property secureNetworkBeacon        Represents whether the node is configured to send Secure
 *                                      Network beacons.
 * @property defaultTTL                 0 to 127 that represents the default Time to Live (TTL)
 *                                      value used when sending messages.
 * @property networkTransmit            [NetworkTransmit] represents the parameters of the
 *                                      transmissions of network layer messages originating from a
 *                                      mesh node.
 * @property relayRetransmit            [RelayRetransmit] represents the parameters of the
 *                                      retransmissions of network layer messages relayed by a mesh
 *                                      node.
 * @property appKeys                    Array of [NodeKey] that includes information about the
 *                                      [ApplicationKey]s known to this node.
 * @property elements                   Array of elements contained in the Node.
 * @property excluded                   True if the node is in the process of being deleted and is
 *                                      excluded from the new network key distribution during the
 *                                      Key Refresh procedure; otherwise, it is set to “false”.
 * @property elementsCount              Number of elements belonging to this node.
 * @property addresses                  List of addresses used by this node.
 * @property unicastRange               Address range used by this node.
 * @property lastUnicastAddress         Address of the last element in the node.
 * @property primaryUnicastAddress      Address of the primary element in the node.
 * @property configComplete             True if the node is configured.
 * @property networkKeys                List of network keys known to this node.
 * @property applicationKeys            List of application keys known to this node.
 * @property primaryElementData         The primary element of the node.
 * @property provisioner                The provisioner that provisioned this node.
 * @property isProvisioner              True if the node is a provisioner.
 * @property isLocalProvisioner         True if the node is the local provisioner.
 * @property provisioner                The provisioner that provisioned this node.
 * @property isCompositionDataReceived  True if the node has received composition data.
 *
 * @constructor                         Creates a mesh node.
 */
@ConsistentCopyVisibility
@OptIn(ExperimentalUuidApi::class)
data class NodeData internal constructor(
    val uuid: Uuid,
    val name: String,
    val deviceKey: ByteArray?,
    val netKeys: List<NodeKey>,
    val appKeys: List<NodeKey>,
    val elements: List<ElementData>,
    val primaryUnicastAddress: UnicastAddress,
    val security: Security,
    val configComplete: Boolean,
    val networkKeys: List<NetworkKeyData>,
    val applicationKeys: List<ApplicationKeyData>,
    val companyIdentifier: UShort?,
    val productIdentifier: UShort?,
    val versionIdentifier: UShort?,
    val replayProtectionCount: UShort?,
    val features: Features,
    val secureNetworkBeacon: Boolean?,
    val networkTransmit: NetworkTransmit?,
    val relayRetransmit: RelayRetransmit?,
    val defaultTTL: UByte?,
    val excluded: Boolean,
    val heartbeatPublication: HeartbeatPublication?,
    val heartbeatSubscription: HeartbeatSubscription?,
    val primaryElementData: ElementData?,
    val elementsCount: Int,
    val addresses: List<UnicastAddress>,
    val unicastRange: UnicastRange,
    val lastUnicastAddress: UnicastAddress,
    val isCompositionDataReceived: Boolean,
    val isProvisioner: Boolean,
    val isLocalProvisioner: Boolean,
    val provisioner: ProvisionerData?,
) {
    constructor(node: Node) : this(
        uuid = node.uuid,
        name = node.name,
        deviceKey = node.deviceKey,
        netKeys = node.netKeys,
        appKeys = node.appKeys,
        elements = node.elements.map { ElementData(element = it) },
        primaryUnicastAddress = node.primaryUnicastAddress,
        security = node.security,
        configComplete = node.configComplete,
        networkKeys = node.networkKeys.map { NetworkKeyData(key = it) },
        applicationKeys = node.applicationKeys.map { ApplicationKeyData(key = it) },
        companyIdentifier = node.companyIdentifier,
        productIdentifier = node.productIdentifier,
        versionIdentifier = node.versionIdentifier,
        replayProtectionCount = node.replayProtectionCount,
        features = node.features,
        secureNetworkBeacon = node.secureNetworkBeacon,
        networkTransmit = node.networkTransmit,
        relayRetransmit = node.relayRetransmit,
        defaultTTL = node.defaultTTL,
        excluded = node.excluded,
        heartbeatPublication = node.heartbeatPublication,
        heartbeatSubscription = node.heartbeatSubscription,
        primaryElementData = ElementData(element = node.primaryElement),
        elementsCount = node.elementsCount,
        addresses = node.addresses,
        unicastRange = node.unicastRange,
        lastUnicastAddress = node.lastUnicastAddress,
        isCompositionDataReceived = node.isCompositionDataReceived,
        isProvisioner = node.isProvisioner,
        isLocalProvisioner = node.isLocalProvisioner,
        provisioner = node.provisioner?.let { ProvisionerData(provisioner = it) }
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NodeData

        if (uuid != other.uuid) return false
        if (name != other.name) return false
        if (deviceKey != null) {
            if (other.deviceKey == null) return false
            if (!deviceKey.contentEquals(other.deviceKey)) return false
        } else if (other.deviceKey != null) return false
        if (netKeys != other.netKeys) return false
        if (appKeys != other.appKeys) return false
        if (elements != other.elements) return false
        if (primaryUnicastAddress != other.primaryUnicastAddress) return false
        if (security != other.security) return false
        if (configComplete != other.configComplete) return false
        if (networkKeys != other.networkKeys) return false
        if (applicationKeys != other.applicationKeys) return false
        if (companyIdentifier != other.companyIdentifier) return false
        if (productIdentifier != other.productIdentifier) return false
        if (versionIdentifier != other.versionIdentifier) return false
        if (replayProtectionCount != other.replayProtectionCount) return false
        if (features != other.features) return false
        if (secureNetworkBeacon != other.secureNetworkBeacon) return false
        if (networkTransmit != other.networkTransmit) return false
        if (relayRetransmit != other.relayRetransmit) return false
        if (defaultTTL != other.defaultTTL) return false
        if (excluded != other.excluded) return false
        if (heartbeatPublication != other.heartbeatPublication) return false
        if (heartbeatSubscription != other.heartbeatSubscription) return false
        if (primaryElementData != other.primaryElementData) return false
        if (elementsCount != other.elementsCount) return false
        if (addresses != other.addresses) return false
        if (unicastRange != other.unicastRange) return false
        if (lastUnicastAddress != other.lastUnicastAddress) return false
        if (isCompositionDataReceived != other.isCompositionDataReceived) return false
        if (isProvisioner != other.isProvisioner) return false
        if (isLocalProvisioner != other.isLocalProvisioner) return false
        if (provisioner != other.provisioner) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (deviceKey?.contentHashCode() ?: 0)
        result = 31 * result + netKeys.hashCode()
        result = 31 * result + appKeys.hashCode()
        result = 31 * result + elements.hashCode()
        result = 31 * result + primaryUnicastAddress.hashCode()
        result = 31 * result + security.hashCode()
        result = 31 * result + configComplete.hashCode()
        result = 31 * result + networkKeys.hashCode()
        result = 31 * result + applicationKeys.hashCode()
        result = 31 * result + (companyIdentifier?.hashCode() ?: 0)
        result = 31 * result + (productIdentifier?.hashCode() ?: 0)
        result = 31 * result + (versionIdentifier?.hashCode() ?: 0)
        result = 31 * result + (replayProtectionCount?.hashCode() ?: 0)
        result = 31 * result + features.hashCode()
        result = 31 * result + (secureNetworkBeacon?.hashCode() ?: 0)
        result = 31 * result + (networkTransmit?.hashCode() ?: 0)
        result = 31 * result + (relayRetransmit?.hashCode() ?: 0)
        result = 31 * result + (defaultTTL?.hashCode() ?: 0)
        result = 31 * result + excluded.hashCode()
        result = 31 * result + (heartbeatPublication?.hashCode() ?: 0)
        result = 31 * result + (heartbeatSubscription?.hashCode() ?: 0)
        result = 31 * result + (primaryElementData?.hashCode() ?: 0)
        result = 31 * result + elementsCount
        result = 31 * result + addresses.hashCode()
        result = 31 * result + unicastRange.hashCode()
        result = 31 * result + lastUnicastAddress.hashCode()
        result = 31 * result + isCompositionDataReceived.hashCode()
        result = 31 * result + isProvisioner.hashCode()
        result = 31 * result + isLocalProvisioner.hashCode()
        result = 31 * result + (provisioner?.hashCode() ?: 0)
        return result
    }
}