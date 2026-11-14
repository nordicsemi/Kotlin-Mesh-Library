package no.nordicsemi.kotlin.mesh.core.util

import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.crypto.Crypto
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NetworkIdentityTest {

    private val networkKey = NetworkKey()
    private val random = ByteArray(8) { (0xA0 + it).toByte() }

    @Test
    fun testNetworkIdentityMatchesNetworkKey() {
        val identity = (byteArrayOf(0x00) + networkKey.networkId).networkIdentity()
        assertIs<PublicNetworkIdentity>(identity)
        assertTrue(identity.matches(networkKey))
        assertFalse(identity.matches(NetworkKey()))
    }

    @Test
    fun testPrivateNetworkIdentityMatchesNetworkKey() {
        // Hash = e(IdentityKey, Network ID || Random)
        val hash = Crypto.calculateHash(
            networkKey.networkId + random, networkKey.derivatives.identityKey
        )
        val identity = (byteArrayOf(0x02) + hash + random).networkIdentity()
        assertIs<PrivateNetworkIdentity>(identity)
        assertContentEquals(hash, identity.hash)
        assertContentEquals(random, identity.random)
        assertTrue(identity.matches(networkKey))
        assertFalse(identity.matches(NetworkKey()))
    }

    @Test
    fun testMalformedNetworkIdentityIsIgnored() {
        assertNull(byteArrayOf(0x02, 1, 2, 3, 4, 5, 6, 7, 8).networkIdentity())
        assertNull(byteArrayOf(0x01, 1, 2, 3, 4, 5, 6, 7, 8).networkIdentity())
    }
}
