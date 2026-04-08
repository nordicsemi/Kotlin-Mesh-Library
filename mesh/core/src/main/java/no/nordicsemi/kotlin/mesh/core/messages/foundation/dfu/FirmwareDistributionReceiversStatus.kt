package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import java.nio.ByteOrder

/**
 * Firmware Distribution Receivers Status message is an unacknowledged message sent by a Firmware
 * Distribution Server to report the size of the Distribution Receivers List state.
 *
 * Firmware Distribution Receivers Status message is sent as a response to
 * [FirmwareDistributionReceiversAdd] message or a [FirmwareDistributionReceiversDeleteAll] message.
 *
 * @property totalCount Number of entries in the Distribution Receivers List state.
 */
class FirmwareDistributionReceiversStatus(
    override val status: FirmwareDistributionMessageStatus,
    val totalCount: UShort
) : MeshResponse, FirmwareDistributionStatusMessage {
    override val opCode: UInt = Initializer.opCode
    override val parameters = status.value.toByteArray() +
            totalCount.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x8313u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 3 }
            ?.let { params ->
                FirmwareDistributionMessageStatus
                    .from(value = params[0].toUByte())
                    ?.let { status ->
                    FirmwareDistributionReceiversStatus(
                        status = status,
                        totalCount = params.getUShort(
                            offset = 1,
                            order = ByteOrder.LITTLE_ENDIAN
                        )
                    )
                }
            }
    }
}