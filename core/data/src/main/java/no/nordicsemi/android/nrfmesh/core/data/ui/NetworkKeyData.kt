@file:Suppress("unused")

package no.nordicsemi.android.nrfmesh.core.data.ui

import no.nordicsemi.android.nrfmesh.core.common.KeyIdGenerator
import kotlin.time.Instant
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.KeyRefreshPhase
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.Security
import kotlin.time.ExperimentalTime

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
 * @property name                  Human-readable name for the the mesh subnet associated with this
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
 * @property isPrimary             Returns true if the network key is the primary network key.
 * @property isInUse               Returns whether the network key is added to any nodes in the
 *                                 network. A key that is in use cannot be removed until it has been
 *                                 removed from all the nodes and is no longer bound to any
 *                                 application keys.
 */
@ConsistentCopyVisibility
@OptIn(ExperimentalTime::class)
data class NetworkKeyData internal constructor(
    val index: KeyIndex,
    val name: String,
    val key: ByteArray,
    val oldKey: ByteArray?,
    val security: Security,
    val phase: KeyRefreshPhase,
    val networkId: ByteArray,
    val oldNetworkId: ByteArray?,
    val timestamp: Instant,
    val isPrimary: Boolean,
    val isInUse: Boolean,
    val id: Long = KeyIdGenerator.nextId(),
) {
    /**
     * Convenience constructor for creating a new network key
     *
     * @param key network key
     */
    constructor(key: NetworkKey) : this(
        index = key.index,
        name = key.name,
        key = key.key,
        oldKey = key.oldKey,
        security = key.security,
        phase = key.phase,
        networkId = key.networkId,
        oldNetworkId = key.oldNetworkId,
        timestamp = key.timestamp,
        isPrimary = key.isPrimary,
        isInUse = key.isInUse
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NetworkKeyData

        if (index != other.index) return false
        if (name != other.name) return false
        if (!key.contentEquals(other.key)) return false
        if (oldKey != null) {
            if (other.oldKey == null) return false
            if (!oldKey.contentEquals(other.oldKey)) return false
        } else if (other.oldKey != null) return false
        if (security != other.security) return false
        if (phase != other.phase) return false
        if (!networkId.contentEquals(other.networkId)) return false
        if (oldNetworkId != null) {
            if (other.oldNetworkId == null) return false
            if (!oldNetworkId.contentEquals(other.oldNetworkId)) return false
        } else if (other.oldNetworkId != null) return false
        if (timestamp != other.timestamp) return false
        if (isPrimary != other.isPrimary) return false
        if (isInUse != other.isInUse) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + key.contentHashCode()
        result = 31 * result + (oldKey?.contentHashCode() ?: 0)
        result = 31 * result + security.hashCode()
        result = 31 * result + phase.hashCode()
        result = 31 * result + networkId.contentHashCode()
        result = 31 * result + (oldNetworkId?.contentHashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + isPrimary.hashCode()
        result = 31 * result + isInUse.hashCode()
        return result
    }
}