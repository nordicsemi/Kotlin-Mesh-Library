@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.layers.foundation

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigModelAppList
import no.nordicsemi.kotlin.mesh.core.messages.ConfigModelSubscriptionList
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyDelete
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyList
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyUpdate
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigBeaconStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigCompositionDataStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigDefaultTtlStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigFriendStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigGattProxyStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatPublicationStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatSubscriptionStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigKeyRefreshPhaseStatus
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
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyList
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyUpdate
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetworkTransmitStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNodeIdentityStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNodeResetStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigRelayStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigSigModelAppList
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigSigModelSubscriptionList
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigVendorModelAppList
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigVendorModelSubscriptionList
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.FeatureState
import no.nordicsemi.kotlin.mesh.core.model.FixedGroupAddress
import no.nordicsemi.kotlin.mesh.core.model.Friend
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatPublication
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatSubscription
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.NetworkTransmit
import no.nordicsemi.kotlin.mesh.core.model.PrimaryGroupAddress
import no.nordicsemi.kotlin.mesh.core.model.Proxy
import no.nordicsemi.kotlin.mesh.core.model.Relay
import no.nordicsemi.kotlin.mesh.core.model.RelayRetransmit
import no.nordicsemi.kotlin.mesh.core.model.SubscriptionAddress
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress

/**
 * ConfigurationClientHandler class handles the configuration messages sent from the provisioner
 *
 * @property messageTypes                  Message types supported by the handler.
 * @property isSubscriptionSupported       True if the model supports subscription.
 * @property publicationMessageComposer    Message composer for publication messages.
 * @constructor Initialize ConfigurationClientHandler
 */
