package no.nordicsemi.kotlin.mesh.core.util

import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.Node
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi

/**
 * Tests [PrivateNodeIdentity] matching against the Mesh Protocol sample data in section 8.6.4.
 */
@OptIn(ExperimentalUuidApi::class)
class PrivateNodeIdentityTest {

    private val netKey = "7dd7364cd842ad18c17c2b820c84c3d6".hexToByteArray()

    private fun nodeWithAddress(address: Int): Node {
        val network = MeshNetwork(
            name = "Test Network",
            networkKeys = mutableListOf(NetworkKey(key = netKey))
        )
        val node = Node(name = "Node", address = address, elements = 1)
        network.add(node = node)
        return node
    }

    // Mesh Protocol 8.6.4: Service data using Private Node Identity.
    @Test
    fun testPrivateNodeIdentityMatchesSampleData() {
        val identity = "032c64a8cbca65bfe134ae608fbbc1f2c6".hexToByteArray().nodeIdentity()

        assertIs<PrivateNodeIdentity>(identity)
        assertTrue(identity.matches(node = nodeWithAddress(address = 0x1201)))
    }

    @Test
    fun testPrivateNodeIdentityDoesNotMatchOtherAddress() {
        val identity = "032c64a8cbca65bfe134ae608fbbc1f2c6".hexToByteArray().nodeIdentity()

        assertNotNull(identity)
        assertFalse(identity.matches(node = nodeWithAddress(address = 0x1202)))
    }
}
