@file:Suppress("LocalVariableName", "unused", "MemberVisibilityCanBePrivate")

package no.nordicsemi.kotlin.mesh.crypto

import no.nordicsemi.kotlin.data.and
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.data.xor
import org.bouncycastle.crypto.BlockCipher
import org.bouncycastle.crypto.InvalidCipherTextException
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.macs.CMac
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.modes.CCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.util.BigIntegers
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security
import java.security.spec.InvalidKeySpecException
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.SecretKeySpec
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object Crypto {
    private val secureRandom = SecureRandom()
    private val blockCipher: BlockCipher = AESEngine.newInstance()
    private val SALT_KEY = ByteArray(16)
    private val smk2 = "smk2".encodeToByteArray()
    private val smk3 = "smk3".encodeToByteArray()
    private val smk4 = "smk4".encodeToByteArray()
    private val id64 = "id64".encodeToByteArray()
    private val id6 = "id6".encodeToByteArray()
    private val NKIK = "nkik".encodeToByteArray()
    private val NKBK = "nkbk".encodeToByteArray()
    private val NKPK = "nkpk".encodeToByteArray()
    private val ID128 = "id128".encodeToByteArray()
    private val VTAD = "vtad".encodeToByteArray()

    private val PRCK = "prck".encodeToByteArray()
    private val PRCK256 = "prck256".encodeToByteArray()
    private val PRSK = "prsk".encodeToByteArray()
    private val PRSN = "prsn".encodeToByteArray()
    private val PRDK = "prdk".encodeToByteArray()

    init {
        // First let's check if the BouncyCastle provider is already available
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) != null) {
            // Remove any existing BouncyCastle provider
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        }
        // Install the provider
        Security.insertProviderAt(BouncyCastleProvider(), 0)
    }

    /**
     * Generates a 128-bit random key using a SecureRandom.
     */
    fun generateRandomKey() = secureRandom.run {
        val random = ByteArray(16)
        nextBytes(random)
        random
    }

    /**
     * Generates a random number of the given size in bits.
     *
     * @param sizeInBits Size of the random number in bits.
     * @return ByteArray
     */
    fun generateRandom(sizeInBits: Int) = secureRandom.run {
        val random = ByteArray(sizeInBits shr 3)
        nextBytes(random)
        random
    }

    /**
     * Generates a pair of Private and Public Keys using P256 Elliptic Curve.
     * @param algorithm Algorithm to use.
     * @return KeyPair
     */
    fun generateKeyPair(algorithm: Algorithm): KeyPair = KeyPairGeneratorSpi
        .ECDH()
        .run {
            initialize(algorithm.length)
            generateKeyPair()
        }


    /**
     * Calculates the shared secret based on the given public key and the local private key.
     *
     * @param privateKey  Private key.
     * @param publicKey   Public key.z
     * @return Shared secret.
     * @throws NoSuchAlgorithmException  if the algorithm is not supported.
     * @throws NoSuchProviderException   if the provider is not supported.
     * @throws InvalidKeySpecException   if the key spec is invalid.
     * @throws InvalidKeyException       if the key is invalid.
     * @throws IllegalStateException     if the key agreement is not initialized or if
     *                                   [KeyAgreement.doPhase] has not been called to supply the
     *                                   keys for all parties in the agreement.
     */
    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class,
        InvalidKeySpecException::class,
        InvalidKeyException::class,
        IllegalStateException::class
    )
    fun calculateSharedSecret(privateKey: PrivateKey, publicKey: ByteArray): ByteArray {
        val devicePublicKey = calculateDeviceEcPublicKey(publicKey = publicKey) as ECPublicKey
        return KeyAgreement.getInstance(privateKey.algorithm, BouncyCastleProvider.PROVIDER_NAME)
            .let {
                it.init(privateKey)
                it.doPhase(devicePublicKey, true)
                it.generateSecret()
            }
    }

    /**
     * Calculates the device public key based on the provisioners public key.
     *
     * @param publicKey Provisioner public key.
     * @return Device public key.
     */
    private fun calculateDeviceEcPublicKey(publicKey: ByteArray): PublicKey {
        val x = BigIntegers.fromUnsignedByteArray(publicKey, 0, 32)
        val y = BigIntegers.fromUnsignedByteArray(publicKey, 32, 32)

        val ecParameters = ECNamedCurveTable.getParameterSpec("secp256r1")
        val ecPoint = ecParameters.curve.validatePoint(x, y)
        val keySpec = ECPublicKeySpec(ecPoint, ecParameters)
        val keyFactory = KeyFactory.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
        return keyFactory.generatePublic(keySpec)
    }

    /**
     * Calculates the device public key based on the provisioners public key.
     *
     * TODO This API needs to be clarified currently used for tests
     *
     * @param publicKey Provisioner public key.
     */
    fun calculateDevicePublicKey(publicKey: ByteArray): ByteArray =
        calculateDeviceEcPublicKey(publicKey = publicKey).toByteArray()

    /**
     * Calculates the provisioning confirmation based on the confirmation inputs, device random,
     * shared secret and the auth value.
     *
     * @param confirmationInputs Confirmation inputs.
     * @param sharedSecret       Shared secret.
     * @param deviceRandom       128-bit or 256-bit device random.
     * @param authValue          128-bit or 256-bit auth value.
     * @param algorithm          Algorithm to use.
     * @return Provisioning confirmation value.
     */
    fun calculateConfirmation(
        confirmationInputs: ByteArray,
        sharedSecret: ByteArray,
        deviceRandom: ByteArray,
        authValue: ByteArray,
        algorithm: Algorithm,
    ): ByteArray {
        return when (algorithm) {
            Algorithm.FIPS_P256_ELLIPTIC_CURVE,
            Algorithm.BTM_ECDH_P256_CMAC_AES128_AES_CCM,
                -> {
                val confirmationSalt = calculateS1(input = confirmationInputs)
                val confirmationKey = k1(N = sharedSecret, SALT = confirmationSalt, P = PRCK)
                calculateCmac(input = deviceRandom + authValue, key = confirmationKey)
            }

            Algorithm.BTM_ECDH_P256_HMAC_SHA256_AES_CCM -> {
                val confirmationSalt = calculateS2(confirmationInputs)
                val confirmationKey = k5(sharedSecret + authValue, confirmationSalt, PRCK256)
                calculateHmac256(deviceRandom, confirmationKey)
            }
        }
    }

    /**
     * Creates a 16-bit virtual address for a given UUID.
     * @param uuid 128-bit Label UUID.
     */
    @OptIn(ExperimentalStdlibApi::class, ExperimentalUuidApi::class)
    fun createVirtualAddress(uuid: Uuid): UShort {
        val salt = calculateS1(input = VTAD)
        val hash = calculateCmac(input = uuid.toByteArray(), key = salt)
        // The virtual address is a 16-bit value that has bit 15 set to 1, bit 14 set to 0,
        // and bits 13 to 0 set to the value of a hash.
        // See: Mesh Profile Specification 1.1: 3.4.2.3 Virtual address.
        return (0x8000u or ((hash.getInt(offset = 12) and 0x3FFF).toUInt())).toUShort()
    }

    /**
     * Calculates the NID, EncryptionKey, PrivacyKey, NetworkID, IdentityKey, BeaconKey,
     * PrivateBeaconKey for a given NetworkKey using the given security credentials.
     *
     * @param N           128-bit NetworkKey.
     * @param credentials Security credentials.
     * @return Key Derivatives.
     */
    fun calculateKeyDerivatives(N: ByteArray, credentials: SecurityCredentials): KeyDerivatives {
        return calculateKeyDerivatives(N = N, P = credentials.P)
    }

    /**
     * /**
     * Calculates the Friendship Credentials NID, EncryptionKey, PrivacyKey, NetworkID, IdentityKey,
     * BeaconKey, PrivateBeaconKey for a given NetworkKey
     *
     * @param N 128-bit NetworkKey.
     * @param P additional data to be used when calculating the Key Derivatives for Friendship
     *          Credentials
     * @return Friendship Credentials Key Derivatives.
    */
     */
    fun calculateKeyDerivatives(N: ByteArray, P: ByteArray): KeyDerivatives {
        val k2 = k2(N = N, P = P)
        return KeyDerivatives(
            nid = k2.nid,
            encryptionKey = k2.encryptionKey,
            privacyKey = k2.privacyKey,
            networkId = calculateNetworkId(N = N),
            identityKey = calculateIdentityKey(N = N),
            beaconKey = calculateBeaconKey(N = N),
            privateBeaconKey = calculatePrivateBeaconKey(N = N)
        )
    }

    /**
     * Calculates the AID for a given ApplicationKey.
     *
     * @param N 128-bit ApplicationKey.
     * @return 8-bit Application Key Identifier.
     */
    fun calculateAid(N: ByteArray): Byte {
        return k4(N = N)
    }

    /**
     * Encrypts the [data] with the EncryptionKey , Nonce and concatenates the MIC(Message Integrity
     * Check).
     *
     * @param data                  Data to be encrypted.
     * @param key                   128-bit key.
     * @param nonce                 104-bit nonce.
     * @param micSize               Length of the MIC to be generated, in bytes.
     * @param additionalData        Additional data to be authenticated.
     * @return Encrypted data concatenated with MIC of given size.
     */
    @kotlin.jvm.Throws(InvalidCipherTextException::class)
    fun encrypt(
        data: ByteArray,
        key: ByteArray,
        nonce: ByteArray,
        additionalData: ByteArray? = null,
        micSize: Int,
    ) = calculateCCM(
        data = data,
        key = key,
        nonce = nonce,
        additionalData = additionalData,
        micSize = micSize,
        mode = true
    )

    /**
     * Decrypts the given data with the EncryptionKey, Nonce and authenticates the generated
     * MIC(Message Integrity Check).
     *
     * @param data                  Data to be decrypted.
     * @param key                   128-bit key.
     * @param nonce                 104-bit nonce.
     * @param micSize               Length of the MIC to be generated, in bytes.
     * @param additionalData        Additional data to be authenticated.
     * @throws Error if the decryption failed.
     * @return Encrypted data concatenated with MIC of given size or null if the decryption failed.
     */
    fun decrypt(
        data: ByteArray,
        key: ByteArray,
        nonce: ByteArray,
        additionalData: ByteArray? = null,
        micSize: Int,
    ) = try {
        calculateCCM(
            data = data,
            key = key,
            nonce = nonce,
            additionalData = additionalData,
            micSize = micSize,
            mode = false
        )
    } catch (e: InvalidCipherTextException) {
        null
    } catch (e: Exception) {
        throw Error("CCM decryption failed: ${e.message}")
    }

    /**
     *  Obfuscates or De+obfuscates given data by XORing it with PECB, which is
     *  calculated by encrypting Privacy Plaintext (encrypted data (used as Privacy
     *  Random) and IV Index) using the given key.
     *  - parameters:
     *  @param data         The data to obfuscate or deobfuscate.
     *  @param random       Data used as Privacy Random.
     *  @param ivIndex      The current IV Index value.
     *  @param privacyKey   The 128-bit Privacy Key.
     *  @return a byte array containing Obfuscated or De-obfuscated input data.
     */
    fun obfuscate(
        data: ByteArray,
        random: ByteArray,
        ivIndex: UInt,
        privacyKey: ByteArray,
    ): ByteArray {
        // Privacy Random = (EncDST || EncTransportPDU || NetMIC)[0–6]
        // Privacy Plaintext = 0x0000000000 || IV Index || Privacy Random
        // PECB = e (PrivacyKey, Privacy Plaintext)
        // ObfuscatedData = (CTL || TTL || SEQ || SRC) ⊕ PECB[0–5]
        val privacyRandom = random.copyOfRange(fromIndex = 0, toIndex = 7)
        val privacyPlaintext = ByteArray(5) +
                ivIndex.toByteArray() + privacyRandom
        val pecb = calculateECB(data = privacyPlaintext, key = privacyKey)
        return data xor pecb.copyOfRange(fromIndex = 0, toIndex = 6)
    }

    /**
     * Authenticates the received Secure Network beacon using the given Beacon Key.
     *
     * @param pdu           Received Secure Network beacon.
     * @param beaconKey     Beacon key generated from a network key.
     *
     * @return true if the beacon is valid, false otherwise.
     */
    fun authenticate(pdu: ByteArray, beaconKey: ByteArray): Boolean = try {
        // byte 0 is the beacon type 0x01
        val flagsNetIdAndIvIndex = pdu.sliceArray(indices = 1 until 14)
        val authenticationValue = pdu.sliceArray(indices = 14 until 22)
        val hash = calculateCmac(input = flagsNetIdAndIvIndex, key = beaconKey)
            .sliceArray(indices = 0 until 8)
        hash.contentEquals(other = authenticationValue)
    } catch (e: Exception) {
        false
    }

    /**
     * Decodes and authenticates the received Private Beacon using the given Private Beacon Key.
     *
     * @param pdu                   The received Private Beacon beacon.
     * @param privateBeaconKey      The Private Beacon Key generated from a Network Key.
     *
     * @return a Pair containing the network information from the beacon. First value is the Flags
     *         Byte and the second is the IV Index.
     * TODO decide whether to keep this as a return type or use a data class PrivateBeaconData
     *  containing the flags and ivIndex. Note that this is specific to the Private Beacon as in
     *  a Secure Network Beacon the Flags and IV Index are separated by the Network ID.
     */
    fun decodeAndAuthenticate(pdu: ByteArray, privateBeaconKey: ByteArray): Pair<Byte, ByteArray>? {
        // Byte 0 of the PDU is the Beacon Type (0x02)
        val random = pdu.sliceArray(indices = 1 until 14)
        val obfuscatedData = pdu.sliceArray(indices = 14 until 19)
        val authenticationTag = pdu.sliceArray(indices = 19 until 27)

        // Deobfuscate Private Beacon Data
        val C1 = byteArrayOf(0x01) + random + byteArrayOf(0x00, 0x01)
        val S = calculateECB(data = C1, key = privateBeaconKey)
        val privateBeaconData = S.sliceArray(indices = 0 until 5) xor obfuscatedData

        // Authenticate the Beacon
        val B0 = byteArrayOf(0x19) + random + byteArrayOf(0x00, 0x05)
        val C0 = byteArrayOf(0x01) + random + ByteArray(2) { 0x00 }
        val P = privateBeaconData + ByteArray(11) { 0x00 }
        val T0 = calculateECB(data = B0, key = privateBeaconKey)
        val T1 = calculateECB(data = T0 xor P, key = privateBeaconKey)
        val T2 = T1 xor calculateECB(data = C0, key = privateBeaconKey)
        val calculatedAuthenticationTag = T2.sliceArray(indices = 0 until 8)

        if (!authenticationTag.contentEquals(other = calculatedAuthenticationTag)) return null

        return Pair(
            first = privateBeaconData[0],
            second = privateBeaconData
                .copyOfRange(fromIndex = 1, toIndex = 5)
        )
    }

    /**
     * Calculates Electronic Code Book (ECB) for the given [data] and [key].
     *
     * @param data the input data.
     * @param key the 128-bit key.
     * @return the encrypted data.
     */
    fun calculateECB(data: ByteArray, key: ByteArray): ByteArray {
        try {
            val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
            return cipher.doFinal(data)
        } catch (e: Exception) {
            throw InvalidCipherTextException("Error while encrypting data: ${e.message}")
        }
    }

    /**
     * Calculates the 64-bit Network ID.
     * The Network ID is derived from the network key such that each network key generates one
     * Network ID.
     *
     * This identifier becomes public information.
     *
     * @param N     128-bit Network key.
     * @return 64-bit Network ID.
     */
    fun calculateNetworkId(N: ByteArray): ByteArray = k3(N = N)

    /**
     * Generates Node Identity Hash using the given Identity Key
     *
     * @param data         48-bits of padding of 0s, 65-bit random value and the unicast Address of
     *                     the node.
     * @param identityKey  Identity key.
     * @return Function of the included random number and identity information.
     */
    fun calculateHash(data: ByteArray, identityKey: ByteArray) =
        calculateECB(data, identityKey).drop(n = 8).toByteArray()

    /**
     * Calculates the 128-bit IdentityKey.
     * The IdentityKey is derived from the network key such that each network key generates one
     * IdentityKey
     *
     * @param N     128-bit Network key.
     * @return 128-bit key T.
     */
    private fun calculateIdentityKey(N: ByteArray): ByteArray {
        val s1 = calculateS1(input = NKIK)
        val P = ID128 + 0x01
        return k1(N = N, SALT = s1, P = P)
    }

    /**
     * Calculates the 128-bit BeaconKey.
     *
     * The BeaconKey is derived from the network key such that each network key generates one
     * BeaconKey.
     *
     * @param N     128-bit Network key.
     * @return 128-bit key T.
     */
    private fun calculateBeaconKey(N: ByteArray): ByteArray {
        val s1 = calculateS1(input = NKBK)
        val P = ID128 + 0x01
        return k1(N = N, SALT = s1, P = P)
    }

    /**
     * Calculates the 128-bit PrivateBeaconKey
     * The PrivateBeaconKey is derived from the network key such that each network key generates
     * one PrivateBeaconKey.
     *
     * @param N     128-bit Network key.
     * @return 128-bit key T.
     */
    private fun calculatePrivateBeaconKey(N: ByteArray): ByteArray {
        val s1 = calculateS1(input = NKPK)
        val P = ID128 + 0x01
        return k1(N = N, SALT = s1, P = P)
    }

    /**
     * Calculates the salt based on a given input.
     *
     * @param input A non-zero length octet array or ASCII encoded string.
     * @return 128-bit salt of the given input.
     */
    fun calculateS1(input: ByteArray): ByteArray {
        val key = ByteArray(16) { 0x00 }
        return calculateCmac(input = input, key = key)
    }

    /**
     * Calculates the salt based on a given input.
     *
     * @param input A non-zero length octet array or ASCII encoded string.
     * @return 128-bit salt of the given input.
     */
    fun calculateS2(input: ByteArray): ByteArray {
        val key = ByteArray(32) { 0x00 }
        return calculateHmac256(input = input, key = key)
    }

    /**
     * The network key material derivation function k1 is used to generate instances of IdentityKey
     * and BeaconKey. The definition of this key generation function makes use of the MAC function
     * AES-CMAC(T) with a 128-bit key T.
     *
     * @param N         0 or more octets.
     * @param SALT      128 bits salt.
     * @param P         0 or more octets.
     * @return 128-bit key T.
     */
    internal fun k1(N: ByteArray, SALT: ByteArray, P: ByteArray): ByteArray {
        require(SALT.size == 16) { "Salt must be 128-bits" }
        val t = calculateCmac(N, SALT)
        return calculateCmac(P, t)
    }

    /**
     * The network key material derivation function k2 is used to generate instances of
     * EncryptionKey, PrivacyKey, and NID for use as Master and Private Low Power node
     * communication.
     *
     * The definition of this key generation function makes use of the MAC function AES-CMAC(T) with
     * a 128-bit key T.
     *
     * @param N     128-bit key.
     * @param P     1 or more octets.
     * @return a Triple containing the NID, EncryptionKey and PrivacyKey.
     */
    internal fun k2(N: ByteArray, P: ByteArray): SecurityMaterial {
        require(N.size == 16) {
            "N must be 128-bits"
        }
        require(P.isNotEmpty()) {
            "P must be 1 or more octets"
        }
        val s1 = calculateS1(input = smk2)
        val T = calculateCmac(input = N, key = s1)
        val T0 = byteArrayOf()
        val T1 = calculateCmac(input = (T0 + P + byteArrayOf(0x01)), key = T)
        val T2 = calculateCmac(input = (T1 + P + byteArrayOf(0x02)), key = T) // EncryptionKey
        val T3 = calculateCmac(input = (T2 + P + byteArrayOf(0x03)), key = T) // PrivacyKey

        val nid = T1.last() and 0x7F
        return SecurityMaterial(nid, T2, T3)
    }

    /**
     * The derivation function k3 is used to generate a public value of 64 bits derived from a
     * private key. The definition of this derivation function makes use of the MAC function
     * AES-CMAC(T) with a 128-bit key T.
     *
     * @param N 128-bit key.
     * @return 64-bit key T.
     */
    internal fun k3(N: ByteArray): ByteArray {
        val s1 = calculateS1(input = smk3)
        val T = calculateCmac(input = N, key = s1)
        val result = calculateCmac(input = (id64 + 0x01), key = T)
        return result.copyOfRange(8, result.count())
    }

    /**
     * The derivation function k4 is used to generate a public value of 6 bits derived from a
     * private key. The definition of this derivation function makes use of the MAC function
     * AES-CMAC(T) with a 128-bit key T.
     *
     * @param N 128-bit key.
     * @return 128-bit key T.
     */
    internal fun k4(N: ByteArray): Byte {
        val s1 = calculateS1(input = smk4)
        val T = calculateCmac(input = N, key = s1)
        val result = calculateCmac(input = (id6 + 0x01), key = T)
        return result.last() and 0x3F
    }

    /**
     * The provisioning material derivation function k5 is used to generate the 256-bit key used in
     * provisioning. The definition of this derivation function makes use of the MAC function
     * HMAC-SHA-256 with a 256-bit key T.
     *
     * @param N 128-bit key.
     * @param SALT 128-bit salt.
     * @param P 1 or more octets.
     * @return 256-bit key T.
     */
    internal fun k5(N: ByteArray, SALT: ByteArray, P: ByteArray): ByteArray {
        val T = calculateHmac256(input = N, key = SALT)
        return calculateHmac256(input = P, key = T)
    }

    /**
     * Calculates Cipher-based Message Authentication Code (CMAC) that uses AES-128 as the block
     * cipher function (AES-CMAC).
     *
     * @param input Input to be authenticated.
     * @param key   128-bit key.
     * @return 128-bit message authentication code (MAC).
     */
    private fun calculateCmac(input: ByteArray, key: ByteArray): ByteArray {
        require(key.size == 16) { "Key must be 128-bits" }
        return CMac(blockCipher).run {
            init(KeyParameter(key))
            update(input, 0, input.count())
            val output = ByteArray(macSize) { 0x00 }
            doFinal(output, 0)
            output
        }
    }

    /**
     * Calculates message authentication using cryptographic hash functions.
     *
     * FIPS 180-4 defines the SHA-256 secure hash algorithm. The SHA-256 algorithm is used as a hash
     * function for the HMAC mechanism for the HMAC-SHA-256 function.
     * Bluetooth Mesh Protocol 1.1 defines HMAC-SHA-256 as a function that takes two inputs and
     * results in one output.
     *
     * @param input Input to be authenticated.
     * @param key   256-bit key.
     * @return 256-bit hash-based message authentication code (HMAC).
     */
    private fun calculateHmac256(input: ByteArray, key: ByteArray): ByteArray {
        require(key.size == 32) { "Key must be 256-bits" }
        return HMac(SHA256Digest()).run {
            init(KeyParameter(key))
            update(input, 0, input.count())
            val output = ByteArray(macSize)
            doFinal(output, 0)
            output
        }
    }

    /**
     * This method  generates the ciphertext and MIC (Message Integrity Check) and validates the
     * ciphertext RFC3610 [10] defines the AES Counter with CBC-MAC (CCM).
     *
     * @param data                  Data to be encrypted and authenticated.
     * @param key                   128-bit key.
     * @param nonce                 104-bit nonce.
     * @param additionalData        Additional data to be authenticated.
     * @param micSize               Length of the MIC to be generated, in bytes.
     * @param mode                  True to encrypt or false to decrypt
     * @throws InvalidCipherTextException if the cipher text is invalid.
     * @throws IllegalStateException if the cipher is not initialized.
     * @return if [mode] was set to true, returns the encrypted data with the MIC concatenated
     *          otherwise returns the decrypted data.
     */
    @Throws(InvalidCipherTextException::class, IllegalStateException::class)
    private fun calculateCCM(
        data: ByteArray,
        key: ByteArray,
        nonce: ByteArray,
        additionalData: ByteArray? = null,
        micSize: Int,
        mode: Boolean,
    ) = CCMBlockCipher.newInstance(blockCipher).run {
        val ccm = ByteArray(if (mode) data.size + micSize else data.size - micSize)
        init(mode, AEADParameters(KeyParameter(key), micSize * 8, nonce, additionalData))
        processBytes(data, 0, data.size, ccm, data.size)
        doFinal(ccm, 0)
        ccm
    }

    /**
     * Calculates the Session Key, Session Nonce and the Device Key based on the
     * Confirmation Inputs, 16 or 32-byte Provisioner Random and 16 or 32-byte device Random.
     *
     * @param confirmationInputs   Confirmation inputs is built over hte provisioning process.
     * @param sharedSecret         Shared secret obtained in the previous step.
     * @param provisionerRandom    Array of 16 or 32 bytes random bytes.
     * @param deviceRandom         Array of 16 or 32 bytes random bytes received from the device.
     * @param algorithm            Algorithm to be used.
     * @return Triple of Session Key, Session Nonce and Device Key.
     */
    fun calculateKeys(
        algorithm: Algorithm,
        confirmationInputs: ByteArray,
        sharedSecret: ByteArray,
        provisionerRandom: ByteArray,
        deviceRandom: ByteArray,
    ): Triple<ByteArray, ByteArray, ByteArray> {
        val confirmationSalt = when (algorithm) {
            Algorithm.FIPS_P256_ELLIPTIC_CURVE,
            Algorithm.BTM_ECDH_P256_CMAC_AES128_AES_CCM,
                -> calculateS1(input = confirmationInputs)

            Algorithm.BTM_ECDH_P256_HMAC_SHA256_AES_CCM -> calculateS2(input = confirmationInputs)
        }
        val provisioningSalt =
            calculateS1(input = confirmationSalt + provisionerRandom + deviceRandom)
        val sessionKey = k1(N = sharedSecret, SALT = provisioningSalt, P = PRSK)
        // Only the 13 least significant bits of the calculated session nonce are used.
        val sessionNonce = k1(N = sharedSecret, SALT = provisioningSalt, P = PRSN).let { nonce ->
            nonce.sliceArray(indices = 3 until nonce.size)
        }
        val deviceKey = k1(N = sharedSecret, SALT = provisioningSalt, P = PRDK)
        return Triple(first = sessionKey, second = sessionNonce, third = deviceKey)
    }
}