internal class ConfigurationClientHandler() : ModelEventHandler() {

    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        ConfigCompositionDataStatus.opCode to ConfigCompositionDataStatus,
        ConfigNetKeyStatus.opCode to ConfigNetKeyStatus,
        ConfigNetKeyList.opCode to ConfigNetKeyList,
        ConfigAppKeyStatus.opCode to ConfigAppKeyStatus,
        ConfigAppKeyList.opCode to ConfigAppKeyList,
        ConfigModelAppStatus.opCode to ConfigModelAppStatus,
        ConfigSigModelAppList.opCode to ConfigSigModelAppList,
        ConfigVendorModelAppList.opCode to ConfigVendorModelAppList,
        ConfigModelPublicationStatus.opCode to ConfigModelPublicationStatus,
        ConfigModelSubscriptionStatus.opCode to ConfigModelSubscriptionStatus,
        ConfigSigModelSubscriptionList.opCode to ConfigSigModelSubscriptionList,
        ConfigVendorModelSubscriptionList.opCode to ConfigVendorModelSubscriptionList,
        ConfigDefaultTtlStatus.opCode to ConfigDefaultTtlStatus,
        ConfigRelayStatus.opCode to ConfigRelayStatus,
        ConfigGattProxyStatus.opCode to ConfigGattProxyStatus,
        ConfigFriendStatus.opCode to ConfigFriendStatus,
        ConfigBeaconStatus.opCode to ConfigBeaconStatus,
        ConfigNetworkTransmitStatus.opCode to ConfigNetworkTransmitStatus,
        ConfigNodeIdentityStatus.opCode to ConfigNodeIdentityStatus,
        ConfigNodeResetStatus.opCode to ConfigNodeResetStatus,
        ConfigHeartbeatPublicationStatus.opCode to ConfigHeartbeatPublicationStatus,
        ConfigHeartbeatSubscriptionStatus.opCode to ConfigHeartbeatSubscriptionStatus,
        ConfigKeyRefreshPhaseStatus.opCode to ConfigKeyRefreshPhaseStatus,
        ConfigLowPowerNodePollTimeoutStatus.opCode to ConfigLowPowerNodePollTimeoutStatus
    )
    override val isSubscriptionSupported: Boolean = false
    override val publicationMessageComposer: MessageComposer? = null

    /**
     * Handles the model events.
     *
     * @param event Event to be handled.
     * @throws ModelError if an acknowledged message is received.
     */
    override suspend fun handle(event: ModelEvent): MeshResponse? {
        when (event) {
            is ModelEvent.AcknowledgedMessageReceived -> throw ModelError.InvalidMessage(
                msg = event.request
            )

            is ModelEvent.ResponseReceived -> handleResponses(
                response = event.response,
                request = event.request,
                source = event.source
            )

            is ModelEvent.UnacknowledgedMessageReceived -> {
                // Ignore do nothing
            }
        }
        return null
    }

    /**
     * Handles the responses received by the client model.
     *
     * @param response  Response that was received by the model.
     * @param request   Request that was sent.
     * @param source    Address of the Element from which the message was sent.
     */
    private fun handleResponses(
        response: MeshResponse,
        request: AcknowledgedMeshMessage,
        source: Address,
    ) = meshNetwork.run {
        when (response) {
            // Composition Data
            is ConfigCompositionDataStatus -> {
                require(localProvisioner?.primaryUnicastAddress?.address != source) {
                    return
                }
                node(address = source)?.apply(compositionData = response)
            }
            // Network Keys Management
            is ConfigNetKeyStatus -> if (response.isSuccess) {
                node(address = source)?.apply {
                    when (request as ConfigNetKeyMessage) {
                        is ConfigNetKeyAdd -> addNetKey(response.networkKeyIndex)
                        is ConfigNetKeyDelete -> removeNetKey(response.networkKeyIndex)
                        is ConfigNetKeyUpdate -> updateNetKey(response.networkKeyIndex)
                    }
                }
            }

            is ConfigNetKeyList -> node(address = source)?.apply {
                setNetKeys(netKeyIndexes = response.networkKeyIndexes.toList())
            }

            // Application Keys Management
            is ConfigAppKeyStatus -> if (response.isSuccess) node(address = source)?.apply {
                when (request as ConfigNetKeyMessage) {
                    is ConfigAppKeyAdd -> addAppKey(index = response.applicationKeyIndex)
                    is ConfigAppKeyUpdate -> updateAppKey(index = response.applicationKeyIndex)
                    is ConfigAppKeyDelete -> removeAppKey(response.applicationKeyIndex)
                }
            }

            is ConfigAppKeyList -> node(address = source)?.apply {
                setAppKeys(
                    appKeyIndexes = response.applicationKeyIndexes.toList(),
                    netKeyIndex = response.networkKeyIndex
                )
            }

            is ConfigModelAppStatus -> if (response.isSuccess) {
                node(address = source)
                    ?.element(address = response.elementAddress)
                    ?.model(modelId = response.modelId)
                    ?.let {
                        when (request) {
                            is ConfigModelAppBind -> it.bind(index = request.applicationKeyIndex)
                            is ConfigModelAppUnbind -> it.unbind(index = request.applicationKeyIndex)
                        }
                    }
            }

            is ConfigModelAppList -> if (response.isSuccess) {
                node(address = source)
                    ?.element(address = response.elementAddress)
                    ?.model(modelId = response.modelId)
                    ?.bind(indexes = response.applicationKeyIndexes)
                    .also { updateTimestamp() }
            }

            is ConfigModelPublicationStatus -> if (response.isSuccess) {
                node(address = source)
                    ?.element(address = response.elementAddress)
                    ?.model(modelId = response.modelId)?.let { model ->
                        when (request) {
                            is ConfigModelPublicationGet -> {
                                val publicationAddress = response.publish.address
                                when {
                                    response.publish.isCanceled -> model.clearPublication()
                                    publicationAddress is VirtualAddress -> group(
                                        address = publicationAddress.address
                                    )?.takeIf { it.address is VirtualAddress }
                                        ?.let { model.set(response.publish) }

                                    else -> model.set(response.publish)
                                }
                            }

                            is ConfigModelPublicationSet -> if (!response.publish.isCanceled)
                                model.set(response.publish)
                            else model.clearPublication()

                            is ConfigModelPublicationVirtualAddressSet -> model.set(response.publish)
                            else -> {}
                        }
                    }
            } else {
                // Do nothing
            }

            is ConfigModelSubscriptionStatus -> if (response.isSuccess) {
                val element = node(address = source)
                    ?.element(address = response.elementAddress)
                // val model = element?.models?.model(modelId = response.modelId) ?: return
                element?.model(modelId = response.modelId)?.let { model ->
                    // When a Subscription List is modified on a Node, it affects all
                    // Models with bound state on the same Element.
                    val models = arrayOf(model) + model.relatedModels
                        .filter { it.parentElement == model.parentElement }
                    // The status for delete all request has an invalid address. Lets handle it
                    // directly here.
                    if (request is ConfigModelSubscriptionDeleteAll) {
                        models.forEach { it.unsubscribeFromAll() }
                        return
                    }
                    val address = (meshNetwork.group(address = response.address)?.address
                        ?: MeshAddress.create(address = response.address)) as SubscriptionAddress

                    when (request) {
                        is ConfigModelSubscriptionOverwrite,
                        is ConfigModelSubscriptionVirtualAddressOverwrite,
                            -> {
                            models.forEach { it.unsubscribeFromAll() }
                            models.forEach { it.subscribe(address = address) }
                        }

                        is ConfigModelSubscriptionAdd,
                        is ConfigModelSubscriptionVirtualAddressAdd,
                            ->
                            models.forEach { it.subscribe(address = address) }

                        is ConfigModelSubscriptionDelete,
                        is ConfigModelSubscriptionVirtualAddressDelete,
                            -> models
                            .forEach { it.unsubscribe(address = address.address) }

                        else -> {}
                    }
                }
            }


            is ConfigModelSubscriptionList -> {
                if (response.isSuccess) {
                    val element = node(address = source)
                        ?.element(address = response.elementAddress)
                    element?.model(modelId = response.modelId)?.let { model ->
                        // When a Subscription List is modified on a Node, it affects all
                        // Models with bound state on the same Element.
                        val models = arrayOf(model) + model.relatedModels
                            .filter { it.parentElement == model.parentElement }
                        // A new list will be set Remove existing subscriptions
                        models.forEach { it.unsubscribeFromAll() }
                        // For each new address...
                        response.addresses.forEach {
                            // ...look for an existing Group.
                            val address = (meshNetwork.group(address = it)?.address
                                ?: MeshAddress.create(address = it)) as SubscriptionAddress
                            // Check if address is FixedGroupAddress
                            if (address is FixedGroupAddress) {
                                models.forEach { model -> model.subscribe(address = address) }
                            } else /*if (address is GroupAddress)*/ {
                                // The above line was commented out to support groups with virtual
                                // addresses
                                meshNetwork.group(address = address.address)?.let { group ->
                                    models.forEach { model -> model.subscribe(group = group) }
                                } ?: run {
                                    // If the group was not found lets create a new one.
                                    val group = Group(
                                        address = address as PrimaryGroupAddress,
                                        _name = "New Group"
                                    )
                                    runCatching {
                                        meshNetwork.add(group = group)
                                        models.forEach { model -> model.subscribe(group = group) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is ConfigDefaultTtlStatus -> node(address = source)
                ?.apply { defaultTTL = response.ttl }

            is ConfigRelayStatus -> node(address = source)?.apply {
                features._relay = Relay(state = response.state)
                relayRetransmit = when (response.state) {
                    FeatureState.Unsupported -> null
                    FeatureState.Disabled, FeatureState.Enabled -> RelayRetransmit(response)
                }
                updateTimestamp()
            }

            is ConfigGattProxyStatus -> node(address = source)?.apply {
                features._proxy = Proxy(state = response.state)
                updateTimestamp()
            }

            is ConfigFriendStatus -> node(address = source)?.apply {
                features._friend = Friend(state = response.state)
                updateTimestamp()
            }

            is ConfigBeaconStatus -> node(address = source)?.apply {
                secureNetworkBeacon = response.isEnabled
            }

            is ConfigNetworkTransmitStatus -> node(address = source)?.apply {
                networkTransmit = NetworkTransmit(response)
            }

            is ConfigNodeResetStatus -> node(address = source)?.let { remove(it) }

            is ConfigHeartbeatPublicationStatus -> node(address = source)?.takeIf {
                !it.isLocalProvisioner
            }?.let {
                it.heartbeatPublication = when {
                    response.isEnabled -> HeartbeatPublication(response)
                    else -> null
                }
            }

            is ConfigHeartbeatSubscriptionStatus -> node(address = source)?.takeIf {
                !it.isLocalProvisioner
            }?.let {
                it.heartbeatSubscription = when {
                    response.isEnabled -> HeartbeatSubscription(response)
                    else -> null
                }
            }

            is ConfigNodeIdentityStatus -> {
                // Do nothing here as we don't store the NodeIdentityState in the CDB.
            }

            is ConfigKeyRefreshPhaseStatus -> {
                // Do nothing. The model does not need to be updated.
            }

            is ConfigLowPowerNodePollTimeoutStatus -> {
                // Do nothing. The model does not need to be updated.
            }
        }
    }
}