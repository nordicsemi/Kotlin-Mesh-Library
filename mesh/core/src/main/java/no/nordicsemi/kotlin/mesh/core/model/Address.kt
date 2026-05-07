@file:Suppress("unused", "SERIALIZER_TYPE_INCOMPATIBLE")

package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.Serializable
import no.nordicsemi.kotlin.data.HexString
import no.nordicsemi.kotlin.data.toUuid
import no.nordicsemi.kotlin.mesh.core.model.serialization.MeshAddressSerializer
import no.nordicsemi.kotlin.mesh.crypto.Crypto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Type alias for an unsigned 16-bit address.
 */
typealias Address = UShort

const val minUnicastAddress: Address = 0x0001u
const val maxUnicastAddress: Address = 0x7FFFu

//TODO is this really needed?
private const val minVirtualAddress: Address = 0x8000u
private const val maxVirtualAddress: Address = 0xBFFFu

const val minGroupAddress: Address = 0xC000u
const val maxGroupAddress: Address = 0xFEFFu

//TODO is this really needed?
internal const val unassignedAddress: Address = 0x0000u
private const val allProxies: Address = 0xFFFCu
private const val allFriends: Address = 0xFFFDu
private const val allRelays: Address = 0xFFFEu
internal const val allNodes: Address = 0xFFFFu

val fixedGroupAddresses = listOf<FixedGroupAddress>(AllNodes, AllRelays, AllProxies, AllFriends)

/**
 * An interface containing a property type address.
 *
 * @property address Unsigned 16-bit [Address].
 */
sealed interface HasAddress {
    val address: Address

    /**
     * Converts a mesh address to a hex string.
     *
     * @return The hex string representation of the address.
     */
    fun toHexString(): HexString
}

/**
 * Wrapper class for [Address].
 */
@Serializable(with = MeshAddressSerializer::class)
sealed class MeshAddress : HasAddress {
    abstract override val address: Address

    override fun toString(): String = toHexString()

    @OptIn(ExperimentalStdlibApi::class)
    override fun toHexString(): HexString = address.toHexString(
        HexFormat {
            number {
                prefix = "0x"
                minLength = 4
                upperCase = true
            }
        }
    )

    companion object {

        /**
         * Creates a Mesh address of type Unassigned, Unicast or Group address using the given
         * address value.
         *
         * @param address Address value.
         * @throws IllegalArgumentException If the given address value is not a valid Unassigned,
         *                                  Unicast or a Group address.
         */
        fun create(address: Address): MeshAddress = when {
            UnassignedAddress.isValid(address = address) -> UnassignedAddress
            UnicastAddress.isValid(address = address) -> UnicastAddress(address = address)
            GroupAddress.isValid(address = address) -> GroupAddress(address = address)
            AllProxies.isValid(address = address) -> AllProxies
            AllFriends.isValid(address = address) -> AllFriends
            AllRelays.isValid(address = address) -> AllRelays
            AllNodes.isValid(address = address) -> AllNodes
            else -> throw IllegalArgumentException(
                "Unable to create an Address for the given address value!"
            )
        }

        /**
         * Creates a Mesh address of type Unassigned, Unicast or Group address using the given
         * address value.
         *
         * @param address Address value.
         * @throws IllegalArgumentException If the given address value is not a valid Unassigned,
         *                                  Unicast or a Group address.
         */
        fun create(address: Int): MeshAddress = create(address.toUShort())

        /**
         * Creates a virtual address using the given Uuid Label.
         *
         * @param uuid Uuid Label.
         */
        @OptIn(ExperimentalUuidApi::class)
        fun create(uuid: Uuid) = VirtualAddress(uuid = uuid)
    }
}

/**
 * The unassigned address has the value 0x0000.
 */
@Serializable(with = MeshAddressSerializer::class)
object UnassignedAddress : MeshAddress(),
    ParentGroupAddress,
    PublicationAddress,
    SubscriptionAddress,
    HeartbeatSubscriptionSource,
    HeartbeatPublicationDestination,
    HeartbeatSubscriptionDestination {
    override val address = unassignedAddress

    fun isValid(address: Address): Boolean = address == unassignedAddress
}

/**
 * A unicast address is a unique address allocated to each element. A unicast address has bit 15 set
 * to 0. The unicast address shall not have the value 0x0000, and therefore can have any value from
 * 0x0001 to 0x7FFF inclusive.
 *
 * @property address  16-bit address of the unicast address.
 * @throws IllegalArgumentException If the given address value is not a valid Unicast address.
 */
