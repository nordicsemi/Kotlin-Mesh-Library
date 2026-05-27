@file:Suppress("MemberVisibilityCanBePrivate", "PropertyName")

package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nordicsemi.kotlin.mesh.core.exception.AddressAlreadyInUse
import no.nordicsemi.kotlin.mesh.core.exception.AddressNotInAllocatedRanges
import no.nordicsemi.kotlin.mesh.core.exception.DoesNotBelongToNetwork
import no.nordicsemi.kotlin.mesh.core.exception.OverlappingProvisionerRanges
import no.nordicsemi.kotlin.mesh.core.exception.RangeAlreadyAllocated
import no.nordicsemi.kotlin.mesh.core.model.serialization.UuidSerializer
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A Provisioner is capable of provisioning a device to a mesh network and is represented by a
 * provisioner object in the Mesh Configuration Database. A provisioner is represented as a node in
 * mesh network only if it is assigned a unicast address. Having a unicast address assigned allows
 * configuring nodes in the mesh network. Otherwise, a provisioner can only provision nodes to a
 * mesh network.
 *
 * @property name                          Provisioner name.
 * @property uuid                          UUID of the provisioner.
 * @property allocatedUnicastRanges        List of allocated unicast ranges for a given provisioner.
 * @property allocatedGroupRanges          List of allocated group ranges for a given provisioner.
 * @property allocatedSceneRanges          List of allocated scene ranges for a given provisioner.
 * @property node                          Node of the provisioner.
 * @property primaryUnicastAddress         Primary unicast address of the provisioner.
 * @property hasConfigurationCapabilities  Returns true if the provisioner has configuration
 *                                         capabilities.
 * @property isLocal                       Returns true if the Provisioner is set as the local
 *                                         Provisioner.
 * @property
 *
 * @constructor Creates a Provisioner object.
 */
