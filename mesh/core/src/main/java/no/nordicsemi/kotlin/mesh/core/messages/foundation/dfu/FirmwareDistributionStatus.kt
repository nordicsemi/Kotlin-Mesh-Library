@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.and
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.shl
import no.nordicsemi.kotlin.data.shr
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.BLOBMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionPhase
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdatePolicy
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.TransferMode
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.DistributionMulticastAddress
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import no.nordicsemi.kotlin.mesh.core.model.FixedGroupAddress
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress
import java.nio.ByteOrder

/**
 * The Firmware Distribution Status message is an unacknowledged message sent by a Firmware
 * Distribution Server to report th status of a firmware image distribution
 *
 * A Firmware Distribution Status message is sent as a response to any of [FirmwareDistributionGet],
 * [FirmwareDistributionStart], [FirmwareDistributionSuspend], [FirmwareDistributionCancel],
 * [FirmwareDistributionApply] messages.
 *
 * @property phase                    Phase of the firmware image distribution.
 * @property multicastAddress         Multicast Address used in a Firmware Image Distribution. The
 *                                    value of the Distribution Multicast Address field shall be a
 *                                    [GroupAddress], [FixedGroupAddress] or a [VirtualAddress].
 * @property firmwareImageIndex       Index of the firmware image in the Firmware Images List state
 *                                    to use during firmware image distribution.
 * @property multicastAddress         [DistributionMulticastAddress] can be [GroupAddress],
 *                                    [FixedGroupAddress], [VirtualAddress] and [UnassignedAddress]
 *                                    used in a firmware image distribution.
 * @property applicationKeyIndex      Index of the application key used in a firmware image distribution.
 * @property ttl                      Time To Live (TTL) value used in a firmware image distribution.
 * @property distributionTransferMode Transfer mode used in a firmware image distribution.
 * @property updatePolicy             Update policy used in a firmware image distribution.
 * @property distributionTimeoutBase  Base timeout value used in a firmware image distribution.
 */
