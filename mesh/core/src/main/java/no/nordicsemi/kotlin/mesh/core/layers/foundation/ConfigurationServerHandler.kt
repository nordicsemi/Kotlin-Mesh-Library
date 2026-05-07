@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.layers.foundation

import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyDelete
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyList
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyUpdate
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigBeaconGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigBeaconSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigBeaconStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigCompositionDataGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigCompositionDataStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigDefaultTtlGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigDefaultTtlSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigDefaultTtlStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigFriendGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigFriendSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigFriendStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigGattProxyGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigGattProxySet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigGattProxyStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatPublicationGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatPublicationSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatPublicationStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatSubscriptionGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatSubscriptionSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatSubscriptionStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigKeyRefreshPhaseGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigKeyRefreshPhaseSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigKeyRefreshPhaseStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigLowPowerNodePollTimeoutGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigLowPowerNodePollTimeoutStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppBind
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppUnbind
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationVirtualAddressSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionDelete
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionDeleteAll
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionOverwrite
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionVirtualAddressAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionVirtualAddressDelete
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionVirtualAddressOverwrite
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyDelete
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyList
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyUpdate
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetworkTransmitGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetworkTransmitSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetworkTransmitStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNodeIdentityGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNodeIdentitySet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNodeIdentityStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNodeReset
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNodeResetStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigRelayGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigRelaySet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigRelayStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigSigModelAppGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigSigModelAppList
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigSigModelSubscriptionGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigSigModelSubscriptionList
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigVendorModelAppGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigVendorModelSubscriptionGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigVendorModelSubscriptionList
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.Page0
import no.nordicsemi.kotlin.mesh.core.model.AllFriends
import no.nordicsemi.kotlin.mesh.core.model.AllProxies
import no.nordicsemi.kotlin.mesh.core.model.AllRelays
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatPublication
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatSubscription
import no.nordicsemi.kotlin.mesh.core.model.KeyDistribution
import no.nordicsemi.kotlin.mesh.core.model.KeyRefreshPhase
import no.nordicsemi.kotlin.mesh.core.model.KeyRefreshPhaseTransition
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.NetworkTransmit
import no.nordicsemi.kotlin.mesh.core.model.NormalOperation
import no.nordicsemi.kotlin.mesh.core.model.PrimaryGroupAddress
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.UsingNewKeys
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import no.nordicsemi.kotlin.mesh.core.model.allNodes
import no.nordicsemi.kotlin.mesh.core.model.boundTo
import no.nordicsemi.kotlin.mesh.core.model.isValidKeyIndex
import kotlin.uuid.ExperimentalUuidApi

/**
 * ConfigurationServerHandler is responsible for handling configuration messages.
 */
internal class ConfigurationServerHandler : ModelEventHandler() {

