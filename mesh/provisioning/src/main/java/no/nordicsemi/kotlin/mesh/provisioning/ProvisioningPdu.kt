@file:Suppress("ArrayInDataClass", "unused")

package no.nordicsemi.kotlin.mesh.provisioning

import no.nordicsemi.kotlin.data.toHexString
import no.nordicsemi.kotlin.mesh.crypto.Algorithm
import no.nordicsemi.kotlin.mesh.provisioning.ProvisioningPduType.*

internal typealias ProvisioningPdu = ByteArray

/**
 *  Provisioning PDU type.
 *
 *  @property type The type of provisioning pdu.
 */
internal enum class ProvisioningPduType(val type: Int) {

    /**
     * A Provisioner sends a Provisioning Invite PDU to indicate to the intended Provisionee that
     * the provisioning process is starting. The attention timer is used to identify the device
     * being provisioned among multiple unprovisioned devices.
     */
    INVITE(0),

    /**
     *  A Provisionee sends a Provisioning Capabilities PDU to indicate the provisioning
     *  capabilities of the device.
     */
    CAPABILITIES(1),

    /**
     * A Provisioner sends a Provisioning Start PDU to indicate the method it has selected from the
     * options in the Provisioning Capabilities PDU.
     */
    START(2),

    /**
     * The Provisioner sends a Provisioning Public Key PDU to deliver the public key to be used in
     * the ECDH calculation.
     */
    PUBLIC_KEY(3),

    /**
     * The Provisionee sends a Provisioning Input Complete PDU when the user completes the input
     * operation.
     */
    INPUT_COMPLETE(4),

    /**
     * The Provisioner or the Provisionee sends a Provisioning Confirmation PDU to its peer to
     * confirm the values exchanged so far, including the OOB Authentication value and the random
     * number that is yet to be exchanged.
     */
    CONFIRMATION(5),

    /**
     * The Provisioner or the Provisionee sends a Provisioning Random PDU to allow its peer device
     * to validate the confirmation.
     */
    RANDOM(6),

    /**
     * The Provisioner sends a Provisioning Data PDU to deliver provisioning data to a Provisionee.
     */
    DATA(7),

    /**
     * The Provisionee sends a Provisioning Complete PDU to indicate that it has successfully
     * received and processed the provisioning data.
     */
    COMPLETE(8),

    /**
     * The Provisionee sends a Provisioning Failed PDU if it fails to process a received
     * provisioning protocol PDU.
     */
    FAILED(9);

    internal companion object {

        /**
         * Returns the provisioning pdu type based on the pdu type.
         *
         * @param type The type of provisioning pdu.
         * @return ProvisioningPduType based on the given type.
         */
        fun from(type: Int): ProvisioningPduType? = when (type) {
            0 -> INVITE
            1 -> CAPABILITIES
            2 -> START
            3 -> PUBLIC_KEY
            4 -> INPUT_COMPLETE
            5 -> CONFIRMATION
            6 -> RANDOM
            7 -> DATA
            8 -> COMPLETE
            9 -> FAILED
            else -> null
        }
    }
}

/**
 * Returns the type of provisioning pdu.
 *
 *  @return provisioning pdu type or null if empty.
 */
internal fun ProvisioningPdu.type(): ProvisioningPduType? = if (isNotEmpty()) {
    ProvisioningPduType.from(this[0].toInt())
} else null

/**
 *  Validates a provisioning pdu based on the given type and it's length.
 *
 *  @return True if the pdu is valid, false otherwise.
 */
internal fun ProvisioningPdu.isValid() = when (type()) {
    INVITE, FAILED -> size == 1 + 1
    CAPABILITIES -> size == 1 + 11
    START -> size == 1 + 5
    PUBLIC_KEY -> size == 1 + 32 + 32
    INPUT_COMPLETE, COMPLETE -> size == 1 + 0
    CONFIRMATION, RANDOM -> size == 1 + 16 || size == 1 + 32
    DATA -> size == 1 + 25 + 8
    else -> false
}


/**
 * Provisioning requests are sent by the Provisioner to an unprovisioned device.
 *
 * @property pdu provisioning pdu.
 */
@Suppress("MemberVisibilityCanBePrivate")
sealed class ProvisioningRequest {

    /**
     * A Provisioner sends a Provisioning Invite PDU to indicate to the intended
     * Provisionee that the provisioning process is starting. The attention timer is used to
     * identify the device being provisioned among multiple unprovisioned devices.
     *
     * @property attentionTimer The attention timer value in seconds.
     * @constructor Creates a new Provisioning Invite PDU.
     */
    data class Invite(val attentionTimer: UByte) : ProvisioningRequest() {
        override fun toString() = "Provisioning Invite (attention timer: $attentionTimer sec)"
    }

