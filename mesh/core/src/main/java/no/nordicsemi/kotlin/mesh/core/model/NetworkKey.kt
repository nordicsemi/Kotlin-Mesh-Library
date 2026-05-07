@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nordicsemi.kotlin.mesh.core.exception.InvalidKeyLength
import no.nordicsemi.kotlin.mesh.core.exception.KeyInUse
import no.nordicsemi.kotlin.mesh.core.model.serialization.KeySerializer
import no.nordicsemi.kotlin.mesh.crypto.Crypto
import no.nordicsemi.kotlin.mesh.crypto.KeyDerivatives
import no.nordicsemi.kotlin.mesh.crypto.SecurityCredentials
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

/**
 * AThe network key object represents the state of the mesh network key that is used for securing
 * communication at the network layer.
 *
 * @property index                 The index property contains an integer from 0 to 4095 that
 *                                 represents the NetKey index for this network key.
 * @property key                   128-bit key.
 * @property security              Security property contains a string with a value of either
 *                                 “insecure” or “secure”, which describes a minimum security level
 *                                 for a subnet associated with this network key. If all the nodes
 *                                 on the subnet associated with this network key have been
 *                                 provisioned using the Secure Provisioning procedure, then the
 *                                 value of minSecurity property for the subnet is set to “secure”;
 *                                 otherwise, the value of the minSecurity is set to “insecure”.
 * @property name                  Human-readable name for the mesh subnet associated with this
 *                                 network key.
 * @property phase                 The phase property represents the [KeyRefreshPhase] for the
 *                                 subnet associated with this network key.
 * @property oldKey                The oldKey property contains a 32-character hexadecimal string
 *                                 that represents the 128-bit network key, and shall be present
 *                                 when the phase property has a non-zero value, such as when the
 *                                 Key Refresh procedure is in progress. The value of the oldKey
 *                                 property contains the previous network key.
 * @property timestamp             The timestamp property contains a string that represents the last
 *                                 time the value of the phase property has been updated.
 * @property networkId             Network ID is derived from a network key and is used to identify
 *                                 the network
 * @property oldNetworkId          Old Network ID is derived from the old network key.
 * @property derivatives           Network key derivatives.
 * @property oldDerivatives        Old network key derivatives.
 * @property transmitKeys          Defines the keys used when sending and receiving mesh messages
 *                                 based on the key refresh phase.
 * @property isPrimary             Returns true if the network key is the primary network key.
 * @property network               Returns the network object this network key belongs to.
 * @property isInUse               Returns whether the network key is added to any nodes in the
 *                                 network. A key that is in use cannot be removed until it has been
 *                                 removed from all the nodes and is no longer bound to any
 *                                 application keys.
 */