@ConsistentCopyVisibility
@OptIn(ExperimentalUuidApi::class)
@Suppress("unused")
@Serializable
data class Provisioner internal constructor(
    @SerialName(value = "UUID")
    @Serializable(with = UuidSerializer::class)
    val uuid: Uuid,
    @SerialName(value = "provisionerName")
    private var _name: String = "nRF Mesh Provisioner",
    @SerialName(value = "allocatedUnicastRange")
    internal val _allocatedUnicastRanges: MutableList<UnicastRange> = mutableListOf(),
    @SerialName(value = "allocatedGroupRange")
    internal val _allocatedGroupRanges: MutableList<GroupRange> = mutableListOf(),
    @SerialName(value = "allocatedSceneRange")
    internal val _allocatedSceneRanges: MutableList<SceneRange> = mutableListOf(),
) {
    var name: String
        get() = _name
        set(value) {
            require(value = value.isNotBlank()) { "Name cannot be empty!" }
            MeshNetwork.onChange(oldValue = _name, newValue = value) {
                _name = value
                network?.updateTimestamp()
            }
        }

    val allocatedUnicastRanges: List<UnicastRange>
        get() = _allocatedUnicastRanges

    val allocatedGroupRanges: List<GroupRange>
        get() = _allocatedGroupRanges

    val allocatedSceneRanges: List<SceneRange>
        get() = _allocatedSceneRanges

    val node: Node?
        get() = network?._nodes?.find { it.uuid == uuid }

    val primaryUnicastAddress: UnicastAddress?
        get() = node?.primaryUnicastAddress

    val hasConfigurationCapabilities: Boolean
        get() = node != null

    val isLocal: Boolean
        get() = network?.provisioners?.firstOrNull { it.uuid == uuid } != null

    val otherUnicastRanges: List<UnicastRange>
        get() = network?.provisioners
            ?.filter { it != this }
            ?.flatMap { it._allocatedUnicastRanges } ?: emptyList()

    val otherGroupRanges: List<GroupRange>
        get() = network?.provisioners
            ?.filter { it != this }
            ?.flatMap { it._allocatedGroupRanges } ?: emptyList()

    val otherSceneRanges: List<SceneRange>
        get() = network?.provisioners
            ?.filter { it != this }
            ?.flatMap { it._allocatedSceneRanges } ?: emptyList()

    @Transient
    internal var network: MeshNetwork? = null

    /**
     * Creates a Provisioner with the given UUID.
     *
     * @param uuid Provisioner UUID.
     */
    constructor(uuid: Uuid = Uuid.random()) : this(
        uuid = uuid,
        _allocatedUnicastRanges = mutableListOf(),
        _allocatedGroupRanges = mutableListOf(),
        _allocatedSceneRanges = mutableListOf(),
        _name = "nRF Mesh Provisioner"
    )

    /**
     * Creates a Provisioner with the given name.
     *
     * @param name Provisioner name.
     */
    constructor(name: String) : this(
        uuid = Uuid.random(),
        _allocatedUnicastRanges = mutableListOf(),
        _allocatedGroupRanges = mutableListOf(),
        _allocatedSceneRanges = mutableListOf(),
        _name = name
    )
    /**
     * Creates a Provisioner with the given UUID and name.
     *
     * @param uuid Provisioner UUID.
     * @param name Provisioner name.
     */
    constructor(uuid: Uuid = Uuid.random(), name: String) : this(
        uuid = uuid,
        _allocatedUnicastRanges = mutableListOf(),
        _allocatedGroupRanges = mutableListOf(),
        _allocatedSceneRanges = mutableListOf(),
        _name = name
    )


    /**
     * Convenience constructor for tests
     *
     * @property name                      Provisioner name.
     * @property allocatedUnicastRanges    List of allocated unicast ranges for a given provisioner.
     * @property allocatedGroupRanges      List of allocated group ranges for a given provisioner.
     * @property allocatedSceneRanges      List of allocated scene ranges for a given provisioner.
     *
     */
    internal constructor(
        name: String,
        allocatedUnicastRanges: List<UnicastRange> = mutableListOf(),
        allocatedGroupRanges: List<GroupRange> = mutableListOf(),
        allocatedSceneRanges: List<SceneRange> = mutableListOf(),
    ) : this(
        uuid = Uuid.random(),
        _allocatedUnicastRanges = allocatedUnicastRanges.toMutableList(),
        _allocatedGroupRanges = allocatedGroupRanges.toMutableList(),
        _allocatedSceneRanges = allocatedSceneRanges.toMutableList(),
        _name = name
    )

    /**
     * Allocates the given range to a provisioner.
     *
     * @param range Allocated range could [UnicastRange], [GroupRange] or a [SceneRange].
     * @throws OverlappingProvisionerRanges if the given range is allocated to another provisioner.
     */
    @Throws(OverlappingProvisionerRanges::class, RangeAlreadyAllocated::class)
    fun allocate(range: Range) {
        // TODO clarify the api with iOS version
        when (range) {
            is UnicastRange -> allocate(range)
            is GroupRange -> allocate(range)
            is SceneRange -> allocate(range)
        }
    }

    /**
     * Allocates a list of ranges to a given provisioner.
     *
     * @param ranges List of ranges to be allocated.
     * @throws OverlappingProvisionerRanges if any of the given ranges overlap with another
     *                                       provisioner's allocated ranges.
     */
    @Throws(OverlappingProvisionerRanges::class, RangeAlreadyAllocated::class)
    fun allocate(ranges: List<Range>) {
        when (ranges.firstOrNull()) {
            is UnicastRange -> if (ranges.overlaps(otherUnicastRanges))
                throw OverlappingProvisionerRanges()

            is GroupRange -> if (ranges.overlaps(otherGroupRanges))
                throw OverlappingProvisionerRanges()

            is SceneRange -> if (ranges.overlaps(otherSceneRanges))
                throw OverlappingProvisionerRanges()

            else -> return
        }
        ranges.forEach {
            allocate(range = it)
        }
    }

    /**
     * Allocates the given unicast range to a provisioner.
     *
     * @param range Allocated unicast range.
     * @throws OverlappingProvisionerRanges if the given range is allocated to another provisioner.
     */
    @Throws(OverlappingProvisionerRanges::class, RangeAlreadyAllocated::class)
    fun allocate(range: UnicastRange) {
        // If the provisioner is not a part of network we don't have to validate for overlapping
        // unicast ranges. This will be validated when the provisioner is added to the network.
        network?.let { network ->
            if (!isRangeAvailableForAllocation(range = range)) {
                throw OverlappingProvisionerRanges()
            }
        }
        _allocatedUnicastRanges.add(range)
    }

    /**
     * Allocates the given group range to a provisioner.
     *
     * @param range Allocated group range.
     * @throws OverlappingProvisionerRanges if the given range is allocated to another provisioner.
     */
    @Throws(OverlappingProvisionerRanges::class, RangeAlreadyAllocated::class)
    fun allocate(range: GroupRange) {
        // If the provisioner is not a part of network we don't have to validate for overlapping
        // group ranges. This will be validated when the provisioner is added to the network.
        network?.let { network ->
            if (!isRangeAvailableForAllocation(range = range)) {
                throw OverlappingProvisionerRanges()
            }
        }
        _allocatedGroupRanges.add(range)
    }

    /**
     * Allocates the given scene range to a provisioner.
     *
     * @param range Allocated scene range.
     * @throws OverlappingProvisionerRanges if the given range is allocated to another provisioner.
     */
    @Throws(OverlappingProvisionerRanges::class, RangeAlreadyAllocated::class)
    fun allocate(range: SceneRange) {
        // If the provisioner is not a part of network we don't have to validate for overlapping
        // scene ranges. This will be validated when the provisioner is added to the network.
        network?.let { network ->
            if (!isRangeAvailableForAllocation(range = range)) {
                throw OverlappingProvisionerRanges()
            }
        }
        _allocatedSceneRanges.add(range)
    }

    /**
     * Updates the given range with the new range.
     *
     * @param range Range to be updated.
     * @param newRange New range.
     */
    fun update(range: Range, newRange: Range) {
        when (range) {
            is UnicastRange if newRange is UnicastRange -> update(range, newRange)
            is GroupRange if newRange is GroupRange -> update(range, newRange)
            is SceneRange if newRange is SceneRange -> update(range, newRange)
            else -> return
        }
    }

    /**
     * Updates the given unicast range with the new unicast range.
     *
     * @param range unicast range to be updated.
     * @param newRange new unicast range.
     */
    fun update(range: UnicastRange, newRange: UnicastRange) {
        _allocatedUnicastRanges.indexOf(range).takeIf { it != -1 }?.let {
            allocate(range = newRange)
        }
    }

    /**
     * Updates the given group range with the new group range.
     *
     * @param range Range to be updated.
     * @param newRange new group range.
     */
    fun update(range: GroupRange, newRange: GroupRange) {
        _allocatedGroupRanges.indexOf(range).takeIf { it != -1 }?.let {
            allocate(range = newRange)
        }
    }

    /**
     * Updates the given scene range with the new scene range.
     *
     * @param range scene range to be updated.
     * @param newRange new scene range.
     */
    fun update(range: SceneRange, newRange: SceneRange) {
        _allocatedSceneRanges.indexOf(range).takeIf { it != -1 }?.let {
            allocate(range = newRange)
        }
    }

    /**
     * Removes the given range from the allocated ranges.
     * @param range Range to be removed.
     */
    fun remove(range: Range) {
        when (range) {
            is UnicastRange -> remove(range)
            is GroupRange -> remove(range)
            is SceneRange -> remove(range)
        }
    }

    /**
     * Removes the given range from the allocated ranges.
     * @param range Range to be removed.
     */
    fun remove(range: UnicastRange) {
        _allocatedUnicastRanges.remove(range).also { network?.updateTimestamp() }
    }

    /**
     * Removes the given range from the allocated ranges.
     * @param range Range to be removed.
     */
    fun remove(range: SceneRange) {
        _allocatedSceneRanges.remove(range).also { network?.updateTimestamp() }
    }

    /**
     * Removes the given range from the allocated ranges.
     * @param range Range to be removed.
     */
    fun remove(range: GroupRange) {
        _allocatedGroupRanges.remove(range).also { network?.updateTimestamp() }
    }

    /**
     * Removes the given list of ranges from the allocated ranges.
     * @param ranges Ranges to be removed.
     */
    fun remove(ranges: List<Range>) = ranges.forEach { remove(range = it) }

    /**
     * Returns the maximum number of elements that can be assigned to a Node with the given Unicast
     * address.
     *
     * This method ensures that the addresses are ina single Unicast Address range allocated to the
     * Provisioner and are not already assigned to any other Node.
     *
     * @param address Node address to check
     * @return Maximum number of elements that the Node can have before the addresses go out of
     *         Provisioner's range, or will overlap an existing node.
     */
    internal fun maxElementCount(address: UnicastAddress): Int {
        var count = 0

        // CHeck the maximum number of elements that fits inside a single range.
        for (range in _allocatedUnicastRanges) {
            if (range.contains(address.address)) {
                count = minOf(
                    a = ((range.high - address.address) + 1u).toInt(),
                    b = UByte.MAX_VALUE.toInt()
                )
                break
            }
        }
        // The requested address is not in Provisioner's range
        require(count > 0) { return 0 }
        // If the Provisioner is not in Provisioner's range
        val otherNodes = network?.nodes?.filter { it.primaryUnicastAddress != address }
        val range = UnicastRange(address, count)
        otherNodes?.forEach { node ->
            if (node.containsElementsWithAddress(range)) {
                count = (node._primaryUnicastAddress - address).address.toInt()
            }
        }
        return count
    }

    /**
     * Checks if the given range is within the Allocated ranges.
     *
     * @param range Range to be checked.
     * @return true if the range is within the allocated range.
     */
    fun hasAllocatedRange(range: Range) = when (range) {
        is UnicastRange -> _allocatedUnicastRanges
        is GroupRange -> _allocatedGroupRanges
        is SceneRange -> _allocatedSceneRanges
    }.contains(range)

    /**
     * Checks if the current provisioner has overlapping unicast, group or scene ranges with the
     * given provisioner.
     *
     * @param provisioner Other provisioner.
     * @return true if there are any overlapping unicast, groups or scene ranges or false otherwise.
     */
    fun hasOverlappingRanges(provisioner: Provisioner) =
        hasOverlappingUnicastRanges(provisioner) ||
                hasOverlappingGroupRanges(provisioner) ||
                hasOverlappingSceneRanges(provisioner)

    /**
     * Checks if the current provisioner has overlapping unicast ranges with the given provisioner.
     *
     * @param other Other provisioner.
     * @return true if there are any overlapping unicast ranges or false otherwise.
     */
    fun hasOverlappingUnicastRanges(other: Provisioner) = _allocatedUnicastRanges.any { range ->
        other._allocatedUnicastRanges.any { otherRange -> otherRange.overlaps(range) }
    }

    /**
     * Checks if the current provisioner has overlapping group ranges with the given provisioner.
     *
     * @param other Other provisioner.
     * @return true if there are any overlapping group ranges or false otherwise.
     */

    fun hasOverlappingGroupRanges(other: Provisioner) = _allocatedGroupRanges.any { range ->
        other._allocatedGroupRanges.any { otherRange -> otherRange.overlaps(range) }
    }

    /**
     * Checks if the current provisioner has overlapping scene ranges with the given provisioner.
     *
     * @param other Other provisioner.
     * @return true if there are any overlapping scene ranges or false otherwise.
     */
    fun hasOverlappingSceneRanges(other: Provisioner) = _allocatedSceneRanges.any { range ->
        other._allocatedSceneRanges.any { otherRange -> otherRange.overlaps(range) }
    }

    /**
     * Assigns the unicast address to the given Provisioner. If the provisioner did not have a
     * unicast address assigned, the method will create a Node with the address. This will enable
     * configuration capabilities for the provisioner. The provisioner must be in the mesh network.
     *
     * @param                              address Unicast address to assign.
     * @throws DoesNotBelongToNetwork      If the provisioner does not belong to this network.
     * @throws AddressNotInAllocatedRanges If the address is not in the provisioner's allocated
     *                                     address range.
     * @throws AddressAlreadyInUse         If the address is already in use
     */
    @Throws(DoesNotBelongToNetwork::class)
    fun assign(address: UnicastAddress) {
        network?.let { network ->
            require(value = network.has(this@Provisioner)) { throw DoesNotBelongToNetwork() }
            var isNewNode = false
            val node = network.node(provisioner = this@Provisioner) ?: Node(
                provisioner = this@Provisioner,
                unicastAddress = address
            ).apply {
                assignNetKeys(network.networkKeys)
                assignAppKeys(network.applicationKeys)
                network.localProvisioner?.takeIf {
                    it.uuid == this@Provisioner.uuid
                }?.let {
                    add(elements = network.localElements)
                    companyIdentifier = 0x00E0u // Google
                    replayProtectionCount = maxUnicastAddress
                    name = this@Provisioner.name
                } ?: run {
                    add(element = Element.primaryElement)
                }
            }.also { isNewNode = true }

            // Is it in Provisioner's range?
            val newRange = UnicastRange(address = address, elementsCount = node.elementsCount)
            require(value = hasAllocatedRange(range = newRange)) {
                throw AddressNotInAllocatedRanges()
            }

            // Is there any other node using the address?
            require(value = network.isAddressAvailable(address = address, node = node)) {
                throw AddressAlreadyInUse()
            }

            when (isNewNode) {
                true -> network.add(node = node)
                else -> node._primaryUnicastAddress = address
            }
            network.updateTimestamp()
        }
    }

    /**
     * Disables the configuration capabilities by un-assigning provisioner's address. Un-assigning
     * an address will delete the provisioner's node. This results in the provisioner not being
     * able to send or receive mesh messages in the mesh network. However, the provisioner will
     * still retain it's provisioning capabilities.
     */
    fun disableConfigurationCapabilities() {
        network?.remove(uuid = uuid)
    }

    /**
     * Checks if the given range is allocatable a provisioner.
     *
     * @param range Ranges to be allocated.
     * @return true if the range is not in use by another provisioner or false otherwise.
     * @throws DoesNotBelongToNetwork if the provisioner does not belong to the network.
     */
    @Throws(DoesNotBelongToNetwork::class)
    fun isRangeAvailableForAllocation(range: Range): Boolean = network?.run {
        when (range) {
            is UnicastRange -> provisioners
                .filter { it.uuid != this@Provisioner.uuid }
                .none { it._allocatedUnicastRanges.overlaps(other = range) }

            is GroupRange -> provisioners
                .filter { it.uuid != this@Provisioner.uuid }
                .none { it._allocatedGroupRanges.overlaps(other = range) }

            is SceneRange -> provisioners
                .filter { it.uuid != this@Provisioner.uuid }
                .none { it._allocatedSceneRanges.overlaps(other = range) }
        }

    } ?: true

    /**
     * Check if the given list of ranges are allocatable to a provisioner.
     *
     * @param ranges Ranges to be allocated.
     * @return true if the given ranges are not in use by another provisioner or false otherwise.
     */
    fun areRangesAvailableForAllocation(ranges: List<Range>) = network?.run {
        when (ranges.firstOrNull()) {
            is UnicastRange ->
                provisioners
                    .filter { it.uuid != this@Provisioner.uuid }
                    .none { it._allocatedUnicastRanges.overlaps(ranges) }

            is GroupRange ->
                provisioners
                    .filter { it.uuid != this@Provisioner.uuid }
                    .none { it._allocatedGroupRanges.overlaps(ranges) }

            is SceneRange ->
                provisioners
                    .filter { it.uuid != this@Provisioner.uuid }
                    .none { it._allocatedSceneRanges.overlaps(ranges) }

            else -> false
        }
    } ?: false

}