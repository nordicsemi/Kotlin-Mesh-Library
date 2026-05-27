@file:Suppress(
    "unused", "EXPERIMENTAL_API_USAGE", "SERIALIZER_TYPE_INCOMPATIBLE",
    "ConvertArgumentToSet"
)

package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nordicsemi.kotlin.mesh.core.model.serialization.UShortAsStringSerializer

/**
 * Allocated Range.
 *
 * @property low       Low value for a given range.
 * @property high      High value for a given  range.
 */
@Serializable
sealed class Range {
    internal abstract val range: UIntRange
    internal abstract val diff: UInt
    val low: UShort
        get() = range.first.toUShort()
    val high: UShort
        get() = range.last.toUShort()

    /**
     * Checks if the given value is within the range.
     *
     * @param value Value to be checked.
     * @return true if the value is in the range and false otherwise.
     */
    fun contains(value: UShort) = range.contains(value)

    /**
     * Checks if the given value is contained within the range.
     *
     * @param other Range to be checked.
     * @return true if the value is in the range and false otherwise.
     */
    fun contains(other: Range) = range.contains(other.range)

    /**
     * Returns the overlapping region of a given range
     *
     * @param other Range to be checked.
     * @return the overlapping range or null if there is no overlap.
     */
    fun overlap(other: Range) = if (overlaps(other)) {
        val overlap = range.intersect(other.range).toList()
        if (overlap.isNotEmpty()) {
            when (other) {
                is UnicastRange -> UnicastRange(
                    lowAddress = UnicastAddress(overlap.first().toUShort()),
                    highAddress = UnicastAddress(overlap.last().toUShort())
                )

                is GroupRange -> GroupRange(
                    lowAddress = GroupAddress(overlap.first().toUShort()),
                    highAddress = GroupAddress(overlap.last().toUShort())
                )

                is SceneRange -> SceneRange(
                    firstScene = overlap.first().toUShort(),
                    lastScene = overlap.last().toUShort()
                )
            }
        } else null
    } else null

    fun overlap(other: List<Range>) = other.mapNotNull { overlap(it) }

    /**
     * Checks if the given range overlaps.
     *
     *
     * @param other Range to check for overlapping elements.
     * @return true if there are overlapping elements.
     */
    fun overlaps(other: Range): Boolean = contains(other.low) || contains(other.high) ||
            other.contains(low) || other.contains(high)

    /**
     * Checks if the given list of ranges overlaps with the current range
     *
     * @param otherRanges List of ranges to check for overlapping elements.
     * @return true if there are overlapping elements.
     */
    fun overlaps(otherRanges: List<Range>) = otherRanges.any { it.overlaps(this) }

    /**
     * Returns the closest distance between this and the given range.
     *
     * When range 1 ends at 0x1000 and range 2 starts at 0x1002, the distance between them is 1. If
     * the range 2 starts at 0x0001, the distance is 0 and they can be merged. If ranges overlap
     * each other, the distance is 0.
     *
     * @param other The range to check distance to.
     * @return The distance between ranges in units.
     */
    fun distance(other: Range) = when {
        high < other.low -> (other.low - high - 1u).toInt()
        low > other.high -> (low - other.high - 1u).toInt()
        else -> 0
    }

    /**
     * Returns the minimum of the given values.
     *
     * @param a UShort value.
     * @param b UShort value.
     * @return the minimum value.
     */
    private fun min(a: UShort, b: UShort): UShort = kotlin.math.min(a.toInt(), b.toInt()).toUShort()

    /**
     * Returns the maximum of the given values.
     *
     * @param a UShort value.
     * @param b UShort value.
     * @return the maximum value.
     */
    private fun max(a: UShort, b: UShort): UShort = kotlin.math.max(a.toInt(), b.toInt()).toUShort()

