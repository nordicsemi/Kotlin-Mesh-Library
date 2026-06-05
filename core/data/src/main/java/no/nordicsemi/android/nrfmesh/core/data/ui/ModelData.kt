@file:Suppress("MemberVisibilityCanBePrivate", "unused", "PropertyName")

package no.nordicsemi.android.nrfmesh.core.data.ui

import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.ModelId
import no.nordicsemi.kotlin.mesh.core.model.Publish
import no.nordicsemi.kotlin.mesh.core.model.SubscriptionAddress

/**
 * Represents Bluetooth mesh model contained in an element in a node.
 *
 * @property modelId                                    The [ModelId] property contains a 16-bit
 *                                                      [SigModelId] that represents a Bluetooth SIG
 *                                                      defined model identifier field or a 32-bit
 *                                                      [VendorModelId] that represents a
 *                                                      vendor-defined model identifier.
 * @property subscribe                                  The subscribe property contains a list of
 *                                                      [MeshAddress].
 * @property publish                                    The publish property contains a [Publish]
 *                                                      that describes the configuration of this
 *                                                      model’s publication.
 * @property bind                                       The bind property contains a list of
 *                                                      integers that represents indexes of the
 *                                                      [ApplicationKey] to which this model is
 *                                                      bound. Each application key index
 *                                                      corresponds to the index values of one of
 *                                                      the application key entries in the node’s
 *                                                      [ApplicationKey] list.
 * @property name                                       Name of the model.
 * @property isBluetoothSigAssigned                     True if the model is a Bluetooth SIG defined
 *                                                      model.
 * @property boundApplicationKeys                       List of [ApplicationKeyData] bound to the model.
 * @property supportsApplicationKeyBinding              True if the model supports application key
 *                                                      binding.
 * @property isConfigurationServer                      True if the model is a configuration server
 *                                                      model.
 * @property isConfigurationClient                      True if the model is a configuration client
 *                                                      model.
 * @property isHealthServer                             True if the model is a health server model.
 * @property isHealthClient                             True if the model is a health client model.
 * @property isSceneClient                              True if the model is a scene client model.
 * @property isRemoteProvisioningServer                 True if the model is a remote provisioning
 *                                                      server model.
 * @property isRemoteProvisioningClient                 True if the model is a remote provisioning
 *                                                      client model.
 * @property isDirectedForwardingConfigurationServer    True if the model is a directed forwarding
 *                                                      configuration server model.
 * @property isDirectedForwardingConfigurationClient    True if the model is a directed forwarding
 *                                                      configuration client model.
 * @property isBridgeConfigurationServer                True if the model is a bridge configuration
 *                                                      server model.
 * @property isBridgeConfigurationClient                True if the model is a bridge configuration
 *                                                      client model.
 * @property isPrivateBeaconServer                      True if the model is a private beacon server
 *                                                      model.
 * @property isPrivateBeaconClient                      True if the model is a private beacon client
 *                                                      model.
 * @property isOnDemandPrivateProxyServer               True if the model is a on demand private
 *                                                      proxy server model.
 * @property isOnDemandPrivateProxyClient               True if the model is a on demand private
 *                                                      proxy client model.
 * @property isSarConfigurationServer                   True if the model is a SAR configuration
 *                                                      server model.
 * @property isSarConfigurationClient                   True if the model is a SAR configuration
 *                                                      client model.
 * @property isOpcodesAggregatorServer                  True if the model is a opcodes aggregator
 *                                                      server model.
 * @property isOpcodesAggregatorClient                  True if the model is a opcodes aggregator
 *                                                      client model.
 * @property isLargeCompositionDataServer               True if the model is a large composition
 *                                                      data server model.
 * @property isLargeCompositionDataClient               True if the model is a large composition
 *                                                      data client model.
 * @property requiresDeviceKey                          True if the model requires a device key.
 * @constructor Creates a model object.
 */
@ConsistentCopyVisibility
data class ModelData internal constructor(
    val modelId: ModelId,
    val name: String?,
    val bind: List<KeyIndex>,
    val subscribe: List<SubscriptionAddress>,
    val publish: Publish? = null,
    val isBluetoothSigAssigned: Boolean,
    val boundApplicationKeys: List<ApplicationKeyData>,
    val supportsApplicationKeyBinding: Boolean,
    val supportsDeviceKey: Boolean,
    val isConfigurationServer: Boolean,
    val isConfigurationClient: Boolean,
    val isHealthServer: Boolean,
    val isHealthClient: Boolean,
    val isSceneClient: Boolean,
    val isRemoteProvisioningServer: Boolean,
    val isRemoteProvisioningClient: Boolean,
    val isDirectedForwardingConfigurationServer: Boolean,
    val isDirectedForwardingConfigurationClient: Boolean,
    val isBridgeConfigurationServer: Boolean,
    val isBridgeConfigurationClient: Boolean,
    val isPrivateBeaconServer: Boolean,
    val isPrivateBeaconClient: Boolean,
    val isOnDemandPrivateProxyServer: Boolean,
    val isOnDemandPrivateProxyClient: Boolean,
    val isSarConfigurationServer: Boolean,
    val isSarConfigurationClient: Boolean,
    val isOpcodesAggregatorServer: Boolean,
    val isOpcodesAggregatorClient: Boolean,
    val isLargeCompositionDataServer: Boolean,
    val isLargeCompositionDataClient: Boolean,
    val requiresDeviceKey: Boolean,
) {
    constructor(model: Model) : this(
        modelId = model.modelId,
        name = model.name,
        bind = model.bind,
        subscribe = model.subscribe,
        publish = model.publish,
        isBluetoothSigAssigned = model.isBluetoothSigAssigned,
        boundApplicationKeys = model.boundApplicationKeys.map { ApplicationKeyData(it) },
        supportsApplicationKeyBinding = model.supportsApplicationKeyBinding,
        supportsDeviceKey = model.supportsDeviceKey,
        isConfigurationServer = model.isConfigurationServer,
        isConfigurationClient = model.isConfigurationClient,
        isHealthServer = model.isHealthServer,
        isHealthClient = model.isHealthClient,
        isSceneClient = model.isSceneClient,
        isRemoteProvisioningServer = model.isRemoteProvisioningServer,
        isRemoteProvisioningClient = model.isRemoteProvisioningClient,
        isDirectedForwardingConfigurationServer = model.isDirectedForwardingConfigurationServer,
        isDirectedForwardingConfigurationClient = model.isDirectedForwardingConfigurationClient,
        isBridgeConfigurationServer = model.isBridgeConfigurationServer,
        isBridgeConfigurationClient = model.isBridgeConfigurationClient,
        isPrivateBeaconServer = model.isPrivateBeaconServer,
        isPrivateBeaconClient = model.isPrivateBeaconClient,
        isOnDemandPrivateProxyServer = model.isOnDemandPrivateProxyServer,
        isOnDemandPrivateProxyClient = model.isOnDemandPrivateProxyClient,
        isSarConfigurationServer = model.isSarConfigurationServer,
        isSarConfigurationClient = model.isSarConfigurationClient,
        isOpcodesAggregatorServer = model.isOpcodesAggregatorServer,
        isOpcodesAggregatorClient = model.isOpcodesAggregatorClient,
        isLargeCompositionDataServer = model.isLargeCompositionDataServer,
        isLargeCompositionDataClient = model.isLargeCompositionDataClient,
        requiresDeviceKey = model.requiresDeviceKey
    )
}

