@file:Suppress("MemberVisibilityCanBePrivate", "PropertyName")

package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Represents a list of unicast addresses that are excluded by a Mesh Manager for a particular IV
 * index.
 *
 * After a node is removed from a network, other nodes keep SeqAuth values associated with addresses
 * used by the removed node. Packets sent from a new node using the same set of addresses would be
 * discarded until the new sequence numbers would get higher than what is in Replay Protection
 * List or the receivers. Therefore, until the IV index is incremented by 2, the addresses of a
 * removed node are reserved and cannot be assigned to a new node. When the IV index increments by
 * at least 2, the SeqAuth is guaranteed to be higher, no matter what the sequence number used by
 * the new node would be, therefore the addresses can be reassigned.
 *
 * @property ivIndex       32-bit value that is a shared network resource known by all nodes in a
 *                         given network.
 * @property addresses     List of excluded addresses for a given ivIndex.
 * @constructor            Creates an ExclusionList object.
 */
@ConsistentCopyVisibility
@Serializable
internal data class ExclusionList internal constructor(
    val ivIndex: UInt,
    @SerialName(value = "addresses")
    internal val _addresses: MutableList<UnicastAddress> = mutableListOf()
) {
    val addresses: List<UnicastAddress>
        get() = _addresses

    @Transient
    internal var network: MeshNetwork? = null

    /**
     * Excludes a given unicast address.
     *
     * @param address Unicast address to be excluded.
     */
    fun exclude(address: UnicastAddress): Boolean {
        if (address !in _addresses) {
            _addresses.add(address)
        }
        network?.updateTimestamp()
        return true
    }

    /**
     * Excludes all the unicast addresses of the elements in a given node.
     *
     * @param node Node containing the element addresses to be excluded.
     */
    fun exclude(node: Node) {
        node.elements.forEach { element ->
            exclude(element.unicastAddress)
        }
        network?.updateTimestamp()
    }

    /**
     * Returns true if the address is excluded.
     *
     * @param address unicast address to be verified.
     */
    fun isExcluded(address: UnicastAddress): Boolean = address in _addresses
}

/**
 * Checks whether the given Unicast Address range can be reassigned to a new Node, as it has been
 * used by a Node that was recently removed.
 *
 * @param range Unicast range to check.
 * @param ivIndex Current IV Index .
 * @returns true if the given address is excluded or false otherwise.
 */
internal fun List<ExclusionList>.contains(
    range: UnicastRange,
    ivIndex: IvIndex
) = isNotEmpty() && excludedAddresses(ivIndex).any { range.contains(it.address) }

/**
 * Checks for excluded Unicast addresses for the given IV Index.
 *
 * @param ivIndex IV Index of the exclusion list.
 * @return List of exclusion list objects.
 */
internal fun List<ExclusionList>.excludedAddresses(ivIndex: IvIndex) = filter {
    it.ivIndex == ivIndex.index || (ivIndex.index > 0u && it.ivIndex == ivIndex.index - 1u)
}.flatMap { it.addresses }