    /**
     * Adds a range to another.
     *
     * @param other Range to be added.
     * @return a list of ranges.
     */
    operator fun plus(other: Range) = when (distance(other) == 0) {
        true -> listOf(
            when {
                this is UnicastRange && other is UnicastRange -> UnicastAddress(
                    min(
                        low,
                        other.low
                    )
                )..UnicastAddress(max(high, other.high))

                this is GroupRange && other is GroupRange -> GroupAddress(
                    min(
                        low,
                        other.low
                    )
                )..GroupAddress(max(high, other.high))

                this is SceneRange && other is SceneRange -> SceneRange(
                    min(low, other.low), max(high, other.high)
                )

                else -> throw IllegalArgumentException(
                    "Left and Right ranges must be of same range type!"
                )
            }
        )

        false -> listOf(this, other)
    }

    /**
     * Removes one range from the other.
     *
     * @param other Range to be added.
     * @return a list of ranges.
     */
    operator fun minus(other: Range): List<Range> {
        var result = listOf<Range>()
        // Left:   |------------|                  |-----------|                 |---------|
        //                  -                            -                            -
        // Right:      |-----------------|   or                   |---|   or        |----|
        //                  =                            =                            =
        // Result: |---|                           |-----------|                 |--|
        // Left:   |------------|                  |-----------|                 |---------|
        //                  -                            -                            -
        // Right:      |-----------------|   or                   |---|   or        |----|
        //                  =                            =                            =
        // Result: |---|                           |-----------|                 |--|
        if (other.low > low) {
            result = result + when {
                this is UnicastRange && other is UnicastRange -> UnicastAddress(low)..UnicastAddress(
                    min(high, (other.low - 1u).toUShort())
                )

                this is GroupRange && other is GroupRange -> GroupAddress(low)..GroupAddress(
                    min(
                        high,
                        (other.low - 1u).toUShort()
                    )
                )

                this is SceneRange && other is SceneRange -> SceneRange(
                    low,
                    min(high, (other.low - 1u).toUShort())
                )

                else -> throw IllegalArgumentException(
                    "Left and Right ranges must be of same range type!"
                )
            }
        }
        // Left:                |----------|             |-----------|                   |--------|
        //                         -                          -                           -
        // Right:      |----------------|           or       |----|        or     |---|
        //                         =                          =                           =
        // Result:                      |--|                      |--|                   |--------|
        if (other.high < high) {
            result = result + when {
                this is UnicastRange && other is UnicastRange -> UnicastAddress(
                    max(
                        (other.high + 1u).toUShort(),
                        low
                    )
                )..UnicastAddress(high)

                this is GroupRange && other is GroupRange -> GroupAddress(
                    max(
                        (other.high + 1u).toUShort(),
                        low
                    )
                )..GroupAddress(high)

                this is SceneRange && other is SceneRange -> SceneRange(
                    max(
                        (other.high + 1u).toUShort(),
                        low
                    ), high
                )

                else -> throw IllegalArgumentException(
                    "Left and Right ranges must be of same range type!"
                )
            }
        }
        return result
    }
}

/**
 * Allocated address range.
 *
 * @property lowAddress       Low value for a given range.
 * @property highAddress      High value for a given  range.
 */
@Serializable
sealed class AddressRange : Range() {
    abstract val lowAddress: MeshAddress
    abstract val highAddress: MeshAddress

    override val range
        get() = lowAddress.address..highAddress.address
    override val diff
        get() = highAddress.address - lowAddress.address
}

/**
 * The AllocatedUnicastRange represents the range of unicast addresses that the Provisioner can
 * allocate to new devices when they are provisioned onto the mesh network, without needing to
 * coordinate the node additions with other Provisioners. The lowAddress and highAddress represent
 * values from 0x0001 to 0x7FFF. The value of the lowAddress property shall be less than or equal to
 * the value of the highAddress property.
 *
 * @property lowAddress        Low address for a given range.
 * @property highAddress       High address for a given  range.
 * @constructor Creates a Unicast Range.
 */
