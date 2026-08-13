@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package no.nordicsemi.kotlin.mesh.provisioning

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.model.Insecure
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.KeyDistribution
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.Secure
import no.nordicsemi.kotlin.mesh.core.model.Security
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.crypto.Algorithm
import no.nordicsemi.kotlin.mesh.crypto.Crypto
import no.nordicsemi.kotlin.mesh.crypto.toByteArray
import java.security.PrivateKey
import java.security.PublicKey

/**
 * This class contains the provisioning data used during the provisioning process.
 *
 * @property networkKey                          Network key used during provisioning.
 * @property ivIndex                             IV index used during provisioning.
 * @property unicastAddress                      Unicast address used during provisioning.
 * @property privateKey                          Private key used during provisioning.
 * @property publicKey                           Public key used during provisioning.
 * @property sharedSecret                        Shared secret used during provisioning.
 * @property authValue                           Auth value used during provisioning.
 * @property deviceConfirmation                  Device confirmation used during provisioning.
 * @property deviceRandom                        Device random used during provisioning.
 * @property oobPublicKey                        OOB public key used during provisioning.
 * @property algorithm                           Algorithm used during provisioning.
 * @property deviceKey                           Device key used during provisioning.
 * @property provisionerRandom                   Provisioner random used during provisioning.
 * @property provisionerPublicKey                Provisioner public key used during provisioning.
 * @property confirmationInputs                  Confirmation inputs used during provisioning and is
 *                                               built over the provisioning process. It's composed
 *                                               of Provisioning Invite PDU, Provisioning
 *                                               Capabilities PDU,Provisioning Start PDU,
 *                                               Provisioner's Public Key,Provisionee's Public Key.
 * @property provisionerConfirmation             Provisioner confirmation used during provisioning.
 * @property encryptedProvisioningDataWithMic    Encrypted provisioning data with mic used during
 * @property security                            Security used during provisioning.
 *                                               provisioning.
 * @constructor Creates a [ProvisioningData] object.
 */
internal class ProvisioningData {
    private lateinit var networkKey: NetworkKey
    private lateinit var ivIndex: IvIndex
    private lateinit var unicastAddress: UnicastAddress
    private lateinit var privateKey: PrivateKey
    private lateinit var publicKey: PublicKey
    private lateinit var sharedSecret: ByteArray
    private lateinit var authValue: ByteArray
    private lateinit var deviceConfirmation: ByteArray
    private lateinit var deviceRandom: ByteArray
    private var oobPublicKey: Boolean = false

    lateinit var algorithm: Algorithm
        private set
    lateinit var deviceKey: ByteArray
        private set
    lateinit var provisionerRandom: ByteArray
        private set
    lateinit var provisionerPublicKey: ByteArray
        private set

    private var confirmationInputs = byteArrayOf()

    val provisionerConfirmation: ByteArray
        get() = Crypto.calculateConfirmation(
            confirmationInputs,
            sharedSecret,
            provisionerRandom,
            authValue,
            algorithm
        )

    val encryptedProvisioningDataWithMic: ByteArray
        get() {
            val keys = Crypto.calculateKeys(
                algorithm,
                confirmationInputs,
                sharedSecret,
                provisionerRandom,
                deviceRandom
            )
            deviceKey = keys.third
            val flags = Flags.from(ivIndex, networkKey)
            val key = when (networkKey.phase) {
                KeyDistribution -> networkKey.oldKey!!
                else -> networkKey.key
            }
            val data = key + networkKey.index.toByteArray() +
                    flags.rawValue.toByteArray() + ivIndex.index.toByteArray() +
                    unicastAddress.address.toByteArray()
            return Crypto.encrypt(data = data, key = keys.first, nonce = keys.second, micSize = 8)
        }

    val security: Security
        get() = if (oobPublicKey) Secure else Insecure

    fun prepare(
        networkKey: NetworkKey,
        ivIndex: IvIndex,
        unicastAddress: UnicastAddress,
    ) {
        this.networkKey = networkKey
        this.ivIndex = ivIndex
        this.unicastAddress = unicastAddress
    }

    /**
     * Generates a key pair based on the given algorithm.
     *
     * @param algorithm Algorithm to use for key generation.
     */
    fun generateKeys(algorithm: Algorithm) {
        Crypto.generateKeyPair(algorithm).let {
            publicKey = it.public
            privateKey = it.private
        }

        provisionerPublicKey = publicKey.toByteArray()
        this.algorithm = algorithm

        // Generate Provisioner Random
        provisionerRandom = Crypto.generateRandom(algorithm.length)
    }

    /**
     *
     * This method adds the given PDU to the Provisioning Inputs. Provisioning Inputs are used for
     * authenticating the Provisioner and the Unprovisioned Device. This method must be called (in
     * order) for:
     * - Provisioning Invite,
     * - Provisioning Capabilities,
     * - Provisioning Start,
     * - Provisioner's Public Key,
     * - Provisionee's Public Key.
     *
     * @param data provisioning pdu.
     */
    fun accumulate(data: ByteArray) {
        confirmationInputs += data
    }

    /**
     * Invoked when the Provisionee's Public Key has been obtained. This must be called after
     * generating keys.
     *
     * @param key       Provisionee's Public Key.
     * @param usingOob  Indicates whether the Public Key was obtained Out-Of-Band.
     * @throws InvalidPublicKey if the Provisioner's keys have not been generated.
     */
    fun onDevicePublicKeyReceived(key: ByteArray, usingOob: Boolean) {
        sharedSecret = Crypto.calculateSharedSecret(privateKey = privateKey, publicKey = key)
        oobPublicKey = usingOob
    }

    /**
     * Invoked when the auth value is received from the device.
     *
     * @param authValue auth value
     */
    fun onAuthValueReceived(authValue: ByteArray) {
        this.authValue = authValue
    }

    /**
     * Invoked when the device confirmation is received from the device.
     *
     * @param confirmation device confirmation
     */
    fun onDeviceConfirmationReceived(confirmation: ByteArray) {
        this.deviceConfirmation = confirmation
    }

    /**
     * Invoked when the device random is received from the device.
     *
     * @param random device random
     */
    fun onDeviceRandomReceived(random: ByteArray) {
        this.deviceRandom = random
    }

    /**
     * Validates the received the Provisioning Confirmation and matches it with one calculated
     * locally based on the Provisioning data received from the device and Auth Value.
     *
     * @return true if the confirmation matches, false otherwise.
     */
    fun checkIfConfirmationsMatch() =
        if (deviceRandom.isNotEmpty() && authValue.isNotEmpty() && sharedSecret.isNotEmpty()) {
            Crypto.calculateConfirmation(
                confirmationInputs,
                sharedSecret,
                deviceRandom,
                authValue,
                algorithm
            ).contentEquals(deviceConfirmation)
        } else {
            throw InvalidState()
        }
}