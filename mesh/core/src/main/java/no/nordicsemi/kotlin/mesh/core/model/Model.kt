@file:Suppress("MemberVisibilityCanBePrivate", "unused", "PropertyName")

package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import kotlin.uuid.ExperimentalUuidApi

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
 * @property publish                                    Publish property contains a [Publish]
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
 * @property parentElement                              Parent element of the model.
 * @property boundApplicationKeys                       List of [ApplicationKey] bound to the model.
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
 * @property isOnDemandPrivateProxyServer               True if the model is an on demand private
 *                                                      proxy server model.
 * @property isOnDemandPrivateProxyClient               True if the model is an on demand private
 *                                                      proxy client model.
 * @property isSarConfigurationServer                   True if the model is a SAR configuration
 *                                                      server model.
 * @property isSarConfigurationClient                   True if the model is a SAR configuration
 *                                                      client model.
 * @property isOpcodesAggregatorServer                  True if the model is an opcodes aggregator
 *                                                      server model.
 * @property isOpcodesAggregatorClient                  True if the model is an opcodes aggregator
 *                                                      client model.
 * @property isLargeCompositionDataServer               True if the model is a large composition
 *                                                      data server model.
 * @property isLargeCompositionDataClient               True if the model is a large composition
 *                                                      data client model.
 * @property requiresDeviceKey                          True if the model requires a device key.
 * @property supportsModelPublication                   True if the model supports model publication.
 * @property supportsModelSubscription                  True if the model supports model subscription.
 * @property directBaseModels                           List of direct base [Model]s to the Model.
 *                                                      The "Extend" relationship is explained in
 *                                                      Mesh Profile 1.0.1, chapter 2.3.6.
 *                                                      Note: Models that operate on bound states
 *                                                      share a single subscription list per element.
 *                                                      Note: Model Extension is only defined for
 *                                                      SIG Models. Currently, it is not possible to
 *                                                      get relationships between Vendor Models, and
 *                                                      for those this method returns an empty list.
 * @property baseModels                                 List of all base [Model]s extended by Model,
 *                                                      directly or indirectly.
 *                                                      The "Extend" relationship is explained in
 *                                                      Mesh Profile 1.0.1, chapter 2.3.6.
 *                                                      Note: Models that operate on bound states
 *                                                      share a single subscription list per element.
 *                                                      Note: Model Extension is only defined for
 *                                                      SIG Models. Currently, it is not possible to
 *                                                      get relationships between Vendor Models, and
 *                                                      for those this method returns an empty list.
 * @property directExtendingModels                      List of [Model]s directly extending the
 *                                                      Model. The "Extend" relationship is
 *                                                      explained in Mesh Profile 1.0.1, chapter
 *                                                      2.3.6.
 * @property extendingModels                            List of all [Model]s extending the Model,
 *                                                      directly or indirectly. The "Extend"
 *                                                      The "Extend" relationship is explained in
 *                                                      Mesh Profile 1.0.1, chapter 2.3.6.
 *                                                      Note: Models that operate on bound states
 *                                                      share a single subscription list per element.
 *                                                      Note: Model Extension is only defined for
 *                                                      SIG Models. Currently, it is not possible to
 *                                                      get relationships between Vendor Models, and
 *                                                      for those this method returns an empty list.
 * @property relatedModels                              Returns all [Model] insurances that are in a
 *                                                      hierarchy of "Extend" relationship with this
 *                                                      Model.
 *                                                      The "Extend" relationship is explained in
 *                                                      Mesh Profile 1.0.1, chapter 2.3.6.
 *                                                      Note: Models that operate on bound states
 *                                                      share a single subscription list per element.
 *                                                      Note: Model Extension is only defined for
 *                                                      SIG Models. Currently, it is not possible to
 *                                                      get relationships between Vendor Models, and
 *                                                      for those this method returns an empty list.
 * @constructor Creates a model object.
 */