    override val messageTypes = mapOf(
        ConfigCompositionDataGet.opCode to ConfigCompositionDataGet,
        ConfigNetKeyAdd.opCode to ConfigNetKeyAdd,
        ConfigNetKeyUpdate.opCode to ConfigNetKeyUpdate,
        ConfigNetKeyDelete.opCode to ConfigNetKeyDelete,
        ConfigNetKeyGet.opCode to ConfigNetKeyGet,
        ConfigAppKeyAdd.opCode to ConfigAppKeyAdd,
        ConfigAppKeyUpdate.opCode to ConfigAppKeyUpdate,
        ConfigAppKeyDelete.opCode to ConfigAppKeyDelete,
        ConfigAppKeyGet.opCode to ConfigAppKeyGet,
        ConfigModelAppBind.opCode to ConfigModelAppBind,
        ConfigModelAppUnbind.opCode to ConfigModelAppUnbind,
        ConfigSigModelAppGet.opCode to ConfigSigModelAppGet,
        ConfigVendorModelAppGet.opCode to ConfigVendorModelAppGet,
        ConfigModelPublicationSet.opCode to ConfigModelPublicationSet,
        ConfigModelPublicationVirtualAddressSet.opCode to ConfigModelPublicationVirtualAddressSet,
        ConfigModelPublicationGet.opCode to ConfigModelPublicationGet,
        ConfigModelSubscriptionAdd.opCode to ConfigModelSubscriptionAdd,
        ConfigModelSubscriptionOverwrite.opCode to ConfigModelSubscriptionOverwrite,
        ConfigModelSubscriptionDelete.opCode to ConfigModelSubscriptionDelete,
        ConfigModelSubscriptionVirtualAddressAdd.opCode to ConfigModelSubscriptionVirtualAddressAdd,
        ConfigModelSubscriptionVirtualAddressOverwrite.opCode to ConfigModelSubscriptionVirtualAddressOverwrite,
        ConfigModelSubscriptionVirtualAddressDelete.opCode to ConfigModelSubscriptionVirtualAddressDelete,
        ConfigModelSubscriptionDeleteAll.opCode to ConfigModelSubscriptionDeleteAll,
        ConfigSigModelSubscriptionGet.opCode to ConfigSigModelSubscriptionGet,
        ConfigVendorModelSubscriptionGet.opCode to ConfigVendorModelSubscriptionGet,
        ConfigDefaultTtlGet.opCode to ConfigDefaultTtlGet,
        ConfigDefaultTtlSet.opCode to ConfigDefaultTtlSet,
        ConfigRelayGet.opCode to ConfigRelayGet,
        ConfigRelaySet.opCode to ConfigRelaySet,
        ConfigGattProxyGet.opCode to ConfigGattProxyGet,
        ConfigGattProxySet.opCode to ConfigGattProxySet,
        ConfigFriendGet.opCode to ConfigFriendGet,
        ConfigFriendSet.opCode to ConfigFriendSet,
        ConfigBeaconGet.opCode to ConfigBeaconGet,
        ConfigBeaconSet.opCode to ConfigBeaconSet,
        ConfigNetworkTransmitGet.opCode to ConfigNetworkTransmitGet,
        ConfigNetworkTransmitSet.opCode to ConfigNetworkTransmitSet,
        ConfigNodeIdentityGet.opCode to ConfigNodeIdentityGet,
        ConfigNodeIdentitySet.opCode to ConfigNodeIdentitySet,
        ConfigNodeReset.opCode to ConfigNodeReset,
        ConfigHeartbeatPublicationGet.opCode to ConfigHeartbeatPublicationGet,
        ConfigHeartbeatPublicationSet.opCode to ConfigHeartbeatPublicationSet,
        ConfigHeartbeatSubscriptionGet.opCode to ConfigHeartbeatSubscriptionGet,
        ConfigHeartbeatSubscriptionSet.opCode to ConfigHeartbeatSubscriptionSet,
        ConfigKeyRefreshPhaseGet.opCode to ConfigKeyRefreshPhaseGet,
        ConfigKeyRefreshPhaseSet.opCode to ConfigKeyRefreshPhaseSet,
        ConfigLowPowerNodePollTimeoutGet.opCode to ConfigLowPowerNodePollTimeoutGet,
    )
    override val isSubscriptionSupported = false
    override val publicationMessageComposer = null

    override suspend fun handle(event: ModelEvent) = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> handleRequest(event = event)

        is ModelEvent.ResponseReceived -> throw ModelError.InvalidMessage(
            msg = event.response
        )

