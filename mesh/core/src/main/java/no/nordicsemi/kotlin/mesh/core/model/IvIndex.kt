@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package no.nordicsemi.kotlin.mesh.core.model

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.math.max

/**
 * The IV Index received with the last Secure Network Beacon and its
 * current state.
 *
 * Bluetooth Mesh Profile Specification 1.0.1, Chapter 3.10.5:
 *
 * During the Normal Operation state, the IV Update Flag in the Secure Network beacon and in the
 * Friend Update message shall be set to 0. When this state is active, a node shall transmit using
 * the current IV Index and shall process messages from the current IV Index and also the current
 * IV Index - 1.
 *
 * During the IV Update in Progress state, the IV Update Flag in the Secure Network beacon and in
 * the Friend Update message shall be set to 1. When this state is active, a node shall transmit
 * using the current IV Index - 1 and shall process messages from the current IV Index - 1 and also
 * the current IV Index.
 *
 * @property index             A 32-bit shared network resource. All nodes in a mesh network share
 *                             the same value of the IV Index and use it for all subnets they belong
 *                             to.
 * @property isIvUpdateActive  Set to true if IV Update is in progress and false if the network is
 *                             in normal operation.
 * @property transitionDate    Time when the last iv update happened.
 * @property ivRecoveryFlag    Represents the current iv recovery procedure state. A node that is
 *                             away from the network for a long time may miss IV Update procedures,
 *                             in which case it can no longer communicate with the other nodes. In
 *                             order to recover the IV Index, the node must listen for a Secure
 *                             Network beacon, which contains the Network ID and the current
 *                             IV Index. Upon receiving and successfully authenticating a Secure
 *                             Network beacon for a primary subnet whose IV Index is 1 or more
 *                             higher than the current known IV Index, the node shall set its
 *                             current IV Index and its current IV Update procedure state from the
 *                             values in this Secure Network beacon.
 * @property transmitIvIndex   IV index used when transmitting messages.
 * @property next              Next IV Index to be used or null if max value is reached.
 * @property previous          Previous IV Index or null in case of initial one.
 */
data class IvIndex(
    val index: UInt = 0u,
    val isIvUpdateActive: Boolean = false,
    val transitionDate: Instant = Clock.System.now(),
) : Comparable<IvIndex> {

    var ivRecoveryFlag = false
        internal set

    val transmitIvIndex: UInt
        get() = when (isIvUpdateActive && index > 0u) {
            true -> index - 1u
            false -> index
        }

    val next: IvIndex?
        get() = when (isIvUpdateActive) {
            true -> IvIndex(index = index, isIvUpdateActive = false)
            false -> when (index < UInt.MAX_VALUE - 1u) {
                true -> IvIndex(index = index + 1u, isIvUpdateActive = true)
                false -> null
            }
        }

    val previous: IvIndex?
        get() = when (!isIvUpdateActive) {
            true -> IvIndex(index = index, isIvUpdateActive = true)
            false -> when (index > 0u) {
                true -> IvIndex(index = index - 1u, isIvUpdateActive = false)
                false -> null
            }
        }

    /**
     * The IV Index that is to be used for decrypting messages.
     *
     * @param ivi   The IV Index received with the message.
     * @return The IV Index that is to be used for decrypting messages.
     */
    fun index(ivi: Byte) = when {
        ivi.toUInt() == index and 1u -> index
        else -> max(1u, index) - 1u
    }

    override fun equals(other: Any?): Boolean {
        if(other is IvIndex) {
            return index == other.index &&
                    isIvUpdateActive == other.isIvUpdateActive
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = isIvUpdateActive.hashCode()
        result = 31 * result + ivRecoveryFlag.hashCode()
        result = 31 * result + index.hashCode()
        result = 31 * result + transitionDate.hashCode()
        result = 31 * result + transmitIvIndex.hashCode()
        result = 31 * result + (next?.hashCode() ?: 0)
        result = 31 * result + (previous?.hashCode() ?: 0)
        return result
    }

    override fun toString() = "IV Index: $index, ${
        when (isIvUpdateActive) {
            true -> "IV Update Active"
            false -> "Normal Operation"
        }
    }"

    override fun compareTo(other: IvIndex): Int {
        return other.index.compareTo(other.index)
    }

    companion object {
        const val TIME_STAMP_KEY = "IVTimestamp"
        const val IV_RECOVERY_KEY = "IVRecovery"
        const val INDEX_KEY = "IVIndex"
    }
}