@Serializable
class Model internal constructor(
    val modelId: ModelId,
    @SerialName(value = "bind")
    internal var _bind: MutableList<KeyIndex>,
    @SerialName(value = "subscribe")
    internal var _subscribe: MutableList<SubscriptionAddress>,
    @SerialName(value = "publish")
    internal var _publish: Publish? = null,
) {
    val subscribe: List<SubscriptionAddress>
        get() {
            // A model may be additionally subscribed to any special address
            // except from All Nodes and Models on the primary Element are always subscribed to the
            // All Nodes address.
            return _subscribe.takeIf{
                !it.contains(element = AllNodes) && parentElement?.isPrimary == true
            }?.let {
                _subscribe + AllNodes as SubscriptionAddress
            } ?: _subscribe
        }

    var publish: Publish?
        get() = _publish
        internal set(value) {
            _publish = value
            parentElement?.parentNode?.network?.updateTimestamp()
        }
    val bind: List<KeyIndex>
        get() = _bind
    val name: String?
        get() = nameOf(modelId)
    val isBluetoothSigAssigned: Boolean
        get() = modelId is SigModelId

    @Transient
    var parentElement: Element? = null
        internal set

    val boundApplicationKeys: List<ApplicationKey>
        get() = parentElement?.parentNode?.applicationKeys
            ?.filter { it.isBoundTo(this) }
            ?: emptyList()

    val supportsApplicationKeyBinding: Boolean
        get() = !requiresDeviceKey

    val supportsDeviceKey: Boolean
        get() = requiresDeviceKey || isOpcodesAggregatorServer || isOpcodesAggregatorClient

    val isConfigurationServer: Boolean
        get() = modelId.id == CONFIGURATION_SERVER_MODEL_ID.toUInt()
    val isConfigurationClient: Boolean
        get() = modelId.id == CONFIGURATION_CLIENT_MODEL_ID.toUInt()
    val isHealthServer: Boolean
        get() = modelId.id == HEALTH_SERVER_MODEL_ID.toUInt()
    val isHealthClient: Boolean
        get() = modelId.id == HEALTH_CLIENT_MODEL_ID.toUInt()
    val isSceneClient: Boolean
        get() = modelId.id == SCENE_CLIENT_MODEL_ID.toUInt()
    val isRemoteProvisioningServer: Boolean
        get() = modelId.id == REMOTE_PROVISIONING_SERVER_MODEL_ID.toUInt()
    val isRemoteProvisioningClient: Boolean
        get() = modelId.id == REMOTE_PROVISIONING_CLIENT_MODEL_ID.toUInt()
    val isDirectedForwardingConfigurationServer: Boolean
        get() = modelId.id == DIRECTED_FORWARDING_CONFIGURATION_SERVER_MODEL_ID.toUInt()
    val isDirectedForwardingConfigurationClient: Boolean
        get() = modelId.id == DIRECTED_FORWARDING_CONFIGURATION_CLIENT_MODEL_ID.toUInt()
    val isBridgeConfigurationServer: Boolean
        get() = modelId.id == BRIDGE_CONFIGURATION_SERVER_MODEL_ID.toUInt()
    val isBridgeConfigurationClient: Boolean
        get() = modelId.id == BRIDGE_CONFIGURATION_CLIENT_MODEL_ID.toUInt()
    val isPrivateBeaconServer: Boolean
        get() = modelId.id == PRIVATE_BEACON_SERVER_MODEL_ID.toUInt()
    val isPrivateBeaconClient: Boolean
        get() = modelId.id == PRIVATE_BEACON_CLIENT_MODEL_ID.toUInt()
    val isOnDemandPrivateProxyServer: Boolean
        get() = modelId.id == ON_DEMAND_PRIVATE_PROXY_SERVER_MODEL_ID.toUInt()
    val isOnDemandPrivateProxyClient: Boolean
        get() = modelId.id == ON_DEMAND_PRIVATE_PROXY_CLIENT_MODEL_ID.toUInt()
    val isSarConfigurationServer: Boolean
        get() = modelId.id == SAR_CONFIGURATION_SERVER_MODEL_ID.toUInt()
    val isSarConfigurationClient: Boolean
        get() = modelId.id == SAR_CONFIGURATION_CLIENT_MODEL_ID.toUInt()
    val isOpcodesAggregatorServer: Boolean
        get() = modelId.id == OP_CODES_AGGREGATOR_SERVER_MODEL_ID.toUInt()
    val isOpcodesAggregatorClient: Boolean
        get() = modelId.id == OP_CODES_AGGREGATOR_CLIENT_MODEL_ID.toUInt()
    val isLargeCompositionDataServer: Boolean
        get() = modelId.id == LARGE_COMPOSITION_DATA_SERVER_MODEL_ID.toUInt()
    val isLargeCompositionDataClient: Boolean
        get() = modelId.id == LARGE_COMPOSITION_DATA_CLIENT_MODEL_ID.toUInt()

    val requiresDeviceKey: Boolean
        get() = isConfigurationServer || isConfigurationClient ||
                isRemoteProvisioningServer || isRemoteProvisioningClient ||
                isDirectedForwardingConfigurationServer || isDirectedForwardingConfigurationClient ||
                isBridgeConfigurationServer || isBridgeConfigurationClient ||
                isPrivateBeaconServer || isPrivateBeaconClient ||
                isOnDemandPrivateProxyServer || isOnDemandPrivateProxyClient ||
                isSarConfigurationServer || isSarConfigurationClient ||
                isLargeCompositionDataServer || isLargeCompositionDataClient

    @Transient
    var eventHandler: ModelEventHandler? = null

    val supportsModelPublication: Boolean?
        get() = when ((modelId as? SigModelId)?.modelIdentifier) {
            // Foundation
            CONFIGURATION_SERVER_MODEL_ID,
            CONFIGURATION_CLIENT_MODEL_ID,
                -> false

            HEALTH_SERVER_MODEL_ID,
            HEALTH_CLIENT_MODEL_ID,
                -> true
            // Configuration models added in Mesh Protocol 1.1
            REMOTE_PROVISIONING_SERVER_MODEL_ID,
            REMOTE_PROVISIONING_CLIENT_MODEL_ID,
            DIRECTED_FORWARDING_CONFIGURATION_SERVER_MODEL_ID,
            DIRECTED_FORWARDING_CONFIGURATION_CLIENT_MODEL_ID,
            BRIDGE_CONFIGURATION_SERVER_MODEL_ID,
            BRIDGE_CONFIGURATION_CLIENT_MODEL_ID,
            PRIVATE_BEACON_SERVER_MODEL_ID,
            PRIVATE_BEACON_CLIENT_MODEL_ID,
            ON_DEMAND_PRIVATE_PROXY_SERVER_MODEL_ID,
            ON_DEMAND_PRIVATE_PROXY_CLIENT_MODEL_ID,
            SAR_CONFIGURATION_SERVER_MODEL_ID,
            SAR_CONFIGURATION_CLIENT_MODEL_ID,
            OP_CODES_AGGREGATOR_SERVER_MODEL_ID,
            OP_CODES_AGGREGATOR_CLIENT_MODEL_ID,
            LARGE_COMPOSITION_DATA_SERVER_MODEL_ID,
            LARGE_COMPOSITION_DATA_CLIENT_MODEL_ID,
            SOLICITATION_PDU_RPL_CONFIGURATION_SERVER_MODEL_ID,
            SOLICITATION_PDU_RPL_CONFIGURATION_CLIENT_MODEL_ID,
                -> false
            // Generics
            GENERIC_ON_OFF_SERVER_MODEL_ID,
            GENERIC_ON_OFF_CLIENT_MODEL_ID,
            GENERIC_LEVEL_SERVER_MODEL_ID,
            GENERIC_LEVEL_CLIENT_MODEL_ID,
            GENERIC_DEFAULT_TRANSITION_TIME_SERVER_MODEL_ID,
            GENERIC_DEFAULT_TRANSITION_TIME_CLIENT_MODEL_ID,
            GENERIC_POWER_ON_OFF_SERVER_MODEL_ID,
                -> true

            GENERIC_POWER_ON_OFF_SETUP_SERVER_MODEL_ID -> false
            GENERIC_POWER_ON_OFF_CLIENT_MODEL_ID,
            GENERIC_POWER_LEVEL_SERVER_MODEL_ID,
                -> true

            GENERIC_POWER_LEVEL_SETUP_SERVER_MODEL_ID -> false
            GENERIC_POWER_LEVEL_CLIENT_MODEL_ID,
            GENERIC_BATTERY_SERVER_MODEL_ID,
            GENERIC_BATTERY_CLIENT_MODEL_ID,
            GENERIC_LOCATION_SERVER_MODEL_ID,
                -> true

            GENERIC_LOCATION_SETUP_SERVER_MODEL_ID -> false
            GENERIC_LOCATION_CLIENT_MODEL_ID,
            GENERIC_ADMIN_PROPERTY_SERVER_MODEL_ID,
            GENERIC_MANUFACTURER_PROPERTY_SERVER_MODEL_ID,
            GENERIC_USER_PROPERTY_SERVER_MODEL_ID,
            GENERIC_CLIENT_PROPERTY_SERVER_MODEL_ID,
            GENERIC_PROPERTY_CLIENT_MODEL_ID,
                -> true
            // Sensors
            SENSOR_SERVER_MODEL_ID,
            SENSOR_SETUP_SERVER_MODEL_ID,
            SENSOR_CLIENT_MODEL_ID,
                -> true
            // Time and Scenes
            TIME_SERVER_MODEL_ID -> true
            TIME_SETUP_SERVER_MODEL_ID -> false
            TIME_CLIENT_MODEL_ID,
            SCENE_SERVER_MODEL_ID,
                -> true

            SCENE_SETUP_SERVER_MODEL_ID -> false
            SCENE_CLIENT_MODEL_ID,
            SCHEDULER_SERVER_MODEL_ID,
                -> true

            SCHEDULER_SETUP_SERVER_MODEL_ID -> false
            SCHEDULER_CLIENT_MODEL_ID -> true
            // Lighting
            LIGHT_LIGHTNESS_SERVER_MODEL_ID -> true
            LIGHT_LIGHTNESS_SETUP_SERVER_MODEL_ID -> false
            LIGHT_LIGHTNESS_CLIENT_MODEL_ID,
            LIGHT_CTL_SERVER_MODEL_ID,
                -> true

            LIGHT_CTL_SETUP_SERVER_MODEL_ID -> false
            LIGHT_CTL_CLIENT_MODEL_ID,
            LIGHT_CTL_TEMPERATURE_SERVER_MODEL_ID,
            LIGHT_HSL_SERVER_MODEL_ID,
                -> true

            LIGHT_HSL_SETUP_SERVER_MODEL_ID -> false
            LIGHT_HSL_CLIENT_MODEL_ID,
            LIGHT_HSL_HUE_SERVER_MODEL_ID,
            LIGHT_HSL_SATURATION_SERVER_MODEL_ID,
            LIGHT_XYL_SERVER_MODEL_ID,
                -> true

            LIGHT_XYL_SETUP_SERVER_MODEL_ID -> false
            LIGHT_XYL_CLIENT_MODEL_ID,
            LIGHT_LC_SERVER_MODEL_ID,
            LIGHT_LC_SETUP_SERVER_MODEL_ID,
            LIGHT_LC_CLIENT_MODEL_ID,
                -> true

            else -> null
        }

    val supportsModelSubscription: Boolean?
        get() = when ((modelId as? SigModelId)?.modelIdentifier) {
            // Foundation
            CONFIGURATION_SERVER_MODEL_ID,
            CONFIGURATION_CLIENT_MODEL_ID,
                -> false

            HEALTH_SERVER_MODEL_ID,
            HEALTH_CLIENT_MODEL_ID,
                -> true
            // Configuration models added in Mesh Protocol 1.1
            REMOTE_PROVISIONING_SERVER_MODEL_ID,
            REMOTE_PROVISIONING_CLIENT_MODEL_ID,
            DIRECTED_FORWARDING_CONFIGURATION_SERVER_MODEL_ID,
            DIRECTED_FORWARDING_CONFIGURATION_CLIENT_MODEL_ID,
            BRIDGE_CONFIGURATION_SERVER_MODEL_ID,
            BRIDGE_CONFIGURATION_CLIENT_MODEL_ID,
            PRIVATE_BEACON_SERVER_MODEL_ID,
            PRIVATE_BEACON_CLIENT_MODEL_ID,
            ON_DEMAND_PRIVATE_PROXY_SERVER_MODEL_ID,
            ON_DEMAND_PRIVATE_PROXY_CLIENT_MODEL_ID,
            SAR_CONFIGURATION_SERVER_MODEL_ID,
            SAR_CONFIGURATION_CLIENT_MODEL_ID,
            OP_CODES_AGGREGATOR_SERVER_MODEL_ID,
            OP_CODES_AGGREGATOR_CLIENT_MODEL_ID,
            LARGE_COMPOSITION_DATA_SERVER_MODEL_ID,
            LARGE_COMPOSITION_DATA_CLIENT_MODEL_ID,
            SOLICITATION_PDU_RPL_CONFIGURATION_SERVER_MODEL_ID,
            SOLICITATION_PDU_RPL_CONFIGURATION_CLIENT_MODEL_ID,
                -> false
            // Generics
            GENERIC_ON_OFF_SERVER_MODEL_ID,
            GENERIC_ON_OFF_CLIENT_MODEL_ID,
            GENERIC_LEVEL_SERVER_MODEL_ID,
            GENERIC_LEVEL_CLIENT_MODEL_ID,
            GENERIC_DEFAULT_TRANSITION_TIME_SERVER_MODEL_ID,
            GENERIC_DEFAULT_TRANSITION_TIME_CLIENT_MODEL_ID,
            GENERIC_POWER_ON_OFF_SERVER_MODEL_ID,
            GENERIC_POWER_ON_OFF_SETUP_SERVER_MODEL_ID,
            GENERIC_POWER_ON_OFF_CLIENT_MODEL_ID,
            GENERIC_POWER_LEVEL_SERVER_MODEL_ID,
            GENERIC_POWER_LEVEL_SETUP_SERVER_MODEL_ID,
            GENERIC_POWER_LEVEL_CLIENT_MODEL_ID,
            GENERIC_BATTERY_SERVER_MODEL_ID,
            GENERIC_BATTERY_CLIENT_MODEL_ID,
            GENERIC_LOCATION_SERVER_MODEL_ID,
            GENERIC_LOCATION_SETUP_SERVER_MODEL_ID,
            GENERIC_LOCATION_CLIENT_MODEL_ID,
            GENERIC_ADMIN_PROPERTY_SERVER_MODEL_ID,
            GENERIC_MANUFACTURER_PROPERTY_SERVER_MODEL_ID,
            GENERIC_USER_PROPERTY_SERVER_MODEL_ID,
            GENERIC_CLIENT_PROPERTY_SERVER_MODEL_ID,
            GENERIC_PROPERTY_CLIENT_MODEL_ID,
                -> true
            // Sensors
            SENSOR_SERVER_MODEL_ID,
            SENSOR_SETUP_SERVER_MODEL_ID,
            SENSOR_CLIENT_MODEL_ID,
                -> true
            // Time and Scenes
            TIME_SERVER_MODEL_ID,
            TIME_SETUP_SERVER_MODEL_ID,
            TIME_CLIENT_MODEL_ID,
            SCENE_SERVER_MODEL_ID,
            SCENE_SETUP_SERVER_MODEL_ID,
            SCENE_CLIENT_MODEL_ID,
            SCHEDULER_SERVER_MODEL_ID,
            SCHEDULER_SETUP_SERVER_MODEL_ID,
            SCHEDULER_CLIENT_MODEL_ID,
                -> true
            // Lighting
            LIGHT_LIGHTNESS_SERVER_MODEL_ID,
            LIGHT_LIGHTNESS_SETUP_SERVER_MODEL_ID,
            LIGHT_LIGHTNESS_CLIENT_MODEL_ID,
            LIGHT_CTL_SERVER_MODEL_ID,
            LIGHT_CTL_SETUP_SERVER_MODEL_ID,
            LIGHT_CTL_CLIENT_MODEL_ID,
            LIGHT_CTL_TEMPERATURE_SERVER_MODEL_ID,
            LIGHT_HSL_SERVER_MODEL_ID,
            LIGHT_HSL_SETUP_SERVER_MODEL_ID,
            LIGHT_HSL_CLIENT_MODEL_ID,
            LIGHT_HSL_HUE_SERVER_MODEL_ID,
            LIGHT_HSL_SATURATION_SERVER_MODEL_ID,
            LIGHT_XYL_SERVER_MODEL_ID,
            LIGHT_XYL_SETUP_SERVER_MODEL_ID,
            LIGHT_XYL_CLIENT_MODEL_ID,
            LIGHT_LC_SERVER_MODEL_ID,
            LIGHT_LC_SETUP_SERVER_MODEL_ID,
            LIGHT_LC_CLIENT_MODEL_ID,
                -> true

            else -> null
        }

    val directBaseModels: List<Model>
        get() = parentElement?.let { parentElement ->
            // Get all direct base models of this Model.
            parentElement.parentNode?.elements
                // Look only on that and previous Elements.
                // Models can't extend Models on Elements with higher index.
                ?.filter { it.index < parentElement.index }
                // Sort in reverse order so that unifying the list will
                // remove those on Elements with the lowest indexes.
                ?.sortedByDescending { it.index }
                // Get a list of all models.
                ?.flatMap { it.models }
                // Remove duplicates.
                ?.distinctBy { it.modelId }
                // Get all direct base models of this Model.
                ?.filter { extendsDirectly(model = it) }
                ?: emptyList()
        } ?: emptyList()

    val baseModels: List<Model>
        get() = directBaseModels.let { models ->
            models + models.flatMap { it.baseModels }
        }

    val directExtendingModels: List<Model>
        get() = parentElement?.let { parentElement ->
            // Get all models directly extending this Model.
            parentElement.parentNode?.elements
                // Look only on that and next Elements.
                // Models can't be extended by Models on Elements with lower index.
                ?.filter { it.index > parentElement.index }
                // Get a list of all models.
                ?.flatMap { it.models }
                // Remove duplicates.
                ?.distinctBy { it.modelId }
                // Get all models directly extending this Model.
                ?.filter { it.extendsDirectly(model = this) }
                ?: emptyList()
        } ?: emptyList()

    val extendingModels: List<Model>
        get() = directExtendingModels.let { models ->
            models + models.flatMap { it.extendingModels }
        }

    val relatedModels: List<Model>
        get() {
            // The Model must be on an Element on a Node.
            val parentElement = this.parentElement ?: return emptyList()
            val node = parentElement.parentNode ?: return emptyList()

            // Get a list of all models on the Node.
            val models = node.elements.flatMap { it.models }

            val result = mutableListOf<Model>()
            val queue = ArrayDeque<Model>()
            queue.add(this)

            while (queue.isNotEmpty()) {
                val currentModel = queue.removeFirst()
                if (!result.contains(currentModel)) {
                    if (currentModel != this) {
                        result.add(currentModel)
                    }
                    // Models that directly extend currentModel
                    val directlyExtendedModels = models.filter { it.extendsDirectly(currentModel) }
                    queue.addAll(directlyExtendedModels)
                    // Models that are directly extended by currentModel
                    val extendedByModels = models.filter { currentModel.extendsDirectly(it) }
                    queue.addAll(extendedByModels)
                }
            }

            return result.sortedWith(
                compareBy(
                    { it.parentElement!!.index },
                    { it.modelId.id }
                )
            )
        }

    /**
     * Constructs a Model
     *
     * @param modelId Model ID.
     * @param handler Model event handler.
     */
    constructor(modelId: ModelId, handler: ModelEventHandler? = null) : this(
        modelId = modelId,
        _bind = mutableListOf(),
        _subscribe = mutableListOf(),
        _publish = null
    ) {
        eventHandler = handler
    }

    override fun toString(): String {
        return "Model(modelId: $modelId, " +
                "bind: $_bind, " +
                "subscribe: $_subscribe, " +
                "publish: $_publish, " +
                "name: '$name', " +
                "isBluetoothSigAssigned: $isBluetoothSigAssigned, " +
                "parentElement: ${parentElement?.index})"
    }

    /**
     * Binds the given application key index to a model.
     *
     * @param index Application key index.
     * @return true if the key index is bound or false if it's already bound.
     */
    internal fun bind(index: KeyIndex) = when (bind.contains(element = index)) {
        true -> false
        else -> _bind.add(index)
    }

    /**
     * Assigns the application keys bound to this Model. This is invoked when handling
     * ConfigModelAppList messages
     *
     * @param indexes bound application key indexes
     */
    internal fun bind(indexes: List<KeyIndex>) {
        _bind = indexes.toMutableList()
    }

    /**
     * Unbinds the given application key index from a model and clears any publication that was using
     * the same key.
     *
     * @param index Application key index.
     * @return true if the key index is unbound or false if it's already unbound.
     */
    internal fun unbind(index: KeyIndex) = when (bind.contains(element = index)) {
        true -> _bind.remove(element = index).also {
            if (publish?.index == index) _publish = null
        }

        else -> false
    }

    /**
     * Sets the [Publish] settings for this model.
     *
     * @param publish Publish settings.
     */
    internal fun set(publish: Publish?) {
        this._publish = publish
    }

    /**
     * Clears any publication that was assigned to this model.
     */
    internal fun clearPublication() {
        _publish = null
    }

    /**
     * Copies the properties from the given model
     *
     * @param model Model to copy from
     */
    fun copyProperties(model: Model) {
        _bind = model._bind
        _publish = model._publish
        _subscribe = model._subscribe
    }

    /**
     * Checks if the Model is subscribed to the given address.
     *
     * @param address Address to check.
     * @return true if the model is subscribed to the address or false otherwise.
     */
    fun isSubscribedTo(address: SubscriptionAddress) = subscribe.any { it == address }

    /**
     * Checks if the Model is subscribed to the given Group.
     *
     * @param group Group to check.
     * @return true if the model is subscribed to the group or false otherwise.
     */
    fun isSubscribedTo(group: Group) = isSubscribedTo(group.address)

    /**
     * Checks if the Model is subscribed to the given address.
     *
     * @param address Address to check.
     * @return true if the model is subscribed to the address or false otherwise.
     */
    fun isSubscribedTo(address: PrimaryGroupAddress) = subscribe.any { it == address }

    /**
     * Subscribe this model to a given subscription address.
     *
     * @param address Subscription address to be added.
     * @return true if the address is added or false if the address is already exists in the list.
     */
    internal fun subscribe(address: SubscriptionAddress) =
        when (isSubscribedTo(address = address)) {
            true -> false
            false -> {
                _subscribe.add(address)
                parentElement?.parentNode?.network?.updateTimestamp()
                true
            }
        }

    /**
     * Subscribes the model to the given group.
     *
     * @param group Group to subscribe.
     */
    fun subscribe(group: Group) {
        subscribe(address = group.address as SubscriptionAddress)
    }

    /**
     * Removes the given group address from the subscription list.
     *
     * @param group Group to remove.
     */
    fun unsubscribe(group: Group) {
        unsubscribe(address = group.address.address)
    }

    /**
     * Removes the given address from the subscription list.
     *
     * @param address Address to remove.
     */
    fun unsubscribe(address: Address) {
        _subscribe
            .indexOfFirst { it.address == address }
            .takeIf { it > -1 }
            ?.let {
                _subscribe.removeAt(index = it)
                parentElement?.parentNode?.network?.updateTimestamp()
            }
    }

    /**
     * Removes all subscription from this Model.
     */
    fun unsubscribeFromAll() {
        _subscribe.clear()
        parentElement?.parentNode?.network?.updateTimestamp()
    }

    /**
     * Returns whether a model extends the given model directly or indirectly.
     *
     * If a Model A extends B, which extends C, this method will return "true" if checked with A and C.
     * Base Models may exist on the same Element or on an element with a lower index.
     *
     * The "Extends" relationship is defined in the Mesh Profile 1.0.1, chapter 2.3.6
     *
     * Note: Models in Extend relationship share their Subscription List if they are on the same
     * Element.
     *
     * Note: Model extension is only defined for SIG Models. Currently, it is not possible to get
     * relationships between Vendor Models and will always return false.
     *
     * @param model Model to check if this model extends.
     * @return true if this model extends the given model, false otherwise.
     */
    @OptIn(ExperimentalUuidApi::class)
    fun extends(model: Model): Boolean {
        val parentElement = parentElement ?: return false
        val otherParentElement = model.parentElement ?: return false
        val node = parentElement.parentNode ?: return false
        val otherNode = otherParentElement.parentNode ?: return false
        if (node.uuid == otherNode.uuid) return false
        return baseModels.contains(model)
    }

    /**
     * Returns whether a model extends the given model.
     *
     * This method only checks direct Extend relationship, not hierarchical. If a Model A extends B,
     * which extends C, this method will return false for A extends C. Base Models may exist on
     * the same Element or on an element with a lower index.
     *
     * The "Extends" relationship is defined in the Mesh Profile 1.0.1, chapter 2.3.6
     *
     * Note: Models in Extend relationship share their Subscription List if they are ont eh same
     * Element.
     *
     * Note: Model extension is only defined for SIG Models. Currently, it is not possible ot get
     * relationships between Vendor Models and will always return false.
     *
     * @param model Model to check if this model extends.
     * @return true if this model extends the given model, false otherwise.
     */
    @OptIn(ExperimentalUuidApi::class)
    fun extendsDirectly(model: Model): Boolean {
        // Ensure models are on the same node
        val parentElement = parentElement ?: return false
        val otherParentElement = model.parentElement ?: return false
        val node = parentElement.parentNode ?: return false
        val otherNode = otherParentElement.parentNode ?: return false
        if (node.uuid == otherNode.uuid) return false

        // Ensure model does not extend itself or any other instance of the same model
        if (model.modelId == modelId) return false

        // Currently, it is not possible to get relationships between vendor models
        if (modelId as? SigModelId == null || model.modelId as? SigModelId == null) return false

        return if (model.parentElement == parentElement) {
            when (modelId.modelIdentifier) {
                DIRECTED_FORWARDING_CONFIGURATION_SERVER_MODEL_ID,
                BRIDGE_CONFIGURATION_SERVER_MODEL_ID,
                PRIVATE_BEACON_SERVER_MODEL_ID,
                LARGE_COMPOSITION_DATA_SERVER_MODEL_ID,
                    ->
                    model.modelId.modelIdentifier == CONFIGURATION_SERVER_MODEL_ID

                ON_DEMAND_PRIVATE_PROXY_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == PRIVATE_BEACON_SERVER_MODEL_ID
                // Generics
                GENERIC_POWER_ON_OFF_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == GENERIC_ON_OFF_SERVER_MODEL_ID

                GENERIC_POWER_ON_OFF_SETUP_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == GENERIC_POWER_ON_OFF_SERVER_MODEL_ID ||
                            model.modelId.modelIdentifier == GENERIC_DEFAULT_TRANSITION_TIME_SERVER_MODEL_ID

                GENERIC_POWER_LEVEL_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == GENERIC_POWER_ON_OFF_SERVER_MODEL_ID ||
                            model.modelId.modelIdentifier == GENERIC_LEVEL_SERVER_MODEL_ID

                GENERIC_POWER_LEVEL_SETUP_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == GENERIC_POWER_LEVEL_SERVER_MODEL_ID ||
                            model.modelId.modelIdentifier == GENERIC_POWER_ON_OFF_SETUP_SERVER_MODEL_ID

                GENERIC_LOCATION_SETUP_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == GENERIC_LOCATION_SERVER_MODEL_ID

                GENERIC_ADMIN_PROPERTY_SERVER_MODEL_ID, GENERIC_MANUFACTURER_PROPERTY_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == GENERIC_PROPERTY_CLIENT_MODEL_ID
                // Sensors
                SENSOR_SETUP_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == SENSOR_SERVER_MODEL_ID
                // Time and Scenes
                TIME_SETUP_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == TIME_SERVER_MODEL_ID

                SCENE_SETUP_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == SCENE_SERVER_MODEL_ID ||
                            model.modelId.modelIdentifier == GENERIC_DEFAULT_TRANSITION_TIME_SERVER_MODEL_ID

                SCHEDULER_SETUP_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == SCHEDULER_SERVER_MODEL_ID
                // Lighting
                LIGHT_LIGHTNESS_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == GENERIC_POWER_ON_OFF_SERVER_MODEL_ID ||
                            model.modelId.modelIdentifier == GENERIC_LEVEL_SERVER_MODEL_ID

                LIGHT_LIGHTNESS_SETUP_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == LIGHT_LIGHTNESS_SERVER_MODEL_ID ||
                            model.modelId.modelIdentifier == GENERIC_POWER_ON_OFF_SETUP_SERVER_MODEL_ID

                LIGHT_CTL_SERVER_MODEL_ID, LIGHT_HSL_SERVER_MODEL_ID, LIGHT_XYL_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == LIGHT_LIGHTNESS_SERVER_MODEL_ID

                LIGHT_CTL_TEMPERATURE_SERVER_MODEL_ID,
                LIGHT_HSL_HUE_SERVER_MODEL_ID,
                LIGHT_HSL_SATURATION_SERVER_MODEL_ID,
                    ->
                    model.modelId.modelIdentifier == GENERIC_LEVEL_SERVER_MODEL_ID

                LIGHT_CTL_SETUP_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == LIGHT_CTL_SERVER_MODEL_ID ||
                            model.modelId.modelIdentifier == GENERIC_POWER_ON_OFF_SETUP_SERVER_MODEL_ID

                LIGHT_HSL_SETUP_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == LIGHT_HSL_SERVER_MODEL_ID ||
                            model.modelId.modelIdentifier == GENERIC_POWER_ON_OFF_SETUP_SERVER_MODEL_ID

                LIGHT_XYL_SETUP_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == LIGHT_XYL_SERVER_MODEL_ID ||
                            model.modelId.modelIdentifier == LIGHT_LIGHTNESS_SETUP_SERVER_MODEL_ID

                LIGHT_LC_SERVER_MODEL_ID ->
                    // It also extends a Light Lightness Server on another element
                    model.modelId.modelIdentifier == GENERIC_ON_OFF_SERVER_MODEL_ID

                LIGHT_LC_SETUP_SERVER_MODEL_ID ->
                    model.modelId.modelIdentifier == LIGHT_LC_SERVER_MODEL_ID

                FIRMWARE_UPDATE_SERVER_MODEL_ID,
                FIRMWARE_DISTRIBUTION_SERVER_MODEL_ID,
                    ->
                    model.modelId.modelIdentifier == BLOB_TRANSFER_SERVER_MODEL_ID

                else -> false
            }
        } else {
            // Some features are split into two elements.
            when (modelId.modelIdentifier) {
                // Light LC Server Model extends Light Lightness Server Model that cannot be on the
                // same Element. Search for a Model on an Element with lower index
                LIGHT_LC_SERVER_MODEL_ID -> {
                    val lightLightnessServerModelId = SigModelId(
                        modelIdentifier = LIGHT_LIGHTNESS_SERVER_MODEL_ID
                    )
                    val lightLightnessServer = node.elements
                        .filter { it.index < parentElement.index }
                        .sortedWith { first, second -> first.index.compareTo(second.index) }
                        .firstOrNull {
                            it.contains(sigModelId = lightLightnessServerModelId)
                        }?.model(modelId = lightLightnessServerModelId)

                    model == lightLightnessServer
                }

                else -> false
            }
        }
    }

    companion object {

        const val CONFIGURATION_SERVER_MODEL_ID: UShort = 0x0000u
        const val CONFIGURATION_CLIENT_MODEL_ID: UShort = 0x0001u
        const val HEALTH_SERVER_MODEL_ID: UShort = 0x0002u
        const val HEALTH_CLIENT_MODEL_ID: UShort = 0x0003u

        // Configuration models added in Mesh Protocol 1.1
        const val REMOTE_PROVISIONING_SERVER_MODEL_ID: UShort = 0x0004u
        const val REMOTE_PROVISIONING_CLIENT_MODEL_ID: UShort = 0x0005u
        const val DIRECTED_FORWARDING_CONFIGURATION_SERVER_MODEL_ID: UShort = 0x0006u
        const val DIRECTED_FORWARDING_CONFIGURATION_CLIENT_MODEL_ID: UShort = 0x0007u
        const val BRIDGE_CONFIGURATION_SERVER_MODEL_ID: UShort = 0x0008u
        const val BRIDGE_CONFIGURATION_CLIENT_MODEL_ID: UShort = 0x0009u
        const val PRIVATE_BEACON_SERVER_MODEL_ID: UShort = 0x000Au
        const val PRIVATE_BEACON_CLIENT_MODEL_ID: UShort = 0x000Bu
        const val ON_DEMAND_PRIVATE_PROXY_SERVER_MODEL_ID: UShort = 0x000Cu
        const val ON_DEMAND_PRIVATE_PROXY_CLIENT_MODEL_ID: UShort = 0x000Du
        const val SAR_CONFIGURATION_SERVER_MODEL_ID: UShort = 0x000Eu
        const val SAR_CONFIGURATION_CLIENT_MODEL_ID: UShort = 0x000Fu
        const val OP_CODES_AGGREGATOR_SERVER_MODEL_ID: UShort = 0x0010u
        const val OP_CODES_AGGREGATOR_CLIENT_MODEL_ID: UShort = 0x0011u
        const val LARGE_COMPOSITION_DATA_SERVER_MODEL_ID: UShort = 0x0012u
        const val LARGE_COMPOSITION_DATA_CLIENT_MODEL_ID: UShort = 0x0013u
        const val SOLICITATION_PDU_RPL_CONFIGURATION_SERVER_MODEL_ID: UShort = 0x0014u
        const val SOLICITATION_PDU_RPL_CONFIGURATION_CLIENT_MODEL_ID: UShort = 0x0015u

        // Generics
        const val GENERIC_ON_OFF_SERVER_MODEL_ID: UShort = 0x1000u
        const val GENERIC_ON_OFF_CLIENT_MODEL_ID: UShort = 0x1001u
        const val GENERIC_LEVEL_SERVER_MODEL_ID: UShort = 0x1002u
        const val GENERIC_LEVEL_CLIENT_MODEL_ID: UShort = 0x1003u
        const val GENERIC_DEFAULT_TRANSITION_TIME_SERVER_MODEL_ID: UShort = 0x1004u
        const val GENERIC_DEFAULT_TRANSITION_TIME_CLIENT_MODEL_ID: UShort = 0x1005u
        const val GENERIC_POWER_ON_OFF_SERVER_MODEL_ID: UShort = 0x1006u
        const val GENERIC_POWER_ON_OFF_SETUP_SERVER_MODEL_ID: UShort = 0x1007u
        const val GENERIC_POWER_ON_OFF_CLIENT_MODEL_ID: UShort = 0x1008u
        const val GENERIC_POWER_LEVEL_SERVER_MODEL_ID: UShort = 0x1009u
        const val GENERIC_POWER_LEVEL_SETUP_SERVER_MODEL_ID: UShort = 0x100Au
        const val GENERIC_POWER_LEVEL_CLIENT_MODEL_ID: UShort = 0x100Bu
        const val GENERIC_BATTERY_SERVER_MODEL_ID: UShort = 0x100Cu
        const val GENERIC_BATTERY_CLIENT_MODEL_ID: UShort = 0x100Du
        const val GENERIC_LOCATION_SERVER_MODEL_ID: UShort = 0x100Eu
        const val GENERIC_LOCATION_SETUP_SERVER_MODEL_ID: UShort = 0x100Fu
        const val GENERIC_LOCATION_CLIENT_MODEL_ID: UShort = 0x1010u
        const val GENERIC_ADMIN_PROPERTY_SERVER_MODEL_ID: UShort = 0x1011u
        const val GENERIC_MANUFACTURER_PROPERTY_SERVER_MODEL_ID: UShort = 0x1012u
        const val GENERIC_USER_PROPERTY_SERVER_MODEL_ID: UShort = 0x1013u
        const val GENERIC_CLIENT_PROPERTY_SERVER_MODEL_ID: UShort = 0x1014u
        const val GENERIC_PROPERTY_CLIENT_MODEL_ID: UShort = 0x1015u

        // Sensors
        const val SENSOR_SERVER_MODEL_ID: UShort = 0x1100u
        const val SENSOR_SETUP_SERVER_MODEL_ID: UShort = 0x1101u
        const val SENSOR_CLIENT_MODEL_ID: UShort = 0x1102u

        // Time and Scenes
        const val TIME_SERVER_MODEL_ID: UShort = 0x1200u
        const val TIME_SETUP_SERVER_MODEL_ID: UShort = 0x1201u
        const val TIME_CLIENT_MODEL_ID: UShort = 0x1202u
        const val SCENE_SERVER_MODEL_ID: UShort = 0x1203u
        const val SCENE_SETUP_SERVER_MODEL_ID: UShort = 0x1204u
        const val SCENE_CLIENT_MODEL_ID: UShort = 0x1205u
        const val SCHEDULER_SERVER_MODEL_ID: UShort = 0x1206u
        const val SCHEDULER_SETUP_SERVER_MODEL_ID: UShort = 0x1207u
        const val SCHEDULER_CLIENT_MODEL_ID: UShort = 0x1208u

        // Lighting
        const val LIGHT_LIGHTNESS_SERVER_MODEL_ID: UShort = 0x1300u
        const val LIGHT_LIGHTNESS_SETUP_SERVER_MODEL_ID: UShort = 0x1301u
        const val LIGHT_LIGHTNESS_CLIENT_MODEL_ID: UShort = 0x1302u
        const val LIGHT_CTL_SERVER_MODEL_ID: UShort = 0x1303u
        const val LIGHT_CTL_SETUP_SERVER_MODEL_ID: UShort = 0x1304u
        const val LIGHT_CTL_CLIENT_MODEL_ID: UShort = 0x1305u
        const val LIGHT_CTL_TEMPERATURE_SERVER_MODEL_ID: UShort = 0x1306u
        const val LIGHT_HSL_SERVER_MODEL_ID: UShort = 0x1307u
        const val LIGHT_HSL_SETUP_SERVER_MODEL_ID: UShort = 0x1308u
        const val LIGHT_HSL_CLIENT_MODEL_ID: UShort = 0x1309u
        const val LIGHT_HSL_HUE_SERVER_MODEL_ID: UShort = 0x130Au
        const val LIGHT_HSL_SATURATION_SERVER_MODEL_ID: UShort = 0x130Bu
        const val LIGHT_XYL_SERVER_MODEL_ID: UShort = 0x130Cu
        const val LIGHT_XYL_SETUP_SERVER_MODEL_ID: UShort = 0x130Du
        const val LIGHT_XYL_CLIENT_MODEL_ID: UShort = 0x130Eu
        const val LIGHT_LC_SERVER_MODEL_ID: UShort = 0x130Fu
        const val LIGHT_LC_SETUP_SERVER_MODEL_ID: UShort = 0x1310u
        const val LIGHT_LC_CLIENT_MODEL_ID: UShort = 0x1311u

        // BLOB Transfer
        const val BLOB_TRANSFER_SERVER_MODEL_ID: UShort = 0x1400u
        const val BLOB_TRANSFER_CLIENT_MODEL_ID: UShort = 0x1401u

        // Device Firmware Update
        const val FIRMWARE_UPDATE_SERVER_MODEL_ID: UShort = 0x1402u
        const val FIRMWARE_UPDATE_CLIENT_MODEL_ID: UShort = 0x1403u
        const val FIRMWARE_DISTRIBUTION_SERVER_MODEL_ID: UShort = 0x1404u
        const val FIRMWARE_DISTRIBUTION_CLIENT_MODEL_ID: UShort = 0x1405u

        /**
         * Returns the name of the model for a given model id.
         *
         * @param modelId Model ID
         * @return name of the model
         */
        private fun nameOf(modelId: ModelId): String? =
            if (!modelId.isBluetoothSigAssigned) "Vendor Model"
            else when (modelId.id) {
                // Foundation
                CONFIGURATION_SERVER_MODEL_ID.toUInt() -> "Configuration Server"
                CONFIGURATION_CLIENT_MODEL_ID.toUInt() -> "Configuration Client"
                HEALTH_SERVER_MODEL_ID.toUInt() -> "Health Server"
                HEALTH_CLIENT_MODEL_ID.toUInt() -> "Health Client"
                // Configuration models added in Mesh Protocol 1.1
                REMOTE_PROVISIONING_SERVER_MODEL_ID.toUInt() -> "Remote Provisioning Server"
                REMOTE_PROVISIONING_CLIENT_MODEL_ID.toUInt() -> "Remote Provisioning Client"
                DIRECTED_FORWARDING_CONFIGURATION_SERVER_MODEL_ID.toUInt() -> "Directed Forwarding Configuration Server"
                DIRECTED_FORWARDING_CONFIGURATION_CLIENT_MODEL_ID.toUInt() -> "Directed Forwarding Configuration Client"
                BRIDGE_CONFIGURATION_SERVER_MODEL_ID.toUInt() -> "Bridge Configuration Server"
                BRIDGE_CONFIGURATION_CLIENT_MODEL_ID.toUInt() -> "Bridge Configuration Client"
                PRIVATE_BEACON_SERVER_MODEL_ID.toUInt() -> "Private Beacon Server"
                PRIVATE_BEACON_CLIENT_MODEL_ID.toUInt() -> "Private Beacon Client"
                ON_DEMAND_PRIVATE_PROXY_SERVER_MODEL_ID.toUInt() -> "On-Demand Private Proxy Server"
                ON_DEMAND_PRIVATE_PROXY_CLIENT_MODEL_ID.toUInt() -> "On-Demand Private Proxy Client"
                SAR_CONFIGURATION_SERVER_MODEL_ID.toUInt() -> "SAR Configuration Server"
                SAR_CONFIGURATION_CLIENT_MODEL_ID.toUInt() -> "SAR Configuration Client"
                OP_CODES_AGGREGATOR_SERVER_MODEL_ID.toUInt() -> "Op Codes Aggregator Server"
                OP_CODES_AGGREGATOR_CLIENT_MODEL_ID.toUInt() -> "Op Codes Aggregator Client"
                LARGE_COMPOSITION_DATA_SERVER_MODEL_ID.toUInt() -> "Large Composition Data Server"
                LARGE_COMPOSITION_DATA_CLIENT_MODEL_ID.toUInt() -> "Large Composition Data Client"
                SOLICITATION_PDU_RPL_CONFIGURATION_SERVER_MODEL_ID.toUInt() -> "Solicitation PDU RPL Configuration Server"
                SOLICITATION_PDU_RPL_CONFIGURATION_CLIENT_MODEL_ID.toUInt() -> "Solicitation PDU RPL Configuration Client"
                // Generic
                GENERIC_ON_OFF_SERVER_MODEL_ID.toUInt() -> "Generic OnOff Server"
                GENERIC_ON_OFF_CLIENT_MODEL_ID.toUInt() -> "Generic OnOff Client"
                GENERIC_LEVEL_SERVER_MODEL_ID.toUInt() -> "Generic Level Server"
                GENERIC_LEVEL_CLIENT_MODEL_ID.toUInt() -> "Generic Level Client"
                GENERIC_DEFAULT_TRANSITION_TIME_SERVER_MODEL_ID.toUInt() -> "Generic Default Transition Time Server"
                GENERIC_DEFAULT_TRANSITION_TIME_CLIENT_MODEL_ID.toUInt() -> "Generic Default Transition Time Client"
                GENERIC_POWER_ON_OFF_SERVER_MODEL_ID.toUInt() -> "Generic Power OnOff Server"
                GENERIC_POWER_ON_OFF_SETUP_SERVER_MODEL_ID.toUInt() -> "Generic Power OnOff Setup Server"
                GENERIC_POWER_ON_OFF_CLIENT_MODEL_ID.toUInt() -> "Generic Power OnOff Client"
                GENERIC_POWER_LEVEL_SERVER_MODEL_ID.toUInt() -> "Generic Power Level Server"
                GENERIC_POWER_LEVEL_SETUP_SERVER_MODEL_ID.toUInt() -> "Generic Power Level Setup Server"
                GENERIC_POWER_LEVEL_CLIENT_MODEL_ID.toUInt() -> "Generic Power Level Client"
                GENERIC_BATTERY_SERVER_MODEL_ID.toUInt() -> "Generic Battery Server"
                GENERIC_BATTERY_CLIENT_MODEL_ID.toUInt() -> "Generic Battery Client"
                GENERIC_LOCATION_SERVER_MODEL_ID.toUInt() -> "Generic Location Server"
                GENERIC_LOCATION_SETUP_SERVER_MODEL_ID.toUInt() -> "Generic Location Setup Server"
                GENERIC_LOCATION_CLIENT_MODEL_ID.toUInt() -> "Generic Location Client"
                GENERIC_ADMIN_PROPERTY_SERVER_MODEL_ID.toUInt() -> "Generic Admin Property Server"
                GENERIC_MANUFACTURER_PROPERTY_SERVER_MODEL_ID.toUInt() -> "Generic Manufacturer Property Server"
                GENERIC_USER_PROPERTY_SERVER_MODEL_ID.toUInt() -> "Generic User Property Server"
                GENERIC_CLIENT_PROPERTY_SERVER_MODEL_ID.toUInt() -> "Generic Client Property Server"
                GENERIC_PROPERTY_CLIENT_MODEL_ID.toUInt() -> "Generic Property Client"
                // Sensors
                SENSOR_SERVER_MODEL_ID.toUInt() -> "Sensor Server"
                SENSOR_SETUP_SERVER_MODEL_ID.toUInt() -> "Sensor Setup Server"
                SENSOR_CLIENT_MODEL_ID.toUInt() -> "Sensor Client"
                // Time and Scenes
                TIME_SERVER_MODEL_ID.toUInt() -> "Time Server"
                TIME_SETUP_SERVER_MODEL_ID.toUInt() -> "Time Setup Server"
                TIME_CLIENT_MODEL_ID.toUInt() -> "Time Client"
                SCENE_SERVER_MODEL_ID.toUInt() -> "Scene Server"
                SCENE_SETUP_SERVER_MODEL_ID.toUInt() -> "Scene Setup Server"
                SCENE_CLIENT_MODEL_ID.toUInt() -> "Scene Client"
                SCHEDULER_SERVER_MODEL_ID.toUInt() -> "Scheduler Server"
                SCHEDULER_SETUP_SERVER_MODEL_ID.toUInt() -> "Scheduler Setup Server"
                SCHEDULER_CLIENT_MODEL_ID.toUInt() -> "Scheduler Client"
                // Lighting
                LIGHT_LIGHTNESS_SERVER_MODEL_ID.toUInt() -> "Light Lightness Server"
                LIGHT_LIGHTNESS_SETUP_SERVER_MODEL_ID.toUInt() -> "Light Lightness Setup Server"
                LIGHT_LIGHTNESS_CLIENT_MODEL_ID.toUInt() -> "Light Lightness Client"
                LIGHT_CTL_SERVER_MODEL_ID.toUInt() -> "Light CTL Server"
                LIGHT_CTL_SETUP_SERVER_MODEL_ID.toUInt() -> "Light CTL Setup Server"
                LIGHT_CTL_CLIENT_MODEL_ID.toUInt() -> "Light CTL Client"
                LIGHT_CTL_TEMPERATURE_SERVER_MODEL_ID.toUInt() -> "Light CTL Temperature Server"
                LIGHT_HSL_SERVER_MODEL_ID.toUInt() -> "Light HSL Server"
                LIGHT_HSL_SETUP_SERVER_MODEL_ID.toUInt() -> "Light HSL Setup Server "
                LIGHT_HSL_CLIENT_MODEL_ID.toUInt() -> "Light HSL Client"
                LIGHT_HSL_HUE_SERVER_MODEL_ID.toUInt() -> "Light HSL Hue Server"
                LIGHT_HSL_SATURATION_SERVER_MODEL_ID.toUInt() -> "Light HSL Saturation Server"
                LIGHT_XYL_SERVER_MODEL_ID.toUInt() -> "Light xyL Server"
                LIGHT_XYL_SETUP_SERVER_MODEL_ID.toUInt() -> "Light xyL Setup Server"
                LIGHT_XYL_CLIENT_MODEL_ID.toUInt() -> "Light xyL Client"
                LIGHT_LC_SERVER_MODEL_ID.toUInt() -> "Light LC Server"
                LIGHT_LC_SETUP_SERVER_MODEL_ID.toUInt() -> "Light LC Setup Server"
                LIGHT_LC_CLIENT_MODEL_ID.toUInt() -> "Light LC Client"
                // BLOB Transfer
                BLOB_TRANSFER_SERVER_MODEL_ID.toUInt() -> "BLOB Transfer Server"
                BLOB_TRANSFER_CLIENT_MODEL_ID.toUInt() -> "BLOB Transfer Client"
                // Firmware Update
                FIRMWARE_UPDATE_SERVER_MODEL_ID.toUInt() -> "Firmware Update Server"
                FIRMWARE_UPDATE_CLIENT_MODEL_ID.toUInt() -> "Firmware Update Client"
                FIRMWARE_DISTRIBUTION_SERVER_MODEL_ID.toUInt() -> "Firmware Distribution Server"
                FIRMWARE_DISTRIBUTION_CLIENT_MODEL_ID.toUInt() -> "Firmware Distribution Client"
                else -> null
            }

        /**
         * Constructs a model object.
         *
         * @param model                Model.
         * @param applicationKeys      List of application keys.
         * @param nodes                List of nodes.
         * @param groups               List of groups.
         *
         */
        fun init(
            model: Model,
            applicationKeys: List<ApplicationKey>,
            nodes: List<Node>,
            groups: List<Group>,
        ): Model? {
            val bind = model.bind.filter { keyIndex ->
                applicationKeys.any { it.index == keyIndex }
            }
            val subscribe = model.subscribe.filter { address ->
                groups.any { group -> group.address == address }
            }

            // Copy publish settings if
            // - it exists
            // - is configured to use one of the exported Application Keys
            // - The destination addresses is an exported Node, an exported Group or a special group
            return model.publish?.takeIf { publish ->
                applicationKeys.any { it.index == publish.index }
            }?.let { publish ->
                if (publish.address is FixedGroupAddress ||
                    (publish.address is UnicastAddress && nodes.any { node ->
                        node.elements.any { element ->
                            element.unicastAddress == publish.address
                        }
                    }) ||
                    (publish.address is GroupAddress && groups.any { it.address == publish.address })
                ) {
                    Model(
                        modelId = model.modelId,
                        _bind = bind.toMutableList(),
                        _subscribe = subscribe.toMutableList(),
                        _publish = publish
                    )
                } else {
                    null
                }
            }
        }
    }
}

