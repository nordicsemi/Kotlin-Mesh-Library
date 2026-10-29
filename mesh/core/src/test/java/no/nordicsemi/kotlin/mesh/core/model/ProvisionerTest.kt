package no.nordicsemi.kotlin.mesh.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class ProvisionerTest {

    private val provisioner = Provisioner(Uuid.random())
    private val other = Provisioner(Uuid.random())

    @Test
    fun testHasOverlappingUnicastRanges_1() {
        // Non overlapping ranges.
        provisioner.allocate(UnicastAddress(1u)..UnicastAddress(10u))
        other.allocate(UnicastAddress(20u)..UnicastAddress(30u))
        assertEquals(false, provisioner.hasOverlappingUnicastRanges(other))

        other.allocate(UnicastAddress(1u)..UnicastAddress(10u))
        assertEquals(true, provisioner.hasOverlappingUnicastRanges(other))
    }

    @Test
    fun testHasOverlappingGroupRanges_2() {
        // Non overlapping ranges.
        provisioner.allocate(GroupAddress(49152u)..GroupAddress(49200u))
        other.allocate(GroupAddress(49250u)..GroupAddress(49350u))
        assertEquals(false, provisioner.hasOverlappingGroupRanges(other))

        other.allocate(GroupAddress(49152u)..GroupAddress(49200u))
        assertEquals(true, provisioner.hasOverlappingGroupRanges(other))
    }

    @Test
    fun testHasOverlappingSceneRanges_3() {
        // Non overlapping ranges.
        provisioner.allocate(SceneRange(1u, 10u))
        other.allocate(SceneRange(20u, 30u))
        assertEquals(false, provisioner.hasOverlappingSceneRanges(other))

        // Overlapping ranges
        other.allocate(SceneRange(1u, 10u))
        assertEquals(true, provisioner.hasOverlappingSceneRanges(other))
    }

    @Test
    fun testHasOverlappingRanges_4() {
        // Non overlapping ranges.
        assertEquals(false, provisioner.hasOverlappingRanges(other))
    }
}