@Serializable
data class UnicastRange(
    override val lowAddress: UnicastAddress,
    override val highAddress: UnicastAddress,
) : AddressRange() {

    /**
     * Convenience constructor to create a Unicast Range for tests.
     *
     * @param address Low address for a given range.
     * @param elementsCount Number of elements in the range.
     * @constructor constructs Unicast Range
     */
    constructor(
        address: UnicastAddress, elementsCount: Int,
    ) : this(
        lowAddress = address,
        highAddress = address + if (elementsCount > 0) elementsCount - 1 else 0
    )

    /**
     * Convenience constructor to create a Unicast Range for tests.
     *
     * @param range Range to be converted to Unicast Range.
     * @constructor constructs Unicast Range
     */
    constructor(
        range: UIntRange,
    ) : this(start = range.first.toInt(), end = range.last.toInt())

    /**
     * Convenience constructor to create a Unicast Range for tests.
     *
     * @property start        Start address.
     * @property end          End address.
     * @constructor Creates a Unicast Range.
     */
    internal constructor(
        start: Int,
        end: Int,
    ) : this(lowAddress = UnicastAddress(start), highAddress = UnicastAddress(end))

}

/**
 * The AllocatedGroupRange represents the range of group addresses that the Provisioner can allocate
 * to newly created groups, without needing to coordinate the group additions with other
 * Provisioners. The lowAddress and highAddress properties represent values from 0xC000 to 0xFEFF.
 * The value of the lowAddress property shall be less than or equal to the value of the highAddress
 * property.
 *
 * @property lowAddress        Low address for a given range.
 * @property highAddress       High address for a given  range.
 * @constructor Creates a Group Range.
 */
@Serializable
data class GroupRange(
    override val lowAddress: GroupAddress,
    override val highAddress: GroupAddress,
) : AddressRange() {

    /**
     * Convenience constructor to create a Group Range.
     *
     * @param address Low address for a given range.
     * @param size Number of addresses in the range.
     */
    constructor(
        address: GroupAddress,
        size: Int,
    ) : this(lowAddress = address, highAddress = address + size)

    /**
     * Convenience constructor to create a Group Range.
     *
     * @param range Range to be converted to Group Range.
     * @constructor constructs Group Range
     */
    constructor(
        range: UIntRange,
    ) : this(start = range.first.toInt(), end = range.last.toInt())

    /**
     * Convenience constructor to create a Group Range.
     *
     * @property start        Start address.
     * @property end          End address.
     * @constructor Creates a Group Range.
     */
    internal constructor(
        start: Int,
        end: Int,
    ) : this(lowAddress = GroupAddress(start), highAddress = GroupAddress(end))
}

/**
 * The AllocatedSceneRange represents the range of scene numbers that the Provisioner can use to
 * register new scenes in the mesh network, without needing to coordinate the allocated scene
 * numbers with other Provisioners. The firstScene and lastScene represents values from 0x0001 to
 * 0xFFFF. The value of the firstScene property shall be less than or equal to the value of the
 * lastScene property.
 *
 * @property firstScene    First scene a given range.
 * @property lastScene     Last scene for a given  range.
 * @constructor Creates a Scene Range.
 */
@Serializable
data class SceneRange(
    @Serializable(with = UShortAsStringSerializer::class) val firstScene: SceneNumber,
    @Serializable(with = UShortAsStringSerializer::class) val lastScene: SceneNumber,
) : Range() {

    /**
     * Convenience constructor to create a Scene Range.
     *
     * @param firstScene First scene for a given range.
     * @param lastScene  Number of scenes in the range.
     */
    internal constructor(firstScene: Int, lastScene: Int) : this(
        firstScene = firstScene.toUShort(),
        lastScene = lastScene.toUShort()
    )

    /**
     * Convenience constructor to create a Scene Range.
     *
     * @param range Range to be converted to Scene Range.
     * @constructor constructs Scene Range
     */
    constructor(range: UIntRange) : this(
        firstScene = range.first.toUShort(),
        lastScene = range.last.toUShort()
    )

    @Transient
    override var range = firstScene..lastScene

    override val diff
        get() = high - low
}

