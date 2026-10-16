@file:Suppress("LocalVariableName")

package no.nordicsemi.kotlin.mesh.crypto

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.data.toHexString
import no.nordicsemi.kotlin.mesh.crypto.Crypto.calculateS1
import no.nordicsemi.kotlin.mesh.crypto.Crypto.calculateS2
import no.nordicsemi.kotlin.mesh.crypto.Crypto.createVirtualAddress
import no.nordicsemi.kotlin.mesh.crypto.Crypto.k1
import no.nordicsemi.kotlin.mesh.crypto.Crypto.k2
import no.nordicsemi.kotlin.mesh.crypto.Crypto.k3
import no.nordicsemi.kotlin.mesh.crypto.Crypto.k4
import no.nordicsemi.kotlin.mesh.crypto.Crypto.k5
import org.junit.Assert
import org.junit.Test
import java.util.Locale
import kotlin.uuid.Uuid

/**
 * Unit tests for crypto module. All test data used can be found at section 8 "Sample data" in the
 * Bluetooth Mesh Profile specification.
 */
@OptIn(ExperimentalStdlibApi::class)
@Suppress("UNUSED_VARIABLE")
class CryptoTest {

    /**
     * Unit test for salt generation function
     *
     * Refer 8.1.1 "s1 SALT generation function" for test data
     */
    @Test
    fun testSalt1() {
        val expected = "b73cefbd641ef2ea598c2b6efb62f79c".uppercase(Locale.US)
        val data = "test".toByteArray(Charsets.UTF_8)
        Assert.assertEquals(expected, calculateS1(data).toHexString())
    }

    /**
     * Unit test for salt generation function
     *
     * Refer 8.17.2 "BTM_ECDH_P256_HMAC_SHA256_AES_CCM algorithm" for test data
     */
    @Test
    fun testSalt2() {
        val expected = ("a71141ba8cb6b40f4f52b622e1c091614c73fc308f871b78ca775e769bc3ae69")
            .uppercase(Locale.US)
        val M = ("00010003000100000000000001000100002c31a47b5779809ef44cb5eaaf5c3e43d5f8fa" +
                "ad4a8794cb987e9b03745c78dd919512183898dfbecd52e2408e43871fd021109117bd3e" +
                "d4eaf8437743715d4ff465e43ff23d3f1b9dc7dfc04da8758184dbc966204796eccf0d6c" +
                "f5e16500cc0201d048bcbbd899eeefc424164e33c201c2b010ca6b4d43a8a155cad8ecb2" +
                "79").uppercase(Locale.US)
        Assert.assertEquals(expected, calculateS2(M.toByteArray()).toHexString())
    }

    /**
     * Unit test for k1 function
     *
     * Refer 8.1.2 "k1 function" for test data
     */
    @Test
    fun testK1() {
        val N = "3216d1509884b533248541792b877f98".uppercase(Locale.US)
        val SALT = "2ba14ffa0df84a2831938d57d276cab4".uppercase(Locale.US)
        val P = "5a09d60797eeb4478aada59db3352a0d".uppercase(Locale.US)
        val T = "c764bea25cf9738b08956ea3c712d5af".uppercase(Locale.US)
        val k1 = "f6ed15a8934afbe7d83e8dcb57fcf5d7".uppercase(Locale.US)
        Assert.assertEquals(
            k1,
            k1(N = N.toByteArray(), SALT = SALT.toByteArray(), P = P.toByteArray()).toHexString()
        )
    }

    /**
     * Unit test for k2 function
     *
     * Refer 8.1.3 "k2 function" for test data
     */
    @Test
    fun testK2() {
        val N = "f7a2a44f8e8a8029064f173ddc1e2b00".uppercase(Locale.US)
        val P = "00".uppercase(Locale.US)
        val expectedNid = "7f".uppercase(Locale.US).toByte(radix = 16)
        val expectedEncryptionKey = "9f589181a0f50de73c8070c7a6d27f46".uppercase(Locale.US)
        val expectedPrivacyKey = "4c715bd4a64b938f99b453351653124f".uppercase(Locale.US)
        val (nid, encryptionKey, privacyKey) = k2(N = N.toByteArray(), P = P.toByteArray())
        Assert.assertEquals(expectedNid, nid)
        Assert.assertEquals(expectedEncryptionKey, encryptionKey.toHexString())
        Assert.assertEquals(expectedPrivacyKey, privacyKey.toHexString())
    }

    /**
     * Unit test for k3 function
     *
     * Refer 8.1.5 "k3 function" for test data
     */
    @Test
    fun testK3() {
        val N = "f7a2a44f8e8a8029064f173ddc1e2b00".uppercase(Locale.US)
        val expectedNetworkId = "ff046958233db014".uppercase(Locale.US)
        Assert.assertEquals(expectedNetworkId, k3(N = N.toByteArray()).toHexString())
    }

    /**
     * Unit test for k4 function.
     *
     * Refer 8.1.6 "k4 function" for N
     */
    @Test
    fun testK4() {
        val N = "3216d1509884b533248541792b877f98".uppercase(Locale.US)
        val expectedNetworkId = "38".uppercase(Locale.US).toByte(radix = 16)
        Assert.assertEquals(expectedNetworkId, k4(N = N.toByteArray()))
    }