    /**
     * A Provisioner sends a Provisioning Start PDU to indicate the method it has selected from the
     * options in the Provisioning Capabilities PDU.
     *
     * @property algorithm                 Algorithm to be used.
     * @property publicKey                 Public key method to be used.
     * @property method                    Authentication method to be used.
     * @constructor Creates a new Provisioning Start PDU
     */
    data class Start(
        val algorithm: Algorithm,
        val publicKey: PublicKeyMethod,
        val method: AuthenticationMethod
    ) : ProvisioningRequest() {

        internal constructor(configuration: ProvisioningParameters) : this(
            configuration.algorithm,
            configuration.publicKey.method,
            configuration.authMethod
        )

        override fun toString() = "Provisioning Start (algorithm: $algorithm, public key: " +
                "$publicKey, method: $method)"
    }

    /**
     * The Provisioner sends a Provisioning Public Key PDU to deliver the public key to be used in
     * the ECDH calculation.
     *
     * @property publicKey Public key to be used in the ECDH calculation.
     * @constructor Creates a new Provisioning PublicKey PDU.
     */
    data class PublicKey(val publicKey: ByteArray) : ProvisioningRequest() {
        override fun toString() = "Provisioning PublicKey (${publicKey.toHexString(prefixOx = true)})"
    }

    /**
     * The Provisioner or the Provisionee sends a Provisioning Confirmation PDU to its peer to
     * confirm the values exchanged so far, including the OOB Authentication value and the random
     * number that is yet to be exchanged.
     *
     * @property confirmation Confirmation value.
     * @constructor Creates a new Provisioning Confirmation PDU.
     */
    data class Confirmation(val confirmation: ByteArray) : ProvisioningRequest() {

        override fun toString() = "Provisioning Confirmation " +
                "(${confirmation.toHexString(prefixOx = true)})"
    }

    /**
     * The Provisioner or the Provisionee sends a Provisioning Random PDU to allow its peer device
     * to validate the confirmation.
     *
     * @property random Random value.
     * @constructor Creates a new Provisioning Random PDU.
     */
    data class Random(val random: ByteArray) : ProvisioningRequest() {
        override fun toString() = "Provisioning Random (${random.toHexString(prefixOx = true)})"
    }

    /**
     * The Provisioner sends a Provisioning Data PDU to deliver provisioning data to a Provisionee.
     *
     * @property encryptedDataWithMic Random value.
     * @constructor Creates a new Provisioning Data PDU.
     */
    data class Data(val encryptedDataWithMic: ByteArray) : ProvisioningRequest() {
        override fun toString() = "Provisioning Data (${encryptedDataWithMic.toHexString(prefixOx = true)})"
    }

    val pdu: ProvisioningPdu
        get() = when (this) {
            is Invite -> ProvisioningPdu(1) { INVITE.type.toByte() } + attentionTimer.toByte()
            is Start -> ProvisioningPdu(1) { START.type.toByte() } +
                    algorithm.value.toByte() +
                    publicKey.value.toByte() +
                    method.value

            is PublicKey -> ProvisioningPdu(1) { PUBLIC_KEY.type.toByte() } + publicKey
            is Confirmation -> ProvisioningPdu(1) { CONFIRMATION.type.toByte() } + confirmation
            is Random -> ProvisioningPdu(1) { RANDOM.type.toByte() } + random
            is Data -> ProvisioningPdu(1) { DATA.type.toByte() } + encryptedDataWithMic
        }

    companion object {

        /**
         * Creates a new provisioning request based on the given pdu.
         *
         * @param pdu Provisioning pdu.
         * @return Provisioning request.
         * @throws InvalidPdu if the pdu is invalid.
         */
        @Throws(InvalidPdu::class)
        fun from(pdu: ProvisioningPdu): ProvisioningRequest {
            val pduType = pdu.type()
            require(pduType != null && pdu.isValid()) { throw InvalidPdu() }
            return when (pduType) {
                INVITE -> Invite(pdu[1].toUByte())
                START -> {
                    val algorithm = Algorithm.from(pdu)
                    val publicKey = PublicKeyMethod.from(pdu)
                    val method = AuthenticationMethod.from(pdu)
                    require(algorithm != null && publicKey != null && method != null) {
                        throw InvalidPdu()
                    }
                    Start(algorithm, publicKey, method)
                }

                PUBLIC_KEY -> PublicKey(pdu.copyOfRange(1, pdu.size))
                CONFIRMATION -> Confirmation(pdu.copyOfRange(1, pdu.size))
                RANDOM -> Random(pdu.copyOfRange(1, pdu.size))
                else -> throw InvalidPdu()
            }
        }
    }
}

