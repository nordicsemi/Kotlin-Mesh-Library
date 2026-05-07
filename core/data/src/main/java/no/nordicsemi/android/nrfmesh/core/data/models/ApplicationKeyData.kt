@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package no.nordicsemi.android.nrfmesh.core.data.models

import no.nordicsemi.android.nrfmesh.core.common.KeyIdGenerator
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import kotlin.uuid.ExperimentalUuidApi

/**
 * Application Keys are used to secure communications at the upper transport layer.
 * The application key (AppKey) shall be generated using a random number generator
 * compatible with the requirements in Volume 2, Part H, Section 2 of the Core Specification [1].
 *
 * @property name               Human-readable name for the application functionality associated
 *                              with this application key.
 * @property index              The index property contains an integer from 0 to 4095 that
 *                              represents the NetKey index for this network key.
 * @property key                128-bit application key.
 * @property oldKey             OldKey property contains the previous application key.
 * @property boundNetKeyIndex   The boundNetKey property contains a corresponding Network Key index
 *                              of the network key in the mesh network.
 * @property isInUse            True if the application key is currently in use in the mesh network.
 */
@OptIn(ExperimentalUuidApi::class)
data class ApplicationKeyData(
    val name: String,
    val index: KeyIndex,
    val key: ByteArray,
    val oldKey: ByteArray? = null,
    val boundNetKeyIndex: KeyIndex,
    val boundNetworkKeyName: String,
    val isInUse: Boolean,
    val id: Long = KeyIdGenerator.nextId()
) {
    constructor(key: ApplicationKey) : this(
        name = key.name,
        index = key.index,
        key = key.key,
        oldKey = key.oldKey,
        boundNetKeyIndex = key.boundNetworkKey.index,
        boundNetworkKeyName = key.boundNetworkKey.name,
        isInUse = key.isInUse
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ApplicationKeyData

        if (isInUse != other.isInUse) return false
        if (id != other.id) return false
        if (name != other.name) return false
        if (index != other.index) return false
        if (!key.contentEquals(other.key)) return false
        if (!oldKey.contentEquals(other.oldKey)) return false
        if (boundNetKeyIndex != other.boundNetKeyIndex) return false
        if (boundNetworkKeyName != other.boundNetworkKeyName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isInUse.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + index.hashCode()
        result = 31 * result + key.contentHashCode()
        result = 31 * result + (oldKey?.contentHashCode() ?: 0)
        result = 31 * result + boundNetKeyIndex.hashCode()
        result = 31 * result + boundNetworkKeyName.hashCode()
        return result
    }
}