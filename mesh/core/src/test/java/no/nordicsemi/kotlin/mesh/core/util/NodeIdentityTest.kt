package no.nordicsemi.kotlin.mesh.core.util

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.crypto.Crypto
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NodeIdentityTest {

    private val networkKey = NetworkKey()
    private val node = Node(name = "Node", address = 0x1234, elements = 1)
    private val random = ByteArray(8) { (0xA0 + it).toByte() }

    init {
        MeshNetwork(name = "Test", networkKeys = mutableListOf(networkKey)).add(node = node)
    }

    private val address = node.primaryUnicastAddress.address.toByteArray()
    private val identityKey = networkKey.derivatives.identityKey

    @Test
    fun testNodeIdentityMatchesNode() {
        // Hash = e(IdentityKey, 48 bits of padding || Random || Address)
        val hash = Crypto.calculateHash(ByteArray(6) + random + address, identityKey)
        val identity = (byteArrayOf(0x01) + hash + random).nodeIdentity()
        assertIs<PublicNodeIdentity>(identity)
        assertTrue(identity.matches(node))
    }

    @Test
    fun testPrivateNodeIdentityMatchesNode() {
        // Hash = e(IdentityKey, 40 bits of padding || 0x03 || Random || Address)
        val hash = Crypto.calculateHash(
            ByteArray(5) + 0x03.toByte() + random + address, identityKey
        )
        val identity = (byteArrayOf(0x03) + hash + random).nodeIdentity()
        assertIs<PrivateNodeIdentity>(identity)
        assertTrue(identity.matches(node))
    }

    @Test
    fun testNodeIdentityDoesNotMatchPrivateNodeIdentity() {
        val hash = Crypto.calculateHash(ByteArray(6) + random + address, identityKey)
        val identity = (byteArrayOf(0x03) + hash + random).nodeIdentity()
        assertNotNull(identity)
        assertFalse(identity.matches(node))
    }
}