    /**
     * Unit test for k5 function.
     *
     * Refer 8.17.2 "BTM_ECDH_P256_HMAC_SHA256_AES_CCM" for test data
     */
    @Test
    fun testK5() {
        val N = ("ab85843a2f6d883f62e5684b38e307335fe6e1945ecd19604105c6f23221eb69906d73a3c7a7cb3" +
                "ff730dca68a46b9c18d673f50e078202311473ebbe253669f")
            .uppercase(Locale.US)
        val SALT = "a71141ba8cb6b40f4f52b622e1c091614c73fc308f871b78ca775e769bc3ae69"
            .uppercase(Locale.US)
        val P = "7072636b323536".uppercase(Locale.US)
        val T = "bb73fb226a7a26c196f3f649bf8d208eca77ae956fc31a5ab51a47267ad41815"
            .uppercase(Locale.US)
        val k5 = "210c3c448152e8d59ef742aa7d22ee5ba59a38648bda6bf05c74f3e46fc2c0bb"
            .uppercase(Locale.US)
        Assert.assertEquals(
            k5,
            k5(N = N.toByteArray(), SALT = SALT.toByteArray(), P = P.toByteArray()).toHexString()
        )
    }

    /**
     * Unit test for NetworkKeyDerivatives.
     *
     * Refer 8.2.2 "Encryption and privacy keys (Master)" for test data for NID, EncryptionKey and
     * PrivacyKey. Refer 5.2.4 for Network ID, 8.2.5 for IdentityKey and 8.2.6 BeaconKey.
     */
    @Test
    fun testNetworkKeyDerivatives() {
        val N = "7dd7364cd842ad18c17c2b820c84c3d6".toByteArray()
        val NID = "68".toInt(16).toByte()
        val encryptionKey = "0953fa93e7caac9638f58820220a398e".uppercase(Locale.US).toByteArray()
        val privacyKey = "8b84eedec100067d670971dd2aa700cf".uppercase(Locale.US).toByteArray()
        val networkId = "3ecaff672f673370".uppercase(Locale.US).toByteArray()
        val identityKey = "84396c435ac48560b5965385253e210c".uppercase(Locale.US).toByteArray()
        val beaconKey = "5423d967da639a99cb02231a83f7d254".uppercase(Locale.US).toByteArray()
        val keyDerivatives = Crypto.calculateKeyDerivatives(N, SecurityCredentials.ManagedFlooding)
        Assert.assertTrue("NID do not match!", NID == keyDerivatives.nid)
        Assert.assertTrue(
            "EncryptionKeys do not match!",
            encryptionKey.contentEquals(keyDerivatives.encryptionKey)
        )
        Assert.assertTrue(
            "PrivacyKeys do not match!",
            privacyKey.contentEquals(keyDerivatives.privacyKey)
        )
        Assert.assertTrue(
            "Network IDs do not match!",
            networkId.contentEquals(keyDerivatives.networkId)
        )
        Assert.assertTrue(
            "Identity Keys do not match!",
            identityKey.contentEquals(keyDerivatives.identityKey)
        )
        Assert.assertTrue(
            "Beacon Keys do not match!",
            beaconKey.contentEquals(keyDerivatives.beaconKey)
        )

        val directedNID = "0D".toInt(16).toByte()
        val directedEncryptionKey = "b47a02c6cc9b4ac4cb9b88e765c9ade4".uppercase(Locale.US).toByteArray()
        val directedPrivacyKey = "9bf7ab5a5ad415fbd77e07bb808f4865".uppercase(Locale.US).toByteArray()
        val directedKeyDerivatives = Crypto.calculateKeyDerivatives(N, SecurityCredentials.DirectedFlooding)
        Assert.assertTrue("NID do not match!", directedNID == directedKeyDerivatives.nid)
        Assert.assertTrue("EncryptionKeys do not match!", directedEncryptionKey.contentEquals(directedKeyDerivatives.encryptionKey))
        Assert.assertTrue("PrivacyKeys do not match!", directedPrivacyKey.contentEquals(directedKeyDerivatives.privacyKey))
    }

    /**
     * Unit test for [Crypto.encrypt].
     *
     * Refer 8.3.1 "Message #1" for test data for NID, EncryptionKey and PrivacyKey.
     * Refer 5.2.4 for Network ID, 8.2.5 for IdentityKey and 8.2.6 BeaconKey.
     */
    @Test
    fun testEncrypt() {
        val expected = "b5e5bfdacbaf6cb7fb6bff871f035444ce83a670df".uppercase(Locale.US)
        val dst = "fffd".toByteArray()
        val transportPdu = "034b50057e400000010000".toByteArray()
        val data = dst + transportPdu
        val encryptionKey = "0953fa93e7caac9638f58820220a398e".toByteArray()
        val nonce = "00800000011201000012345678".toByteArray()
        val mic = 64 / 8
        val actual = Crypto.encrypt(
            data = data,
            key = encryptionKey,
            nonce = nonce,
            micSize = mic
        ).toHexString()
        Assert.assertEquals(expected, actual)
    }

