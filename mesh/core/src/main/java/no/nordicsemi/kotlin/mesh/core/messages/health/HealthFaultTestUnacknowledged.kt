@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.health

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.HealthMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import java.nio.ByteOrder

/**
 * A Health Fault Test Unacknowledged is an unacknowledged message used to invoke
 * a self-test of an element.
 *
 * @property testId Identifier of a specific test to be performed.
 * @property companyIdentifier 16-bit Bluetooth assigned Company Identifier.
 */
class HealthFaultTestUnacknowledged(
    val testId: UByte,
    val companyIdentifier: UShort
) : UnacknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray = byteArrayOf(testId.toByte()) +
            companyIdentifier.toByteArray(ByteOrder.LITTLE_ENDIAN)

    override fun toString() = "HealthFaultTestUnacknowledged(testId: $testId, companyIdentifier: $companyIdentifier)"

    companion object Initializer : HealthMessageInitializer {
        override val opCode = 0x8033u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 3 }
            ?.let { params ->
                HealthFaultTestUnacknowledged(
                    testId = params[0].toUByte(),
                    companyIdentifier = params.getUShort(offset = 1, order = ByteOrder.LITTLE_ENDIAN)
                )
            }
    }
}