@Serializable(with = MeshAddressSerializer::class)
data class UnicastAddress(
    override val address: Address,
) : MeshAddress(),
    PublicationAddress,
    HeartbeatPublicationDestination,
    HeartbeatSubscriptionSource,
    HeartbeatSubscriptionDestination,
    ProxyFilterAddress {

    constructor(address: Int) : this(address = address.toUShort())

    init {
        require(isValid(address)) {
            "A valid unicast address must range from $minUnicastAddress to $maxUnicastAddress!"
        }
    }

    override fun toString() = super.toString()

    operator fun plus(other: Int) = UnicastAddress(address = address.toInt() + other)

    operator fun plus(other: UnicastAddress) = UnicastAddress(
        address = (address + other.address).toInt()
    )

    operator fun minus(other: Int) = UnicastAddress(address = address.toInt() - other)

    operator fun minus(other: UnicastAddress) = UnicastAddress(
        address = (address - other.address).toInt()
    )

    operator fun compareTo(o: UnicastAddress) = address.compareTo(other = o.address)

    operator fun rangeTo(o: UnicastAddress) = UnicastRange(lowAddress = this, highAddress = o)

    companion object {
        fun isValid(address: Address) = address in minUnicastAddress..maxUnicastAddress
    }
}

/**
 * A virtual address represents a set of destination addresses. Each virtual address logically
 * represents a Label Uuid,
 * which is a 128-bit value that does not have to be managed centrally. One or more elements may be
 * programmed to publish or subscribe to a Label Uuid. The Label Uuid is not transmitted and shall
 * be used as the Additional Data field of the message integrity check value in the upper transport
 * layer.
 *
 * @property Uuid     Uuid label of the virtual address.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable(with = MeshAddressSerializer::class)
data class VirtualAddress(
    val uuid: Uuid,
) : MeshAddress(),
    PrimaryGroupAddress,
    ParentGroupAddress,
    PublicationAddress,
    SubscriptionAddress,
    HeartbeatPublicationDestination,
    ProxyFilterAddress {

    @OptIn(ExperimentalUuidApi::class)
    override val address: Address = Crypto.createVirtualAddress(uuid = uuid)

    /**
     * Creates a virtual address using the given byte array.
     * @param label Byte array containing the Uuid label.
     * @throws IllegalArgumentException If the byte array containing the label is not 16 bytes long.
     */
    @OptIn(ExperimentalUuidApi::class)
    constructor(label: ByteArray) : this(uuid = label.toUuid())

    operator fun compareTo(o: VirtualAddress) = address.compareTo(other = o.address)
}

/**
 * A group address is an address that is programmed into zero or more elements. A group address has
 * bit 15 set to 1 and bit 14 set to 1. Group addresses in the range 0xFF00 through 0xFFFF are
 * reserved for [FixedGroupAddress], and addresses in the range 0xC000 through 0xFEFF are generally
 * available for other usage.
 *
 * @constructor Creates a Group Address object.
 * @throws IllegalArgumentException If the given address value is not a valid Group address.
 */
@Serializable(with = MeshAddressSerializer::class)
data class GroupAddress(
    override val address: Address,
) : MeshAddress(),
    PrimaryGroupAddress,
    ParentGroupAddress,
    PublicationAddress,
    SubscriptionAddress,
    HeartbeatPublicationDestination,
    HeartbeatSubscriptionDestination,
    ProxyFilterAddress {

    constructor(address: Int) : this(address = address.toUShort())

    init {
        require(isValid(address)) {
            "A valid group address must range from $minGroupAddress to $maxGroupAddress!"
        }
    }

    operator fun plus(o: Int): GroupAddress = GroupAddress(address = address.toInt() + o)

    operator fun minus(other: Int) = GroupAddress(address = address.toInt() - other)

    operator fun compareTo(o: GroupAddress) = address.compareTo(other = o.address)

    operator fun compareTo(o: PrimaryGroupAddress) = address.compareTo(o.address)

    operator fun compareTo(o: ParentGroupAddress) = address.compareTo(other = o.address)

    operator fun rangeTo(o: GroupAddress) = GroupRange(lowAddress = this, highAddress = o)

    companion object {
        fun isValid(address: Address) = address in minGroupAddress..maxGroupAddress
    }
}

