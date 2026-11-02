@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.layers.network

import kotlin.time.Clock
import kotlin.time.Instant
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey

/**
 * Base interface for a Network Beacon PDU
 *
 * @property networkKey                     Network key used to encrypt the beacon.
 * @property validForKeyRefreshProcedure    Flag indicating whether the beacon is valid for the key
 *                                          refresh procedure.
 * @property keyRefreshFlag                 Flag indicating whether the beacon is sent with the key.
 * @property ivIndex                        The IV Index of the beacon.
 */
internal interface NetworkBeaconPdu : BeaconPdu {
    val networkKey: NetworkKey
    val validForKeyRefreshProcedure: Boolean
    val keyRefreshFlag: Boolean
    val ivIndex: IvIndex

    /**
     * This method returns whether the received network beacon can override the current IV Index.
     *
     * The following restrictions apply:
     * 1. Normal Operation state must last for at least 96 hours.
     * 2. IV Update In Progress state must take at least 96 hours and may not be longer than 144h.
     * 3. IV Index must not decrease.
     * 4. If received Secure Network beacon or Private beacon has IV Index greater than current
     *    IV Index + 1, the device will go into IV Index Recovery procedure. In this state, the 96h
     *    rule does not apply and the IV Index or IV Update Active flag may change before 96 hours.
     * 5. If received Secure Network beacon or Private beacon has IV Index greater than current
     *    IV Index + 42, the beacon should be ignored (unless a setting
     *    [MeshNetworkManager.ivUpdateTestMode] is set to disable this rule).
     * 6. The node shall not execute more than one IV Index Recovery within a period of 192 hours.
     *
     * Refer: Bluetooth Mesh Profile 1.0.1, section 3.10.5.
     *
     * @param target                    IV Index to compare.
     * @param updatedAt                 Date of the most recent transition to the current IV Index.
     * @param isIvRecoveryActive          True if the IV Recovery procedure was used to restore the IV
     *                                  Index on the previous connection.
     * @param isIvTestModeActive                True, if IV Update test mode is enabled; false otherwise.
     * @param ivRecoveryOver42Allowed   Whether the IV Index Recovery procedure should be limited to
     *                                  allow maximum increase of IV Index by 42.
     * @returns: True, if the network information can be applied; false otherwise.
     */
    fun canOverWrite(
        target: IvIndex,
        updatedAt: Instant?,
        isIvRecoveryActive: Boolean,
        isIvTestModeActive: Boolean,
        ivRecoveryOver42Allowed: Boolean
    ): Boolean {
        // IV Index must increase, or, in case it's equal to the current one, the IV Update Active
        // flag must change from true to false. The new index must not be greater than the
        // current one + 42, unless this rule is disabled.

        require(
            (ivIndex.index > target.index &&
                    (ivRecoveryOver42Allowed || ivIndex.index <= target.index + 42u)) ||
                    (ivIndex.index == target.index &&
                            (target.isIvUpdateActive || !ivIndex.isIvUpdateActive))
        ) { return false }

        return updatedAt?.let { date ->
            // Let's define a "state" as a pair of IV and IV Update Active flag. "States" change as
            // follows:
            // 1. IV = X,   IVUA = false (Normal Operation)
            // 2. IV = X+1, IVUA = true  (Update In Progress)
            // 3. IV = X+1, IVUA = false (Normal Operation)
            // 4. IV = X+2, IVUA = true  (Update In Progress)
            // 5. ...

            // Calculate number of states between the state defined by the target
            // IV Index and this Secure Network Beacon.
            val stateDiff = (ivIndex.index - target.index) * 2u - 1u +
                    (if (target.isIvUpdateActive) 1u else 0u) +
                    (if (ivIndex.isIvUpdateActive) 0u else 1u) -
                    (if (isIvRecoveryActive || isIvTestModeActive) 1u else 0u) //  this may set stateDiff = -1

            // Each "state" must last for at least 96 hours.
            // Calculate the minimum number of hours that had to pass since last state change for
            // the beacon to be assumed valid.
            // If more has passed, it's also valid, as Normal Operation has no maximum time duration.
            val numberOfHoursRequired = stateDiff.toInt() * 96

            // Get the number of hours since the state changed last time.
            val numberOfHoursSinceDate = (Clock.System.now() - date).inWholeHours

            // The node shall not execute more than one IV Index Recovery within a
            // period of 192 hours.
            if (isIvRecoveryActive && stateDiff.toInt() > 1 && numberOfHoursSinceDate < 192) {
                return false
            }

            numberOfHoursSinceDate >= numberOfHoursRequired
        } ?: true
    }
}

/**
 * Helper object for decoding Network beacons.
 */
internal object NetworkBeaconPduDecoder {

    /**
     * Decodes the given beacon PDU by iterating through all network keys
     *
     * @param pdu           Beacon PDU
     * @param meshNetwork   Mesh network
     * @return A [SecureNetworkBeacon] or [PrivateBeacon] if the beacon was successfully decoded or
     * null otherwise.
     */
    fun decode(pdu: ByteArray, meshNetwork: MeshNetwork): NetworkBeaconPdu? = when {
        pdu.size > 1 -> when (BeaconType.from(pdu[0].toUByte())) {
            BeaconType.SECURE_NETWORK -> {
                meshNetwork.networkKeys.forEach { networkKey ->
                    SecureNetworkBeaconDecoder.decode(pdu, networkKey)?.let { beacon ->
                        return beacon
                    }
                }
                null
            }

            BeaconType.PRIVATE -> {
                meshNetwork.networkKeys.forEach { networkKey ->
                    PrivateBeaconDecoder.decode(pdu, networkKey)?.let { beacon ->
                        return beacon
                    }
                }
                null
            }

            else -> null
        }

        else -> null

    }

}
