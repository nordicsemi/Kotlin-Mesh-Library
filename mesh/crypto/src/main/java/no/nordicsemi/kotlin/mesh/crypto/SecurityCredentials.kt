package no.nordicsemi.kotlin.mesh.crypto

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.data.ByteOrder

sealed class SecurityCredentials {

    data object ManagedFlooding : SecurityCredentials() {
        override val P = byteArrayOf(0x00)
    }

    data class Friendship(
        val lpnAddress: UShort,
        val lpnCounter: UShort,
        val friendAddress: UShort,
        val friendCounter: UShort,
    ): SecurityCredentials() {
        override val P =  byteArrayOf(0x01) +
                lpnAddress.toByteArray(order = ByteOrder.BIG_ENDIAN) +
                lpnCounter.toByteArray(order = ByteOrder.BIG_ENDIAN) +
                friendAddress.toByteArray(order = ByteOrder.BIG_ENDIAN) +
                friendCounter.toByteArray(order = ByteOrder.BIG_ENDIAN)
    }

    data object DirectedFlooding : SecurityCredentials() {
        override val P = byteArrayOf(0x02)
    }

    @Suppress("PropertyName")
    internal abstract val P: ByteArray
}