/**
 * There are two types of group address; those that can be assigned dynamically and those that are
 * fixed. Fixed group addresses are in the range of 0xFF00 through 0xFFFF.
 */
@Serializable
sealed class FixedGroupAddress(override val address: Address) : MeshAddress(), ProxyFilterAddress,
    PublicationAddress, HeartbeatSubscriptionDestination, HeartbeatPublicationDestination

/**
 * A message sent to the all-proxies address shall be processed by the primary element of all nodes
 * that have the proxy functionality enabled.
 */
object AllProxies : FixedGroupAddress(address = allProxies), SubscriptionAddress {
    fun isValid(address: Address) = address == allProxies
}

/**
 * A message sent to the all-friends address shall be processed by the primary element of all nodes
 * that have the friend functionality enabled.
 */
object AllFriends : FixedGroupAddress(address = allFriends), SubscriptionAddress {
    fun isValid(address: Address) = address == allFriends
}

/**
 * A message sent to the all-relays address shall be processed by the primary element of all nodes
 * that have the relay functionality enabled.
 */
object AllRelays : FixedGroupAddress(address = allRelays), SubscriptionAddress {
    fun isValid(address: Address) = address == allRelays
}

/**
 * A message sent to the all-nodes address shall be processed by the primary element of all nodes.
 *
 * Note: AllNodes cannot be used as subscription address.
 */
data object AllNodes : FixedGroupAddress(address = allNodes), SubscriptionAddress {
    fun isValid(address: Address) = address == allNodes
}

/**
 * An address that is used as a destination address by a Heartbeat Publication or a Heartbeat
 * Subscription message. This represents a [UnicastAddress], [GroupAddress].
 */
@Serializable(with = MeshAddressSerializer::class)
sealed interface HeartbeatDestination : HasAddress

/**
 * Heartbeat publication destination address for heartbeat messages. This represents a
 * [UnicastAddress], [GroupAddress] or an [UnassignedAddress].
 */
@Serializable(with = MeshAddressSerializer::class)
sealed interface HeartbeatPublicationDestination : HeartbeatDestination {

    companion object {
        fun isValid(address: Address) =
            UnicastAddress.isValid(address = address) ||
                    GroupAddress.isValid(address = address) ||
                    UnassignedAddress.isValid(address = address)
    }
}

/**
 * Heartbeat subscription source address for heartbeat messages. This represents a [UnicastAddress]
 * or a [UnassignedAddress] if Heartbeat subscriptions are disabled.
 */
@Serializable(with = MeshAddressSerializer::class)
sealed interface HeartbeatSubscriptionSource : HasAddress

/**
 * Heartbeat subscription destination address for heartbeat messages. This represents a
 * [UnicastAddress], [GroupAddress] or a [UnassignedAddress] if Heartbeat subscriptions are
 * disabled.
 */
@Serializable(with = MeshAddressSerializer::class)
sealed interface HeartbeatSubscriptionDestination : HeartbeatDestination

/**
 * An address a model may publish to. This represents a [UnicastAddress], [GroupAddress]
 * or a [VirtualAddress].
 */
@Serializable(with = MeshAddressSerializer::class)
sealed interface PublicationAddress : HasAddress

/**
 * An address a model may subscribe to. This represents a [GroupAddress], [VirtualAddress],
 * [AllProxies], [AllFriends] or an [AllRelays] address.
 */
@Serializable(with = MeshAddressSerializer::class)
sealed interface SubscriptionAddress : HasAddress

/**
 * An address type used to identify a [GroupAddress] or a [VirtualAddress] that's used to create a
 * group. Primary group address cannot be a fixed group address and not allocatable to a provisioner
 * as a range.
 */
@Serializable(with = MeshAddressSerializer::class)
sealed interface PrimaryGroupAddress : HasAddress

/**
 * An address type used to identify a [GroupAddress], [VirtualAddress] or an [UnassignedAddress]
 * that's used as a parent address of a group. Parent group address cannot be a fixed group address
 * and not allocatable to a provisioner as a range.
 */
@Serializable(with = MeshAddressSerializer::class)
sealed interface ParentGroupAddress : HasAddress

/**
 * An address type that can be added to a Proxy Filter List. This represents a [UnicastAddress],
 * [GroupAddress], [VirtualAddress] or a [FixedGroupAddress].
 */
@Serializable(with = MeshAddressSerializer::class)
sealed interface ProxyFilterAddress : HasAddress
