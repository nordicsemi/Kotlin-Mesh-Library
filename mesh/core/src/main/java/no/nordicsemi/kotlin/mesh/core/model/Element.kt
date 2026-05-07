@file:Suppress("MemberVisibilityCanBePrivate")

package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.layers.foundation.ConfigurationClientHandler
import no.nordicsemi.kotlin.mesh.core.layers.foundation.ConfigurationServerHandler
import no.nordicsemi.kotlin.mesh.core.layers.foundation.FirmwareDistributionClientHandler
import no.nordicsemi.kotlin.mesh.core.layers.foundation.FirmwareUpdateClientHandler
import no.nordicsemi.kotlin.mesh.core.layers.foundation.HealthClientHandler
import no.nordicsemi.kotlin.mesh.core.layers.foundation.HealthServerHandler
import no.nordicsemi.kotlin.mesh.core.layers.foundation.PrivateBeaconHandler
import no.nordicsemi.kotlin.mesh.core.layers.foundation.RemoteProvisioningClientHandler
import no.nordicsemi.kotlin.mesh.core.layers.foundation.SarConfigurationClientHandler
import no.nordicsemi.kotlin.mesh.core.layers.foundation.SceneClientHandler
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionTimer
import no.nordicsemi.kotlin.mesh.core.model.serialization.LocationAsStringSerializer
import no.nordicsemi.kotlin.mesh.logger.Logger
import java.nio.ByteOrder
import kotlin.time.Duration

/**
 * Element represents a mesh element that is defined as an addressable entity within a mesh node.
 *
 * @property location           Describes the element location.
 * @property models             List of [Model] within an element.
 * @property name               A human-readable name that can identify an element within the node
 *                              and is optional according to Mesh CDB.
 * @property index              The index property contains an integer from 0 to 255 that represents
 *                              the numeric order of the element within this node and a node has
 *                              at-least one element which is called the primary element.
 * @property parentNode         Parent node that an element may belong to.
 * @property unicastAddress     Address of the element.
 * @constructor Creates an Element object.
 */