        is ModelEvent.UnacknowledgedMessageReceived -> {
            // Do nothing
            null
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun handleRequest(event: ModelEvent.AcknowledgedMessageReceived): MeshResponse? {
        return event.model.parentElement?.parentNode?.let { localNode ->
            when (val request = event.request) {
                is ConfigCompositionDataGet -> {
                    ConfigCompositionDataStatus(page = Page0(node = localNode))
                }

                is ConfigNetKeyAdd -> {
                    runCatching {
                        val keyIndex = request.networkKeyIndex
                        // Make sure the key with given index didn't exist or was identical to the one
                        // in the request. Otherwise, return [KEY_INDEX_ALREADY_STORED].
                        val existingKey = meshNetwork.networkKey(index = keyIndex)

                        existingKey
                            ?.takeIf { !it.key.contentEquals(request.key) }
                            ?.let {
                                return ConfigNetKeyStatus(
                                    request = request,
                                    status = ConfigMessageStatus.KEY_INDEX_ALREADY_STORED
                                )
                            }

                        val networkKey = existingKey ?: meshNetwork.add(
                            name = "Network Key ${keyIndex + 1u}",
                            key = request.key,
                            index = keyIndex
                        )
                        // Add the Network Key index to the local Node.
                        localNode.addNetKey(index = keyIndex)
                        ConfigNetKeyStatus(networkKey = networkKey)
                    }.getOrElse {
                        ConfigNetKeyStatus(
                            request = request,
                            status = ConfigMessageStatus.UNSPECIFIED_ERROR
                        )
                    }
                }

                is ConfigNetKeyUpdate -> {
                    val keyIndex = request.networkKeyIndex
                    val networkKey = meshNetwork.networkKey(index = keyIndex)
                    // If there’s no such key, return invalidNetKeyIndex
                    if (networkKey == null) {
                        return ConfigNetKeyStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_NET_KEY_INDEX
                        )
                    }
                    // The key can only be changed once during a single Key Refresh Procedure
                    if (networkKey.phase != NormalOperation &&
                        !(networkKey.phase == KeyDistribution && networkKey.key.contentEquals(
                            other = request.newKey))
                    ) {
                        return ConfigNetKeyStatus(
                            request = request,
                            status = ConfigMessageStatus.KEY_INDEX_ALREADY_STORED
                        )
                    }

                    if (networkKey.phase == NormalOperation) {
                        // Update key data
                        networkKey.key = request.newKey
                        // Mark key as updated on local node
                        localNode.updateNetKey(index = keyIndex)
                    }

                    // Confirm response with updated key
                    ConfigNetKeyStatus(networkKey = networkKey)
                }

                is ConfigNetKeyDelete -> {
                    val index = request.networkKeyIndex

                    // If the key isn't present, respond with success (deleting a non-existent key
                    // is a no-op)
                    if (meshNetwork.networkKey(index = index) == null) {
                        return ConfigNetKeyStatus(
                            request = request,
                            status = ConfigMessageStatus.SUCCESS
                        )
                    }
                    // It is not possible to remove the last key
                    if (meshNetwork.networkKeys.size <= 1) {
                        return ConfigNetKeyStatus(
                            request = request,
                            status = ConfigMessageStatus.CANNOT_REMOVE
                        )
                    }
                    // Force delete the key from the network configuration
                    runCatching {
                        meshNetwork.removeNetworkKeyAtIndex(index = index.toInt(), force = true)
                    }

                    // Return success
                    return ConfigNetKeyStatus(
                        request = request,
                        status = ConfigMessageStatus.SUCCESS
                    )
                }

                is ConfigNetKeyGet -> ConfigNetKeyList(networkKeyIndexes = meshNetwork.networkKeys.map { it.index })

                is ConfigAppKeyAdd -> {
                    val networkKeyIndex = request.networkKeyIndex
                    // If the Network Key does not exist, return INVALID_NET_KEY_INDEX
                    val networkKey = meshNetwork.networkKey(index = networkKeyIndex)
                        ?: return ConfigAppKeyStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_NET_KEY_INDEX
                        )

                    val keyIndex = request.applicationKeyIndex

                    return runCatching {
                        // Check if the application key exists or is an identical key bound to the same network key
                        val existingKey = meshNetwork.applicationKey(index = keyIndex)

                        if (existingKey != null &&
                            (!existingKey.key.contentEquals(other = request.key) || !existingKey.isBoundTo(
                                networkKey
                            ))
                        ) {
                            return ConfigAppKeyStatus(
                                request = request,
                                status = ConfigMessageStatus.KEY_INDEX_ALREADY_STORED
                            )
                        }

                        val applicationKey = existingKey ?: meshNetwork.add(
                            key = request.key,
                            index = keyIndex,
                            name = "Application Key ${keyIndex + 1u}",
                            boundNetworkKey = networkKey
                        )

                        // Add the Application Key to the local node
                        localNode.addAppKey(index = keyIndex)
                        ConfigAppKeyStatus(applicationKey = applicationKey)
                    }.getOrElse {
                        ConfigAppKeyStatus(
                            request = request,
                            status = ConfigMessageStatus.UNSPECIFIED_ERROR
                        )
                    }
                }

                is ConfigAppKeyUpdate -> {
                    val networkKeyIndex = request.networkKeyIndex
                    // If the Network Key does not exist, return INVALID_NET_KEY_INDEX
                    val networkKey = meshNetwork.networkKey(index = networkKeyIndex)
                        ?: return ConfigAppKeyStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_NET_KEY_INDEX
                        )

                    val keyIndex = request.applicationKeyIndex

                    // If the Application Key does not exist, return INVALID_APP_KEY_INDEX
                    val applicationKey = meshNetwork.applicationKey(index = keyIndex)
                        ?: return ConfigAppKeyStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_APP_KEY_INDEX
                        )

                    // If the binding is incorrect, return INVALID_BINDING
                    if (!applicationKey.isBoundTo(networkKey)) {
                        return ConfigAppKeyStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_BINDING
                        )
                    }