    /**
     * Unit test for [Crypto.decrypt].
     *
     * Refer 8.3.1 "Message #1" for test data for NID, EncryptionKey and PrivacyKey.
     * Refer 5.2.4 for Network ID, 8.2.5 for IdentityKey and 8.2.6 BeaconKey.
     */
    @Test
    fun testDecrypt() {
        val dst = "fffd"
        val transportPdu = "034b50057e400000010000"
        val expected = (dst + transportPdu).uppercase(Locale.US)
        val encryptedTransportPdu = "b5e5bfdacbaf6cb7fb6bff871f035444ce83a670df".toByteArray()
        val encryptionKey = "0953fa93e7caac9638f58820220a398e".toByteArray()
        val nonce = "00800000011201000012345678".toByteArray()
        val mic = 64 / 8
        val actual = Crypto.decrypt(
            data = encryptedTransportPdu,
            key = encryptionKey,
            nonce = nonce,
            micSize = mic
        )?.toHexString()
        Assert.assertNotNull(actual)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testVirtualAddress() {
        val uuid = Uuid.parse("0073e7e4-d8b9-440f-af84-15df4c56c0e1")
        val expected = "B529".toUShort(radix = 16)
        val actual = createVirtualAddress(uuid = uuid)
        Assert.assertEquals(expected, actual)
    }

    /**
     * Unit test for [Crypto.authenticate].
     *
     * Refer 8.4.3 "Secure Network beacon" for test data for Beacon Key and Beacon PDU.
     */
    @Test
    fun testAuthenticate() {
        val beaconPDU = "01003ecaff672f673370123456788ea261582f364f6f".toByteArray()
        val beaconKey = "5423d967da639a99cb02231a83f7d254".toByteArray()
        val expected = true
        val actual = Crypto.authenticate(beaconPDU, beaconKey)
        Assert.assertEquals(expected, actual)
    }

    /**
     * Unit test for [Crypto.decodeAndAuthenticate].
     *
     * Refer 8.4.6.1 "Mesh Private Beacon - IV update in Progress" for test data for Beacon Key and Beacon PDU.
     */
    @Test
    fun testDecodeAndAuthenticate() {
        val beaconPDU = "02435f18f85cf78a3121f58478a561e488e7cbf3174f022a514741".toByteArray()
        val beaconKey = "6be76842460b2d3a5850d4698409f1bb".toByteArray()
        val expected = Pair(0x02.toByte(), "1010abcd".toByteArray())
        val actual = Crypto.decodeAndAuthenticate(beaconPDU, beaconKey)
        val isEqual = expected.first == actual?.first && expected.second.contentEquals(actual.second)
        Assert.assertEquals(true, isEqual)
    }

    /**
     * Unit test for [Crypto.obfuscate].
     *
     * Refer 8.3.16 "Message #16" for test data for Data to Obfuscate/Deobfuscate, Privacy Key
     * Privacy Random, IV Index and the final expected result.
     */
    @Test
    fun testObfuscate() {
        val data = "e80e5da5af0e".toByteArray()
        val random = "6b9be7f5a642f2f98680e61c3a8b47f228".toByteArray()
        val ivIndex = 0x12345678.toUInt()
        val privacyKey = "8b84eedec100067d670971dd2aa700cf".toByteArray()
        val expected = "0b0000061201".toByteArray()
        val actual = Crypto.obfuscate(data, random, ivIndex, privacyKey)
        Assert.assertTrue(expected.contentEquals(actual))
    }


    /**
     * Unit test for [Crypto.calculateECB].
     *
     * Refer 8.3.17 "Message #17" for test data for Privacy Plain Text, Privacy Key and PECB.
     */
    @Test
    fun testCalculateECB() {
        val privacyPlainText = "0000000000123456782a80d381b91f82".toByteArray()
        val privacyKey = "8b84eedec100067d670971dd2aa700cf".toByteArray()
        val expectedPECB = "b8bd2c18096e".toByteArray()
        val actualPECB = Crypto.calculateECB(privacyPlainText, privacyKey).copyOfRange(0, 6)
        Assert.assertTrue(expectedPECB.contentEquals(actualPECB))
    }

    /**
     * Unit test for [uint16ToUtf8].
     *
     * Refer to 4.7 in Mesh Binary Large Object Transfer Model d1.0r04_PRr00 for test data.
     */
    @Test
    fun testUint16ToUTF8() {
        val expected = "0010C280C480".toByteArray()
        val actual = "0000001000800100".toByteArray().uint16ToUtf8()
        Assert.assertTrue(expected.contentEquals(actual))
    }

    /**
     * Unit test for [utf8ToUint16].
     *
     * Refer to 4.7 in Mesh Binary Large Object Transfer Model d1.0r04_PRr00 for test data.
     */
    @Test
    fun testUTF8ToUint16() {
        val expected = "0000001000800100".toByteArray()
        val actual = "0010C280C480".toByteArray().utf8ToUint16()
        Assert.assertTrue(expected.contentEquals(actual))
    }
}