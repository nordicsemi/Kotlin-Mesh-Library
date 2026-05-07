@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.health

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.HealthMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import java.nio.ByteOrder

/**
 * A Health Fault Status is an unacknowledged message used to report the current
 * Registered Fault state of an element.
 *
 * @property testId Identifier of a most recently performed self-test.
 * @property companyIdentifier 16-bit Bluetooth assigned Company Identifier.
 * @property faults A list of fault values, which indicate conditions
 *                  that are currently present on the node.
 */
class HealthFaultStatus(
    val testId: UByte,
    val companyIdentifier: UShort,
    val faults: List<HealthFault> = listOf(),
) : MeshResponse, UnacknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray =
        byteArrayOf(testId.toByte()) +
                companyIdentifier.toByteArray(ByteOrder.LITTLE_ENDIAN) +
                ByteArray(faults.size) { i -> faults[i].code.toByte() }

    override fun toString() = "HealthFaultStatus(testId: $testId, " +
            "companyIdentifier: ${
                companyIdentifier.toHexString(
                    format = HexFormat {
                        number.prefix = "0x"
                        upperCase = true
                    }
                )
            }, faults: $faults)"

    companion object Initializer : HealthMessageInitializer {
        override val opCode = 0x05u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size >= 3 }
            ?.let { params ->
                val faultsData = params.copyOfRange(3, params.size)
                HealthFaultStatus(
                    testId = params[0].toUByte(),
                    companyIdentifier = params.getUShort(
                        offset = 1,
                        order = ByteOrder.LITTLE_ENDIAN
                    ),
                    faults = faultsData.map { b: Byte ->
                        HealthFault.from(code = b.toUByte())
                    }
                )
            }
    }
}