                    // Updating Application Key is only allowed during Key Refresh Procedure (KeyDistribution phase)
                    if (networkKey.phase !is KeyDistribution) {
                        return ConfigAppKeyStatus(
                            request = request,
                            status = ConfigMessageStatus.CANNOT_UPDATE
                        )
                    }

                    // The key can't be changed multiple times in a single Key Refresh Procedure
                    if (applicationKey.oldKey != null &&
                        !applicationKey.key.contentEquals(other = request.key)) {
                        return ConfigAppKeyStatus(
                            request = request,
                            status = ConfigMessageStatus.KEY_INDEX_ALREADY_STORED
                        )
                    }

                    // If it's the first time updating during this Key Refresh Procedure
                    if (applicationKey.oldKey == null) {
                        applicationKey.key = request.key
                        localNode.updateAppKey(index = keyIndex)
                    }

                    // Confirm with the updated application key
                    ConfigAppKeyStatus(applicationKey = applicationKey)
                }

                is ConfigAppKeyDelete -> {
                    val networkKeyIndex = request.networkKeyIndex
                    // If the Network Key does not exist, return INVALID_NET_KEY_INDEX
                    val networkKey = meshNetwork.networkKey(index = networkKeyIndex)
                        ?: return ConfigAppKeyStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_NET_KEY_INDEX
                        )

                    val keyIndex = request.applicationKeyIndex

                    // If the Application Key does not exist, respond with SUCCESS (as per Mesh spec)
                    val applicationKey = meshNetwork.applicationKey(index = keyIndex)
                        ?: return ConfigAppKeyStatus(
                            request = request,
                            status = ConfigMessageStatus.SUCCESS
                        )

                    // Check if the binding is correct. Otherwise, return INVALID_BINDING
                    if (!applicationKey.isBoundTo(networkKey = networkKey)) {
                        return ConfigAppKeyStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_BINDING
                        )
                    }

                    // Remove the key from the global configuration
                    runCatching {
                        meshNetwork.removeApplicationKeyWithIndex(index = keyIndex, force = true)
                    }

                    // Respond with SUCCESS
                    ConfigAppKeyStatus(
                        request = request,
                        status = ConfigMessageStatus.SUCCESS
                    )
                }

                is ConfigAppKeyGet -> {
                    val networkKeyIndex = request.networkKeyIndex

                    // If the Network Key does not exist, return INVALID_NET_KEY_INDEX
                    val networkKey = meshNetwork.networkKey(index = networkKeyIndex)
                        ?: return ConfigAppKeyList(
                            request = request,
                            status = ConfigMessageStatus.INVALID_NET_KEY_INDEX
                        )

                    // Get application keys bound to the network key
                    val boundAppKeys = meshNetwork.applicationKeys.boundTo(networkKey = networkKey)

                    // Return the application key list status with the bound keys
                    return ConfigAppKeyList(
                        request = request,
                        applicationKeys = boundAppKeys
                    )
                }

                is ConfigModelAppBind -> {
                    val element = localNode.element(address = request.elementAddress)
                        ?: return ConfigModelAppStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )
                    val model = element.model(modelId = request.modelId)
                        ?: return ConfigModelAppStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_MODEL
                        )
                    val applicationKey =
                        meshNetwork.applicationKey(index = request.applicationKeyIndex)
                            ?: return ConfigModelAppStatus(
                                request = request,
                                status = ConfigMessageStatus.INVALID_APP_KEY_INDEX
                            )
                    model.bind(index = applicationKey.index)
                    return ConfigModelAppStatus(request = request)
                }

                is ConfigModelAppUnbind -> {
                    val element = localNode.element(address = request.elementAddress)
                        ?: return ConfigModelAppStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )
                    val model = element.model(modelId = request.modelId)
                        ?: return ConfigModelAppStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_MODEL
                        )
                    model.unbind(index = request.applicationKeyIndex)
                    return ConfigModelAppStatus(request = request)
                }

                is ConfigSigModelAppGet -> {
                    val element = localNode.element(address = request.elementAddress)
                        ?: return ConfigSigModelAppList(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )
                    val model = element.model(modelId = request.modelId)
                        ?: return ConfigSigModelAppList(
                            request = request,
                            status = ConfigMessageStatus.INVALID_MODEL
                        )
                    return ConfigSigModelAppList(
                        request = request,
                        keys = model.boundApplicationKeys
                    )
                }

                is ConfigModelPublicationSet -> {
                    val element = localNode.element(address = request.elementAddress)
                        ?: return ConfigModelPublicationStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )

                    val model = element.model(modelId = request.modelId)
                        ?: return ConfigModelPublicationStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_MODEL
                        )

                    if (model.eventHandler?.publicationMessageComposer == null) {
                        return ConfigModelPublicationStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_PUBLISH_PARAMETERS
                        )
                    }

                    if (!request.publish.isCanceled &&
                        meshNetwork.applicationKey(index = request.publish.index) == null
                    ) {
                        return ConfigModelPublicationStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_APP_KEY_INDEX
                        )
                    }

                    if (!request.publish.isUsingMasterSecurityMaterial) {
                        // Low Power feature isn't supported in this library.
                        return ConfigModelPublicationStatus(
                            request = request,
                            status = ConfigMessageStatus.FEATURE_NOT_SUPPORTED
                        )
                    }

                    if (!request.publish.isCanceled) {
                        val address = request.publish.address
                        if (address is GroupAddress &&
                            meshNetwork.group(address = address.address) == null
                        ) {
                            meshNetwork.add(group = Group(_name = "New Group", address = address))
                        }
                        model.publish = request.publish
                    } else {
                        model.clearPublication()
                    }
                    return ConfigModelPublicationStatus(request = request)
                }

                is ConfigModelPublicationVirtualAddressSet -> {
                    val element = localNode.element(address = request.elementAddress)
                        ?: return ConfigModelPublicationStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )

                    val model = element.model(request.modelId)
                        ?: return ConfigModelPublicationStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_MODEL
                        )

                    if (model.eventHandler?.publicationMessageComposer == null) {
                        return ConfigModelPublicationStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_PUBLISH_PARAMETERS
                        )
                    }

                    if (meshNetwork.applicationKey(index = request.publish.index) == null) {
                        return ConfigModelPublicationStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_APP_KEY_INDEX
                        )
                    }

                    if (!request.publish.isUsingMasterSecurityMaterial) {
                        return ConfigModelPublicationStatus(
                            request = request,
                            status = ConfigMessageStatus.FEATURE_NOT_SUPPORTED
                        )
                    }

                    // A new group?
                    val address = request.publish.address
                    if (address is GroupAddress &&
                        meshNetwork.group(address = address.address) == null
                    ) {
                        meshNetwork.add(group = Group(_name = "New Group", address = address))
                    }
                    model.publish = request.publish
                    return ConfigModelPublicationStatus(request = request)
                }

                is ConfigModelPublicationGet -> {
                    val element = localNode.element(address = request.elementAddress)
                        ?: return ConfigModelPublicationStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )
                    val model = element.model(modelId = request.modelId)
                        ?: return ConfigModelPublicationStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_MODEL
                        )
                    return ConfigModelPublicationStatus(request = request, publish = model.publish)
                }

                is ConfigModelSubscriptionAdd -> {
                    val element = localNode.element(address = request.elementAddress)
                        ?: return ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )

                    val model = element.model(modelId = request.modelId)
                        ?: return ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.INVALID_MODEL
                        )

                    if (!GroupAddress.isValid(address = request.address) || request.address == allNodes) {
                        return ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )
                    }

                    if (model.eventHandler?.isSubscriptionSupported == false) {
                        return ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.NOT_A_SUBSCRIBE_MODEL
                        )
                    }

                    runCatching {
                        val address = MeshAddress.create(address = request.address).let {
                            when (it) {
                                is AllRelays -> it
                                is AllFriends -> it
                                is AllProxies -> it
                                is GroupAddress -> it
                                else -> throw IllegalArgumentException("Invalid Address")
                            } as GroupAddress
                        }
                        model.subscribe(address = address)
                        ConfigModelSubscriptionStatus(address = address, model = model)
                    }.getOrElse {
                        ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )
                    }
                }

                is ConfigModelSubscriptionDelete -> {
                    val element = localNode.element(address = request.elementAddress)
                        ?: return ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )

                    val model = element.model(modelId = request.modelId)
                        ?: return ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.INVALID_MODEL
                        )

                    if (
                        !GroupAddress.isValid(address = request.address) ||
                        request.address == allNodes
                    ) {
                        return ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )
                    }
                    model.unsubscribe(request.address)
                    return ConfigModelSubscriptionStatus.init(
                        request = request,
                        status = ConfigMessageStatus.SUCCESS
                    )
                }

                is ConfigModelSubscriptionVirtualAddressAdd -> {
                    val element = localNode.element(request.elementAddress)
                        ?: return ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )

                    val model = element.model(request.modelId)
                        ?: return ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.INVALID_MODEL
                        )

                    if (model.eventHandler?.isSubscriptionSupported == false) {
                        return ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.NOT_A_SUBSCRIBE_MODEL
                        )
                    }

                    runCatching {
                        val address = VirtualAddress(uuid = request.virtualLabel)
                        val group = meshNetwork.group(address = address.address)
                            ?: createGroup(address)
                        model.subscribe(group = group)

                        ConfigModelSubscriptionStatus(group = group, model = model)
                    }.getOrElse {
                        ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )
                    }
                }

                is ConfigModelSubscriptionVirtualAddressOverwrite -> {
                    val element = localNode.element(request.elementAddress)
                        ?: return ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )

                    val model = element.model(request.modelId)
                        ?: return ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.INVALID_MODEL
                        )

                    if (model.eventHandler?.isSubscriptionSupported == false) {
                        return ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.NOT_A_SUBSCRIBE_MODEL
                        )
                    }

                    runCatching {
                        val address = VirtualAddress(uuid = request.virtualLabel)
                        val group = meshNetwork.group(address = address.address)
                            ?: createGroup(address)
                        model.unsubscribeFromAll()
                        model.subscribe(group = group)

                        ConfigModelSubscriptionStatus(group = group, model = model)
                    }.getOrElse {
                        ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )
                    }
                }

                is ConfigModelSubscriptionVirtualAddressDelete -> {
                    val element = localNode.element(request.elementAddress)
                        ?: return ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )

                    val model = element.model(request.modelId)
                        ?: return ConfigModelSubscriptionStatus.init(
                            request = request,
                            status = ConfigMessageStatus.INVALID_MODEL
                        )

                    val address = VirtualAddress(uuid = request.virtualLabel)
                    meshNetwork.group(address = address.address)?.let {
                        model.unsubscribe(group = it)
                    }
                    ConfigModelSubscriptionStatus(address = address, model = model)
                }

                is ConfigModelSubscriptionDeleteAll -> {
                    val element = localNode.element(request.elementAddress)
                        ?: return ConfigModelSubscriptionStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )

                    val model = element.model(request.modelId)
                        ?: return ConfigModelSubscriptionStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_MODEL
                        )

                    model.unsubscribeFromAll()
                    ConfigModelSubscriptionStatus(model = model)
                }

                is ConfigSigModelSubscriptionGet -> {
                    val element = localNode.element(request.elementAddress)
                        ?: return ConfigSigModelSubscriptionList(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )

                    val model = element.model(request.modelId)
                        ?: return ConfigSigModelSubscriptionList(
                            request = request,
                            status = ConfigMessageStatus.INVALID_MODEL
                        )

                    val addresses = model.subscribe.map { it.address }
                    return ConfigSigModelSubscriptionList(
                        request = request,
                        addresses = addresses
                    )
                }

                is ConfigVendorModelSubscriptionGet -> {
                    val element = localNode.element(request.elementAddress)
                        ?: return ConfigVendorModelSubscriptionList(
                            request = request,
                            status = ConfigMessageStatus.INVALID_ADDRESS
                        )

                    val model = element.model(request.modelId)
                        ?: return ConfigVendorModelSubscriptionList(
                            request = request,
                            status = ConfigMessageStatus.INVALID_MODEL
                        )

                    ConfigVendorModelSubscriptionList(
                        request = request,
                        addresses = model.subscribe.map { it.address }
                    )
                }

                is ConfigDefaultTtlSet, is ConfigDefaultTtlGet -> {
                    if (request is ConfigDefaultTtlSet) {
                        localNode.defaultTTL = request.ttl
                    }
                    ConfigDefaultTtlStatus(ttl = localNode.defaultTTL ?: 5u)
                }

                is ConfigRelayGet, is ConfigRelaySet -> {
                    // Relay feature is not supported
                    ConfigRelayStatus.unsupported
                }

                is ConfigGattProxyGet, is ConfigGattProxySet -> {
                    // Gatt Proxy feature is not supported
                    ConfigGattProxyStatus.unsupported
                }

                is ConfigFriendGet, is ConfigFriendSet -> {
                    // Friend feature is not supported
                    ConfigFriendStatus.unsupported
                }

                is ConfigBeaconGet, is ConfigBeaconSet -> {
                    // Secure Network Beacon feature is not supported.
                    // TODO Add support for sending Secure Network Beacons.
                    ConfigBeaconStatus(isEnabled = false)
                }

                is ConfigNetworkTransmitGet, is ConfigNetworkTransmitSet -> {
                    if (request is ConfigNetworkTransmitSet) {
                        localNode.networkTransmit = NetworkTransmit(request = request)
                    }
                    ConfigNetworkTransmitStatus(node = localNode)
                }

                is ConfigNodeIdentityGet, is ConfigNodeIdentitySet -> {
                    // Node Identity feature is not supported
                    ConfigNodeIdentityStatus(request = request as ConfigNetKeyMessage)
                }

                is ConfigNodeReset -> ConfigNodeResetStatus()

                is ConfigHeartbeatPublicationGet, is ConfigHeartbeatPublicationSet -> {
                    if (request is ConfigHeartbeatPublicationSet) {
                        // The Heartbeat Publication Destination shall be the Unassigned Address, a
                        // Unicast Address, or a Group Address. All other values are Prohibited.
                        when {
                            request.destination !is UnassignedAddress &&
                                    request.destination !is UnicastAddress &&
                                    request.destination !is GroupAddress
                                -> {
                                return ConfigHeartbeatPublicationStatus(
                                    request = request,
                                    status = ConfigMessageStatus.CANNOT_SET
                                )
                            }

                            !request.networkKeyIndex.isValidKeyIndex() ||
                                    (meshNetwork.networkKey(index = request.networkKeyIndex) == null) &&
                                    request.isPublicationEnabled -> {
                                return ConfigHeartbeatPublicationStatus(
                                    request = request,
                                    status = ConfigMessageStatus.INVALID_NET_KEY_INDEX
                                )
                            }

                            request.ttl > 0x7Fu ||
                                    request.periodLog > 0x11u ||
                                    (request.countLog > 0x11u &&
                                            request.countLog != 0xFF.toUByte()) -> {
                                return ConfigHeartbeatPublicationStatus(
                                    request = request,
                                    status = ConfigMessageStatus.CANNOT_SET
                                )
                            }
                        }
                        localNode.heartbeatPublication = HeartbeatPublication(request = request)
                    }
                    ConfigHeartbeatPublicationStatus(publication = localNode.heartbeatPublication)
                }

                is ConfigHeartbeatSubscriptionGet, is ConfigHeartbeatSubscriptionSet -> {
                    if (request is ConfigHeartbeatSubscriptionSet) {
                        // The Heartbeat Subscription Source shall be the Unassigned Address or a
                        // Unicast Address, all other values are Prohibited.
                        if (request.source is UnassignedAddress ||
                            request.source is UnicastAddress
                        ) {
                            return ConfigHeartbeatSubscriptionStatus(
                                request = request,
                                status = ConfigMessageStatus.CANNOT_SET
                            )
                        }
                        // The Heartbeat Subscription Destination shall be the Unassigned Address,
                        // the primary Unicast Address of the local Node, or a Group Address, all
                        // other values are Prohibited.
                        if (request.destination !is UnassignedAddress &&
                            request.destination !is GroupAddress &&
                            request.destination != localNode.primaryUnicastAddress
                        ) {
                            return ConfigHeartbeatSubscriptionStatus(
                                request = request,
                                status = ConfigMessageStatus.CANNOT_SET
                            )
                        }
                        // Values 0x12-0xFF are Prohibited.
                        if (request.periodLog > 0x11u) {
                            return ConfigHeartbeatSubscriptionStatus(
                                request = request,
                                status = ConfigMessageStatus.CANNOT_SET
                            )
                        }
                        // If the Set message disables active Heartbeat subscription, the returned
                        // Status should contain the last Min Hops, Max Hops and CountLog.
                        if (!request.isSubscriptionEnabled) {
                            localNode.heartbeatSubscription?.let { currentSubscription ->
                                localNode.heartbeatSubscription = null
                                return ConfigHeartbeatSubscriptionStatus(
                                    subscription = currentSubscription
                                )
                            }
                        }

                        // Otherwise, set a new subscription
                        localNode.heartbeatSubscription = HeartbeatSubscription(request = request)
                    }
                    ConfigHeartbeatSubscriptionStatus(subscription = localNode.heartbeatSubscription)
                }

                is ConfigKeyRefreshPhaseGet -> {
                    // If there is no such key, return .invalidNetKeyIndex.
                    val networkKey = meshNetwork.networkKey(index = request.networkKeyIndex)
                        ?: return ConfigKeyRefreshPhaseStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_NET_KEY_INDEX
                        )

                    return ConfigKeyRefreshPhaseStatus(networkKey = networkKey)
                }

                is ConfigKeyRefreshPhaseSet -> {
                    // If there is no such key, return .invalidNetKeyIndex.
                    val networkKey = meshNetwork.networkKey(index = request.networkKeyIndex)
                        ?: return ConfigKeyRefreshPhaseStatus(
                            request = request,
                            status = ConfigMessageStatus.INVALID_NET_KEY_INDEX
                        )
                    // Check all possible transitions.
                    when (networkKey.phase to request.transition) {
                        // Not allowed to transition from NormalOperation to UsingNewKeys
                        NormalOperation to KeyRefreshPhaseTransition.UseNewKeys ->
                            return ConfigKeyRefreshPhaseStatus(
                                request,
                                ConfigMessageStatus.CANNOT_SET
                            )

                        // Transition from DistributingKeys to UsingNewKeys
                        KeyDistribution to KeyRefreshPhaseTransition.UseNewKeys ->
                            networkKey.phase = UsingNewKeys

                        // Already in UsingNewKeys, no action needed
                        UsingNewKeys to KeyRefreshPhaseTransition.UseNewKeys -> {
                            // No-op
                        }

                        // No-op for transitioning from Normal to RevokeOldKeys
                        NormalOperation to KeyRefreshPhaseTransition.RevokeOldKeys -> {
                            // No-op
                        }
                        // Transition from UsingNewKeys to RevokeOldKeys
                        KeyRefreshPhase to KeyRefreshPhaseTransition.RevokeOldKeys -> {
                            networkKey.oldKey = null
                            meshNetwork.applicationKeys.boundTo(networkKey)
                                .forEach { it.oldKey = null }
                        }
                    }
                    return ConfigKeyRefreshPhaseStatus(networkKey = networkKey)
                }

                is ConfigLowPowerNodePollTimeoutGet -> {
                    // The library does not support Friend feature. Therefore reply with
                    // PollTimeout set to 0x000000.
                    ConfigLowPowerNodePollTimeoutStatus(request = request)
                }

                else -> {
                    null
                }
            }
        }
    }

    private fun createGroup(address: PrimaryGroupAddress) = Group(
        _name = "New Group", address = address
    ).also { meshNetwork.add(group = it) }
}