/**
 * Returns a list of overlapping regions for a given range.
 *
 * @param range Range to be checked.
 * @return the overlapping region or null.
 */
fun List<Range>.overlap(range: Range) = mapNotNull { it.overlap(range) }

/**
 * Returns the overlapping region for a given range.
 *
 * @param other Range to be checked.
 * @return the overlapping region or null.
 */
fun List<Range>.overlap(other: List<Range>) = mapNotNull { other.overlap(it) }.flatten()

/**
 * Checks if an element in the list of ranges overlaps with the given range.
 *
 * @param other Range to be checked.
 * @return true if the given range overlaps with any of the ranges in the list.
 */
fun List<Range>.overlaps(other: Range) = any { it.overlaps(other) }

/**
 * Checks if the elements in the list of ranges overlaps with the given ranges.
 *
 * @param other Ranges to be checked.
 * @return true if the given list of ranges overlaps with any of the ranges in the list.
 */
fun List<Range>.overlaps(other: List<Range>) = any { it.overlaps(other) }

/**
 *  Checks if the given range is within the range.
 *
 *  @param other Range to be checked.
 *  @return true if the given range is within the range.
 */
operator fun UIntRange.contains(other: UIntRange) = contains(other.first) && contains(other.last)

/**
 * Checks if an element in the list of ranges contains the given range.
 *
 * @param range Range to be checked.
 * @return true if the given range overlaps with any of the ranges in the list.
 */
@Suppress("EXTENSION_SHADOWED_BY _MEMBER")
operator fun List<Range>.contains(range: Range) = any { it.contains(range) }

operator fun List<Range>.plus(other: Range): List<Range> {
    val result = ArrayList<Range>(size + 1)
    result.addAll(this)
    result.add(other)
    return result.merged()
}

operator fun List<Range>.plus(other: List<Range>): List<Range> {
    val result = mutableListOf<Range>()
    result.addAll(this)
    result += other
    return result
}

operator fun MutableList<Range>.plusAssign(other: Range) {
    this.add(other)
    this.merged()
}

operator fun MutableList<Range>.plusAssign(other: List<Range>) {
    this.addAll(other)
    this.merged()
}

operator fun List<Range>.minus(other: Range): List<Range> {
    val result = mutableListOf<Range>()
    result.addAll(this)
    result -= other
    return result.toList()
}

operator fun List<Range>.minus(other: List<Range>): List<Range> {
    val result = mutableListOf<Range>()
    result.addAll(this)
    result -= other
    return result.toList()
}

operator fun MutableList<Range>.minusAssign(other: Range) {
    val result = mutableListOf<Range>()
    //result.addAll(this)
    for (range in this) {
        result += range - other
    }
    clear()
    addAll(result)
}

operator fun MutableList<Range>.minusAssign(other: List<Range>) {
    other.map { this -= it }
}

/**
 * Merges any overlapping ranges within a given list of ranges.
 *
 * @return List of merged ranges.
 */
fun List<Range>.merged(): List<Range> {
    if (size <= 1) return this
    val result = mutableListOf<Range>()
    var accumulator: Range? = null

    for (range in sortedWith { r0, r1 -> r0.low.compareTo(r1.low) }) {
        if (accumulator == null) {
            accumulator = range
        }
        if (accumulator.high >= range.high) {
            // Do nothing
        } else if (accumulator.high + 1u >= range.low) {
            accumulator = when (accumulator) {
                is UnicastRange -> UnicastAddress(accumulator.low)..UnicastAddress(range.high)
                is GroupRange -> GroupAddress(accumulator.low)..GroupAddress(range.high)
                is SceneRange -> SceneRange(accumulator.low, range.high)
            }
        } else {
            result.add(accumulator)
            accumulator = range
        }
    }

    if (accumulator != null) {
        result.add(accumulator)
    }
    return result.toList()
}