@ConsistentCopyVisibility
@OptIn(ExperimentalTime::class)
@Serializable
data class NetworkKey internal constructor(
    val index: KeyIndex,
    @SerialName(value = "name")
    private var _name: String,
    @Serializable(with = KeySerializer::class)
    @SerialName("key")
    private var _key: ByteArray = Crypto.generateRandomKey(),
    @SerialName(value = "minSecurity")
    private var _security: Security = Secure,
    @SerialName(value = "phase")
    private var _phase: KeyRefreshPhase = NormalOperation,
) {
    /**
     * Convenience constructor for creating a new network key for tests
     *
     * @param index   The index property contains an integer from 0 to 4095 that represents the
     *                NetKey index for this network key.
     * @param key     128-bit key.
     * @param name    Human-readable name for the mesh subnet associated with this network key.
     */
    internal constructor(
        name: String = "Primary Network Key",
        index: KeyIndex = 0u,
        key: ByteArray = Crypto.generateRandomKey(),
    ) : this(index = index, _name = name, _key = key)

    var name: String
        get() = _name
        set(value) {
            require(value.isNotBlank()) { "Name cannot be empty!" }
            MeshNetwork.onChange(oldValue = _name, newValue = value) { network?.updateTimestamp() }
            _name = value
        }

    var security: Security
        get() = _security
        internal set(value) {
            // Security can only be downgraded.
            if (_security is Secure)
                _security = value
        }
    var phase: KeyRefreshPhase
        get() = _phase
        set(value) {
            MeshNetwork.onChange(oldValue = _phase, newValue = value) { updateTimeStamp() }
            _phase = value
        }
    var key: ByteArray
        get() = _key
        internal set(value) {
            require(value = value.size == 16) { throw InvalidKeyLength() }
            MeshNetwork.onChange(oldValue = _key, newValue = value) {
                oldKey = _key
            }
            _key = value
            _phase = KeyDistribution
            regenerateKeyDerivatives()
        }

    @Serializable(with = KeySerializer::class)
    @SerialName(value = "oldKey")
    var oldKey: ByteArray? = null
        internal set(value) {
            MeshNetwork.onChange(oldValue = _name, newValue = value) {
                if (value == null) {
                    oldDerivatives = null
                    oldNetworkId = null
                    _phase = NormalOperation
                }
                field = value
            }
        }

    var timestamp: Instant = Clock.System.now()
        internal set

    @Transient
    lateinit var networkId: ByteArray
        private set

    @Transient
    var oldNetworkId: ByteArray? = null
        private set

    @Transient
    internal var derivatives: NetworkKeyDerivatives = Crypto
        .calculateKeyDerivatives(key, SecurityCredentials.ManagedFlooding)
        .toNetworkKeyDerivatives()
        private set

    @Transient
    internal var oldDerivatives: NetworkKeyDerivatives? = null
        private set

    internal val transmitKeys: NetworkKeyDerivatives
        get() = when (phase) {
            KeyDistribution -> oldDerivatives!!
            else -> derivatives
        }

    val isPrimary: Boolean
        get() = index == 0.toUShort()

    val isSecondary: Boolean
        get() = !isPrimary

    @Transient
    var network: MeshNetwork? = null
        internal set

    @OptIn(ExperimentalUuidApi::class)
    val isInUse: Boolean
        get() = network?.run {
            // A network key is in use if at least one application key is bound to it.
            // OR
            // The network key is known by any of the nodes in the network.
             val hasBoundAppKey = _applicationKeys
                .any { it.boundNetKeyIndex == index }
            val knownByRemoteNode = nodes
                .filter { it.uuid != localProvisioner?.uuid }
                .any { it.knows(key = this@NetworkKey) }
            hasBoundAppKey || knownByRemoteNode
        } ?: false

    init {
        require(index.isValidKeyIndex()) { "Key index must be in range from 0 to 4095" }
        regenerateKeyDerivatives()
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        return "NetworkKey(index: $index, name: $_name, security: $_security, phase: $_phase, " +
                "timestamp: $timestamp)"
    }

    private fun updateTimeStamp() {
        timestamp = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    }

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

    private fun regenerateKeyDerivatives() {
        // Calculate Network ID.
        networkId = Crypto.calculateNetworkId(key)
        // Calculate key derivatives.
        derivatives = Crypto
            .calculateKeyDerivatives(key, SecurityCredentials.ManagedFlooding)
            .toNetworkKeyDerivatives()

        // When the Network Key is imported from JSON, old key derivatives must be calculated.
        oldKey?.let { oldKey ->
            // Calculate old Network ID.
            oldNetworkId = Crypto.calculateNetworkId(oldKey)
            // Calculate old key derivatives.
            oldDerivatives = Crypto
                .calculateKeyDerivatives(oldKey, SecurityCredentials.ManagedFlooding)
                .toNetworkKeyDerivatives()
        }
    }

    /**
     * Sets the security level to secure.
     */
    fun lowerSecurity() {
        _security = Insecure
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NetworkKey

        if (index != other.index) return false
        if (_name != other._name) return false
        if (!_key.contentEquals(other._key)) return false
        if (_security != other._security) return false
        if (_phase != other._phase) return false
        if (oldKey != null) {
            if (other.oldKey == null) return false
            if (!oldKey.contentEquals(other.oldKey)) return false
        } else if (other.oldKey != null) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index.hashCode()
        result = 31 * result + _name.hashCode()
        result = 31 * result + _key.contentHashCode()
        result = 31 * result + _security.hashCode()
        result = 31 * result + _phase.hashCode()
        result = 31 * result + (oldKey?.contentHashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        return result
    }

    companion object {
        private const val MIN_KEY_INDEX = 0
        private const val MAX_KEY_INDEX = 4095
    }

}

internal fun KeyDerivatives.toNetworkKeyDerivatives() = NetworkKeyDerivatives(
    identityKey = identityKey,
    beaconKey = beaconKey,
    privateBeaconKey = privateBeaconKey,
    encryptionKey = encryptionKey,
    privacyKey = privacyKey,
    nid = nid
)


/**
 * Network Key derivatives
 *
 * @property identityKey         Identity key.
 * @property beaconKey           Beacon key.
 * @property privateBeaconKey    Private beacon key.
 * @property encryptionKey       Encryption key.
 * @property privacyKey          Privacy key.
 * @property nid                 Network identifier.
 */
internal data class NetworkKeyDerivatives(
    val identityKey: ByteArray,
    val beaconKey: ByteArray,
    val privateBeaconKey: ByteArray,
    val encryptionKey: ByteArray,
    val privacyKey: ByteArray,
    val nid: Byte,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NetworkKeyDerivatives

        if (!identityKey.contentEquals(other.identityKey)) return false
        if (!beaconKey.contentEquals(other.beaconKey)) return false
        if (!privateBeaconKey.contentEquals(other.privateBeaconKey)) return false
        if (!encryptionKey.contentEquals(other.encryptionKey)) return false
        if (!privacyKey.contentEquals(other.privacyKey)) return false
        if (nid != other.nid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = identityKey.contentHashCode()
        result = 31 * result + beaconKey.contentHashCode()
        result = 31 * result + privateBeaconKey.contentHashCode()
        result = 31 * result + encryptionKey.contentHashCode()
        result = 31 * result + privacyKey.contentHashCode()
        result = 31 * result + nid.hashCode()
        return result
    }
}

/**
 * Filters the list of Network Keys to only those that are known to the given node.
 *
 * @param node Node to check.
 * @return List of network keys known to the node.
 */
fun List<NetworkKey>.knownTo(node: Node): List<NetworkKey> = filter {
    node.knows(key = it)
}