class FirmwareDistributionStatus(
    override val status: FirmwareDistributionMessageStatus,
    val phase: FirmwareDistributionPhase,
    val firmwareImageIndex: UShort?,
    val multicastAddress: Address?,
    val applicationKeyIndex: KeyIndex?,
    val ttl: UByte?,
    val distributionTransferMode: TransferMode?,
    val updatePolicy: FirmwareUpdatePolicy?,
    val distributionTimeoutBase: UShort?,
) : MeshResponse, FirmwareDistributionStatusMessage {
    override val opCode: UInt = Initializer.opCode
    override val parameters = status.value.toByteArray() +
            phase.value.toByteArray() +
            if (multicastAddress != null &&
                applicationKeyIndex != null &&
                ttl != null &&
                distributionTimeoutBase != null &&
                distributionTransferMode != null &&
                updatePolicy != null &&
                firmwareImageIndex != null
            ) {
                multicastAddress.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                        applicationKeyIndex.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                        ttl.toByteArray() +
                        distributionTimeoutBase.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                        (distributionTransferMode.value or (updatePolicy.value shl 2)).toByteArray() +
                        firmwareImageIndex.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
            } else byteArrayOf()

    /**
     * Convenience constructor to create a FirmwareDistributionStatus message.
     *
     * @param phase                    Phase of the firmware image distribution.
     * @param multicastAddress         Multicast Address used in a Firmware Image Distribution. The
     *                                 value of the Distribution Multicast Address field shall be a
     *                                 [GroupAddress], [FixedGroupAddress] or a [VirtualAddress].
     * @param firmwareImageIndex       Index of the firmware image in the Firmware Images List state
     *                                 to use during firmware image distribution.
     * @param multicastAddress         [DistributionMulticastAddress] can be [GroupAddress],
     *                                 [FixedGroupAddress], [VirtualAddress] and [UnassignedAddress]
     *                                 used in a firmware image distribution.
     * @param applicationKeyIndex      Index of the application key used in a firmware image distribution.
     * @param ttl                      Time To Live (TTL) value used in a firmware image distribution.
     * @param distributionTransferMode Transfer mode used in a firmware image distribution.
     * @param updatePolicy             Update policy used in a firmware image distribution.
     * @param distributionTimeoutBase  Base timeout value used in a firmware image distribution.
     */
    constructor(status: FirmwareDistributionMessageStatus) : this(
        status = status,
        phase = FirmwareDistributionPhase.IDLE,
        firmwareImageIndex = null,
        multicastAddress = null,
        applicationKeyIndex = null,
        ttl = null,
        distributionTransferMode = null,
        updatePolicy = null,
        distributionTimeoutBase = null
    )

    /**
     * Convenience constructor to create a FirmwareDistributionStatus message.
     *
     * @param phase                         Phase of the firmware image distribution.
     * @param distributionMulticastAddress  Multicast Address used in a Firmware Image Distribution.
     *                                      The value of the Distribution Multicast Address field
     *                                      shall be a [GroupAddress], [FixedGroupAddress] or a
     *                                      [VirtualAddress].
     * @param firmwareImageIndex            Index of the firmware image in the Firmware Images List
     *                                      state to use during firmware image distribution.
     * @param multicastAddress              [DistributionMulticastAddress] can be [GroupAddress],
     *                                      [FixedGroupAddress], [VirtualAddress] and
     *                                      [UnassignedAddress] used in a firmware image distribution.
     * @param applicationKeyIndex           Index of the application key used in a firmware image
     *                                      distribution.
     * @param ttl                           Time To Live (TTL) value used in a firmware image distribution.
     * @param distributionTransferMode      Transfer mode used in a firmware image distribution.
     * @param updatePolicy                  Update policy used in a firmware image distribution.
     * @param distributionTimeoutBase       Base timeout value used in a firmware image distribution.
     */
    constructor(
        status: FirmwareDistributionMessageStatus,
        phase: FirmwareDistributionPhase,
        firmwareImageIndex: UShort?,
        distributionMulticastAddress: DistributionMulticastAddress?,
        applicationKeyIndex: KeyIndex?,
        ttl: UByte?,
        distributionTransferMode: TransferMode?,
        updatePolicy: FirmwareUpdatePolicy?,
        distributionTimeoutBase: UShort?,
    ) : this(
        status = status,
        phase = phase,
        firmwareImageIndex = firmwareImageIndex,
        multicastAddress = distributionMulticastAddress?.address,
        applicationKeyIndex = applicationKeyIndex,
        ttl = ttl,
        distributionTransferMode = distributionTransferMode,
        updatePolicy = updatePolicy,
        distributionTimeoutBase = distributionTimeoutBase
    )

    companion object Initializer : BLOBMessageInitializer {
        override val opCode: UInt = 0x831Du

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 2 || it.size == 12
        }?.let { params ->
            val status = FirmwareDistributionMessageStatus.from(value = params[0].toUByte())
                ?: return@let null

            if (parameters.size == 12) {
                FirmwareDistributionStatus(
                    status = status,
                    multicastAddress = params.getUShort(
                        offset = 2,
                        order = ByteOrder.LITTLE_ENDIAN
                    ),
                    applicationKeyIndex = params.getUShort(
                        offset = 4,
                        order = ByteOrder.LITTLE_ENDIAN
                    ),
                    ttl = params[6].toUByte(),
                    distributionTimeoutBase = params.getUShort(
                        offset = 7,
                        order = ByteOrder.LITTLE_ENDIAN
                    ),
                    distributionTransferMode = TransferMode.from(
                        value = (params[9] and 0x03).toUByte()
                    ) ?: return@let null,
                    updatePolicy = FirmwareUpdatePolicy.from(
                        value = ((params[9] shr 2) and 0x01).toUByte()
                    ) ?: return@let null,
                    phase = FirmwareDistributionPhase.from(
                        value = params[1].toUByte()
                    ) ?: return@let null,
                    firmwareImageIndex = params.getUShort(
                        offset = 10,
                        order = ByteOrder.LITTLE_ENDIAN
                    )
                )
            } else FirmwareDistributionStatus(status = status)
        }
    }
}