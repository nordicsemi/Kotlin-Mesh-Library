@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nordicsemi.kotlin.mesh.core.exception.InvalidKeyLength
import no.nordicsemi.kotlin.mesh.core.exception.KeyInUse
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork.Companion.onChange
import no.nordicsemi.kotlin.mesh.core.model.serialization.KeySerializer
import no.nordicsemi.kotlin.mesh.crypto.Crypto
import kotlin.uuid.ExperimentalUuidApi

/**
 * Application Keys are used to secure communications at the upper transport layer.
 * The application key (AppKey) shall be generated using a random number generator
 * compatible with the requirements in Volume 2, Part H, Section 2 of the Core Specification [1].
 *
 * @property index              The index property contains an integer from 0 to 4095 that
 *                              represents the NetKey index for this network key.
 * @property name               Human-readable name for the application functionality associated
 *                              with this application key.
 * @property key                128-bit application key.
 * @property oldKey             OldKey property contains the previous application key.
 * @property boundNetworkKey    Network key to which this application key is bound to.
 * @param    _key               128-bit application key.
 */
@ConsistentCopyVisibility
@Serializable
data class ApplicationKey internal constructor(
    @SerialName(value = "name")
    private var _name: String,
    val index: KeyIndex,
    @Serializable(with = KeySerializer::class)
    @SerialName("key")
    private var _key: ByteArray = Crypto.generateRandomKey(),
) {
    var name: String
        get() = _name
        set(value) {
            require(value.isNotBlank()) { "Name cannot be empty" }
            onChange(oldValue = _name, newValue = value) { network?.updateTimestamp() }
            _name = value
        }

    /**
     * The boundNetKey property contains a corresponding Network Key index of the network key in
     * the mesh network.
     */
    @SerialName("boundNetKey")
    internal var boundNetKeyIndex: KeyIndex = 0u
        set(value) {
            require(value.isValidKeyIndex()) { "Key index must be in range from 0 to 4095" }
            onChange(oldValue = field, newValue = value) { network?.updateTimestamp() }
            field = value
        }

    var key: ByteArray
        get() = _key
        internal set(value) {
            require(value.size == 16) { "Key must be 16-bytes long" }
            onChange(oldValue = _key, newValue = value) {
                oldKey = _key
                oldAid = aid
                _key = value
                regenerateKeyDerivatives()
            }
        }

    @Serializable(with = KeySerializer::class)
    var oldKey: ByteArray? = null
        internal set(value) {
            onChange(oldValue = field, newValue = value) {
                field = value
                if (field == null) {
                    oldAid = null
                }
            }
        }

    @Transient
    internal var network: MeshNetwork? = null

    // Returns the Network Key to which this Application Key is bound to. This property should not
    // by null as the MeshNetwork is assigned when adding a Network Key via the public api.
    val boundNetworkKey: NetworkKey
        get() = network!!.networkKey(index = boundNetKeyIndex)!!

    internal var aid: Byte = Crypto.calculateAid(N = key)

    internal var oldAid: Byte? = null

    @OptIn(ExperimentalUuidApi::class)
    val isInUse: Boolean
        get() = network?.run {
            nodes.filter { it.uuid != localProvisioner?.uuid }
                .any { it.knows(key = this@ApplicationKey) }
        } ?: false

    init {
        require(index.isValidKeyIndex()) { "Key index must be in range from 0 to 4095" }
        regenerateKeyDerivatives()
    }

    override fun toString(): String =
        "ApplicationKey(name: '$name', index: $index, boundNetKeyIndex: $boundNetKeyIndex)"

    /**
     * Returns whether the application key is added to any nodes in the network.
     * A key that is in use cannot be removed until it has been removed from all the nodes.
     */
    /*fun isInUse(): Boolean = network?.run {
        // The application key in used when it is known by any of the nodes in the network.
        _nodes.any { node ->
            node.appKeys.any { nodeKey ->
                nodeKey.index == index
            }
        }
    } ?: false*/

    /**
     * Updates the existing key with the given key, if it is not in use.
     *
     * @param key New key.
     * @throws KeyInUse If the key is already in use.
     */
    fun setKey(key: ByteArray) {
        require(!isInUse) { throw KeyInUse() }
        require(key.size == 16) { throw InvalidKeyLength() }
        _key = key
    }

    /**
     * Binds the application key to a given network key. The application key must not be in use.
     * If any of the network Nodes already knows this key, this method throws an error
     *
     * @param networkKey Network key to which the application key is bound to.
     * @throws KeyInUse If the key is already in use.
     */
    @Throws(KeyInUse::class)
    fun bind(networkKey: NetworkKey) {
        network?.let {
            require(!isInUse) { throw KeyInUse() }
            boundNetKeyIndex = networkKey.index
        }
    }

    /**
     * Checks if the application key is bound to a given network key.
     *
     * @param networkKey Network key to which the application key is bound to.
     * @return true if the application key is bound to the given network key, false otherwise.
     */
    fun isBoundTo(networkKey: NetworkKey) = boundNetKeyIndex == networkKey.index

    /**
     * Checks if the application key is bound to any of the given list of network keys.
     *
     * @param networkKeys Network key to which the application key is bound to.
     * @return true if the application key is bound to the given network key, false otherwise.
     */
    fun isBoundTo(networkKeys: List<NetworkKey>) = networkKeys.any { isBoundTo(it) }

    /**
     * Checks if the given model is bound to this key.
     *
     * @param model Model to check against.
     * @return true if the key is bound to the model or false otherwise.
     */
    fun isBoundTo(model: Model) = model.bind.any { it == index }

    private fun regenerateKeyDerivatives() {
        aid = Crypto.calculateAid(N = key)

        // When the Application Key is imported from JSOn, old key derivatives must be calculated.
        oldKey?.let { oldKey ->
            if (oldAid == null) {
                oldAid = Crypto.calculateAid(N = oldKey)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApplicationKey

        if (index != other.index) return false
        if (!_key.contentEquals(other._key)) return false
        if (boundNetKeyIndex != other.boundNetKeyIndex) return false
        if (oldKey != null) {
            if (other.oldKey == null) return false
            if (!oldKey.contentEquals(other.oldKey)) return false
        } else if (other.oldKey != null) return false
        if (boundNetworkKey != other.boundNetworkKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index.hashCode()
        result = 31 * result + _key.contentHashCode()
        result = 31 * result + boundNetKeyIndex.hashCode()
        result = 31 * result + (oldKey?.contentHashCode() ?: 0)
        result = 31 * result + boundNetworkKey.hashCode()
        return result
    }
}

/**
 * Checks whether any of the Application keys in the List is bound to the given network Key. The key
 * comparison is based on Key Index property.
 *
 * @param networkKey Network key to which the application keys are bound to.
 * @return True if any of the application keys in the list is bound to the given network key,
 *         false otherwise.
 */
infix fun List<ApplicationKey>.contains(networkKey: NetworkKey) = any { it.isBoundTo(networkKey) }

/**
 * Returns a list of application keys bound to a given network key.
 *
 * @param networkKey Network key to which the application keys are bound to.
 * @return List<ApplicationKey> List of application keys bound to the given network key.
 */
infix fun List<ApplicationKey>.boundTo(networkKey: NetworkKey): List<ApplicationKey> = filter {
    it.isBoundTo(networkKey)
}

/**
 * Filters the list of Application keys to only those that are known to the given node.
 *
 * @param node Node to check.
 * @return List of Application Keys known to the node.
 */
infix fun List<ApplicationKey>.knownTo(node: Node): List<ApplicationKey> = filter { node.knows(it) }