@Serializable
class Element(
    @SerialName(value = "name")
    private var _name: String? = null,
    @Serializable(with = LocationAsStringSerializer::class)
    val location: Location,
    @SerialName(value = "models")
    private var _models: MutableList<Model> = mutableListOf(),
) {
    var name: String?
        get() = _name
        set(value) {
            name?.let { require(it.isNotBlank()) { "Element name cannot be blank!" } }
            MeshNetwork.onChange(oldValue = _name, newValue = value) {
                parentNode?.network?.updateTimestamp()
            }
            _name = value
        }

    // Final index will be set when Element is added to the Node.
    // Refer https://github.com/NordicSemiconductor/IOS-nRF-Mesh-Library/blob/
    // c1755555f76fb6f393bfdad37a23566ddd581536/nRFMeshProvision/Classes/Mesh%20Model/Node.swift#L620
    var index: Int = 0
        internal set

    val models: List<Model>
        get() = _models

    @Transient
    var parentNode: Node? = null
        internal set

    val unicastAddress: UnicastAddress
        get() = parentNode?.run {
            primaryUnicastAddress + index
        } ?: UnicastAddress(address = index + 1)

    val isPrimary: Boolean
        get() = index == 0

    internal val composition: ByteArray
        get() {
            var data = location.value.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
            val sigModels = mutableListOf<Model>()
            val vendorModels = mutableListOf<Model>()
            for (model in _models) {
                if (model.isBluetoothSigAssigned) {
                    sigModels.add(model)
                } else {
                    vendorModels.add(model)
                }
            }
            data += sigModels.size.toByte()
            data += vendorModels.size.toByte()
            for (model in sigModels) {
                data += (model.modelId as SigModelId).modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
            }
            for (model in vendorModels) {
                val modelId = model.modelId as VendorModelId
                data += modelId.companyIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
                data += modelId.modelIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
            }
            return data
        }

    init {
        require(index in LOWER_BOUND..HIGHER_BOUND) {
            " Index must be a value ranging from $LOWER_BOUND to $HIGHER_BOUND!"
        }
        // Assign the parent element to all models.
        _models.forEach { it.parentElement = this }
    }

    override fun toString(): String {
        return "Element(name: $name, location: $location, models: $_models, index: $index, unicastAddress: $unicastAddress)"
    }

    /**
     * Returns the model with the given model ID.
     *
     * @param modelId Model ID.
     * @return Model with the given model ID, or null if not found.
     */
    fun model(modelId: ModelId) = model(modelId = modelId.id)

    /**
     * Returns the model with the given model ID.
     *
     * @param modelId Model ID.
     * @return Model with the given model ID, or null if not found.
     */
    fun model(modelId: UInt) = _models.firstOrNull { it.modelId.id == modelId }

    /**
     * Adds a model to the element.
     *
     * @param model Model to be added.
     * @return true if the model was added, false otherwise.
     */
    internal fun add(model: Model) = _models.add(model).also {
        model.parentElement = this
    }

    /**
     * Inserts a model to the element at the given index.
     *
     * @param model Model to be added.
     * @param index Index at which the model should be added.
     * @return true if the model was added, false otherwise.
     */
    internal fun insert(model: Model, index: Int) = _models.add(index, model).also {
        model.parentElement = this
    }

    /**
     * Adds the natively supported Models to the Element.
     *
     * Note: This is only to be called for the primary element of the Local Node.
     */
    internal fun addPrimaryElementModels(
        triggerAttentionTimer: (HealthAttentionTimer) -> Unit,
        logger: Logger?,
    ) {
        require(isPrimary) { return }
        insert(
            model = Model(
                modelId = SigModelId(Model.CONFIGURATION_SERVER_MODEL_ID),
                handler = ConfigurationServerHandler()
            ),
            index = 0
        )
        insert(
            model = Model(
                modelId = SigModelId(Model.CONFIGURATION_CLIENT_MODEL_ID),
                handler = ConfigurationClientHandler()
            ),
            index = 1
        )
        insert(
            model = Model(
                modelId = SigModelId(Model.HEALTH_SERVER_MODEL_ID),
                handler = HealthServerHandler(
                    triggerAttentionTimer = triggerAttentionTimer,
                    logger = logger
                )
            ),
            index = 2
        )
        insert(
            Model(
                modelId = SigModelId(Model.HEALTH_CLIENT_MODEL_ID),
                handler = HealthClientHandler()
            ), index = 3
        )
        insert(
            model = Model(
                modelId = SigModelId(Model.PRIVATE_BEACON_CLIENT_MODEL_ID),
                handler = PrivateBeaconHandler()
            ),
            index = 4
        )
        insert(
            model = Model(
                modelId = SigModelId(Model.SAR_CONFIGURATION_CLIENT_MODEL_ID),
                handler = SarConfigurationClientHandler()
            ),
            index = 5
        )
        insert(
            model = Model(
                modelId = SigModelId(Model.REMOTE_PROVISIONING_CLIENT_MODEL_ID),
                handler = RemoteProvisioningClientHandler()
            ),
            index = 6
        )
        insert(
            model = Model(
                modelId = SigModelId(Model.SCENE_CLIENT_MODEL_ID),
                handler = SceneClientHandler()
            ),
            index = 7
        )
        insert(
            model = Model(
                modelId = SigModelId(Model.FIRMWARE_UPDATE_CLIENT_MODEL_ID),
                handler = FirmwareUpdateClientHandler()
            ),
            index = 8
        )
        insert(
            model = Model(
                modelId = SigModelId(Model.FIRMWARE_DISTRIBUTION_CLIENT_MODEL_ID),
                handler = FirmwareDistributionClientHandler()
            ),
            index = 9
        )
    }

    /**
     * Removes the models that are or should be supported by the library.
     */
    internal fun removePrimaryElementModels() {
        _models.removeAll { model ->
            // Health models are not yet supported.
            model.isHealthServer || model.isHealthClient ||
                    // The library supports Scene Client model natively.
                    model.isSceneClient ||
                    // The models that require Device Key should not be managed by users.
                    // Some of them are supported natively in the library.
                    model.requiresDeviceKey
        }
    }

    /**
     * Returns whether the Element contains a Bluetooth SIG defined Model with the given model ID.
     *
     * @param sigModelId Bluetooth SIG defined Model ID.
     * @return true if the Element contains the Model, false otherwise.
     */
    fun contains(sigModelId: SigModelId) = models.any { it.modelId == sigModelId }

    /**
     * Returns whether the Element contains a Vendor Model with the given model ID.
     *
     * @param vendorModelId Vendor Model ID.
     * @return true if the Element contains the Model, false otherwise.
     */
    fun contains(vendorModelId: VendorModelId) = models.any { it.modelId == vendorModelId }

    /**
     * Returns whether the Element contains the given model.
     *
     * @param model Model to be checked.
     * @return true if the Element contains the Model, false otherwise.
     */
    fun contains(model: Model) = models.contains(model)

    /**
     * Returns whether the Element contains a model bound to the given Application Key.
     *
     * @param key Application Key.
     * @return true if the Element contains the Model, false otherwise.
     */
    fun contains(key: ApplicationKey) = models.any { key.isBoundTo(model = it) }

    /**
     * Returns whether the element contains any model that is subscribed to the given group.
     *
     * @param group Group to be checked.
     */
    fun contains(group: Group): Boolean = models.any {
        it.subscribe.contains(group.address as SubscriptionAddress)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Element) return false
        if (_name != other._name) return false
        if (location != other.location) return false
        if (!_models.equals(other._models)) return false
        if (index != other.index) return false
        return true
    }

    override fun hashCode(): Int {
        var result = _name?.hashCode() ?: 0
        result = 31 * result + location.hashCode()
        result = 31 * result + _models.hashCode()
        result = 31 * result + index
        return result
    }

    internal companion object {
        const val LOWER_BOUND = 0
        const val HIGHER_BOUND = 255

        /**
         * Primary Element for the Provisioner's Node.
         *
         * The Element will contain all mandatory Models (Configuration Server and Health Server)
         * and supported client models (Configuration Client, Health Client and Scene Client).
         */
        var primaryElement = Element(location = Location.UNKNOWN).apply {
            name = "Primary Element"
            // Configuration Server is required for all nodes.
            add(Model(modelId = SigModelId(Model.CONFIGURATION_SERVER_MODEL_ID)))
            // Configuration Client is added because this is a Provisioner's Node.
            add(Model(modelId = SigModelId(Model.CONFIGURATION_CLIENT_MODEL_ID)))
            // Configuration Server is required for all nodes.
            add(Model(modelId = SigModelId(Model.HEALTH_SERVER_MODEL_ID)))
            // Health Client is added because this is a Provisioner's Node.
            add(Model(modelId = SigModelId(Model.HEALTH_CLIENT_MODEL_ID)))
        }
    }
}

/**
 * Constructs the composition returned by the Composition Data Page 0 fir a given list of elements.
 *
 * @receiver List of elements.
 * @return Byte array containing the composition of a list of elements.
 */
internal fun List<Element>.composition(): ByteArray {
    var data = byteArrayOf()
    for (element in this) {
        data += element.composition
    }
    return data
}