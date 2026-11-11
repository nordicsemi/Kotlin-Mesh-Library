package no.nordicsemi.kotlin.mesh.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ProvisionerMoveTest {

    private val network = MeshNetwork(
        name = "Test Network",
        networkKeys = mutableListOf(NetworkKey())
    )
    private val provisioners = listOf("A", "B", "C", "D", "E").mapIndexed { index, name ->
        Provisioner(
            name = name,
            allocatedUnicastRanges = mutableListOf(
                UnicastRange(index * 0x1000 + 1, index * 0x1000 + 0x0FFF)
            )
        ).also {
            network.add(
                provisioner = it,
                address = UnicastAddress((index * 0x1000 + 1).toUShort())
            )
        }
    }

    private fun order() = network.provisioners.map { it.name }

    @Test
    fun testMoveDownByMoreThanOnePlace() {
        // The provisioner ends up at the requested index.
        network.move(provisioner = provisioners[1], to = 4)
        assertEquals(expected = listOf("A", "C", "D", "E", "B"), actual = order())
    }

    @Test
    fun testMoveDownByOnePlace() {
        network.move(provisioner = provisioners[1], to = 2)
        assertEquals(expected = listOf("A", "C", "B", "D", "E"), actual = order())
    }

    @Test
    fun testMoveUp() {
        network.move(provisioner = provisioners[4], to = 2)
        assertEquals(expected = listOf("A", "B", "E", "C", "D"), actual = order())
    }

    @Test
    fun testMoveToSameIndexDoesNothing() {
        network.move(provisioner = provisioners[2], to = 2)
        assertEquals(expected = listOf("A", "B", "C", "D", "E"), actual = order())
    }
}