/**
 * Provisioning responses are sent by the Provisionee to the Provisioner. as a response to a
 * [ProvisioningRequest].
 */
sealed class ProvisioningResponse {

    /**
     * The Provisionee sends a Provisioning Capabilities PDU to indicate it's supported provisioning
     * to a Provisioner.
     *
     * @property capabilities Provisioning capabilities.
     */
    data class Capabilities(val capabilities: ProvisioningCapabilities) : ProvisioningResponse() {
        override fun toString() = "Device Capabilities \n$capabilities"
    }

    /**
     * The Provisionee sends a Provisioning Input Complete PDU when the user completes the input
     * operation.
     */
    object InputComplete : ProvisioningResponse() {
        override fun toString() = "Input Complete"
    }

    /**
     * The Provisioner sends a Provisioning Public Key PDU to deliver the public key to be used in
     * the ECDH calculations.
     *
     * @property key public key.
     */
    data class PublicKey(val key: ByteArray) : ProvisioningResponse() {
        override fun toString() = "Device PublicKey (${key.toHexString(prefixOx = true)})"
    }

    /**
     * The provisioner or the Provisionee sends a Provisioning Confirmation PDU to its peer to
     * confirm the values exchanged so far, including the OOB Authentication value and the random
     * number that is yet to be exchanged.
     *
     * @property confirmation confirmation value.
     */
    data class Confirmation(val confirmation: ByteArray) : ProvisioningResponse() {
        override fun toString() = "Device Confirmation (${confirmation.toHexString(prefixOx = true)})"
    }

    /**
     * The Provisioner or the Provisionee sends a Provisioning Random PDU to allow its peer device
     * to validate the confirmation.
     *
     * @property random random value.
     */
    data class Random(val random: ByteArray) : ProvisioningResponse() {
        override fun toString() = "Device Random (${random.toHexString(prefixOx = true)})"
    }

    /**
     * The Provisionee sends a Provisioning Complete PDU to indicate that it has successfully
     * received and processed the provisioning data.
     */
    object Complete : ProvisioningResponse() {
        override fun toString() = "Provisioning Complete"
    }

    /**
     * The Provisionee sends a Provisioning Failed PDU if it fails to process a received
     * provisioning protocol PDU.
     *
     * @property error Provisioning error
     */
    data class Failed(val error: RemoteProvisioningError) : ProvisioningResponse() {
        override fun toString() = "Provisioning Failed (${error})"
    }

    val pdu: ProvisioningPdu
        get() = when (this) {
            is Capabilities -> ProvisioningPdu(1) {
                CAPABILITIES.type.toByte()
            } + capabilities.value

            is InputComplete -> ProvisioningPdu(1) { INPUT_COMPLETE.type.toByte() }
            is PublicKey -> ProvisioningPdu(1) { PUBLIC_KEY.type.toByte() } + key
            is Confirmation -> ProvisioningPdu(1) {
                CONFIRMATION.type.toByte()
            } + confirmation

            is Random -> ProvisioningPdu(1) { RANDOM.type.toByte() } + random
            is Complete -> ProvisioningPdu(1) { COMPLETE.type.toByte() }
            is Failed -> ProvisioningPdu(2) {
                FAILED.type.toByte()
            } + error.errorCode.toByte()
        }

    companion object {

        /**
         * Creates a new provisioning response based on the given pdu.
         *
         * @param pdu Provisioning pdu.
         * @return Provisioning response.
         * @throws InvalidPdu if the pdu is invalid.
         */
        @Throws(InvalidPdu::class)
        fun from(pdu: ProvisioningPdu): ProvisioningResponse {
            val pduType = pdu.type()
            require(pduType != null && pdu.isValid()) { throw InvalidPdu() }
            return when (pduType) {
                CAPABILITIES -> Capabilities(ProvisioningCapabilities(pdu))
                INPUT_COMPLETE -> InputComplete
                PUBLIC_KEY -> PublicKey(pdu.copyOfRange(1, pdu.size))
                CONFIRMATION -> Confirmation(pdu.copyOfRange(1, pdu.size))
                RANDOM -> Random(pdu.copyOfRange(1, pdu.size))
                COMPLETE -> Complete
                FAILED -> RemoteProvisioningError.entries.firstOrNull {
                    it.errorCode == pdu[1].toInt()
                }?.let { Failed(it) } ?: throw InvalidPdu()
                else -> throw InvalidPdu()
            }
        }
    }
}