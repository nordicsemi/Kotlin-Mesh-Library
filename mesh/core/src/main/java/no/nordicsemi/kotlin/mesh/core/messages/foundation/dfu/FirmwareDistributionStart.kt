@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.and
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.shl
import no.nordicsemi.kotlin.data.shr
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdatePolicy
import no.nordicsemi.kotlin.mesh.core.messages.TransferMode
import no.nordicsemi.kotlin.mesh.core.model.DistributionMulticastAddress
import no.nordicsemi.kotlin.mesh.core.model.FixedGroupAddress
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import java.nio.ByteOrder
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The Firmware Distribution Start message is an acknowledged message sent by a Firmware
 * Distribution Client to start the firmware image distribution to the Target Nodes in the
 * Distribution Receivers List.
 *
 * @property firmwareImageIndex       Index of the firmware image in the Firmware Images List state
 *                                    to use during firmware image distribution. The maximum
 *                                    supported Firmware Images List Size can be obtained using
 *                                    [FirmwareDistributionCapabilitiesGet] message.
 * @property multicastAddress         Multicast Address used in a Firmware Image Distribution. The
 *                                    value of the Distribution Multicast Address field shall be a
 *                                    [GroupAddress], [FixedGroupAddress], [VirtualAddress] or an
 *                                    [UnassignedAddress]. If the value of the Distribution
 *                                    Multicast Address state is the Unassigned address, then
 *                                    messages are not sent to a multicast address.
 * @property applicationKeyIndex      Index of the application key used in a firmware image
 *                                    distribution used in a firmware image distribution.
 * @property ttl                      Time To Live (TTL) value used in a firmware image distribution.
 * @property distributionTransferMode Transfer mode used in a firmware image distribution.
 * @property updatePolicy             Update policy used in a firmware image distribution.
 * @property distributionTimeoutBase  The value that is used to calculate when firmware image
 *                                    distribution will be suspended. The Timeout is calculated using
 *                                    the following formula:
 *                                    Timeout = (10,000 × (Timeout Base + 2)) + (100 × Transfer TTL)
 *                                    milliseconds.
 */
class FirmwareDistributionStart internal constructor(
    val applicationKeyIndex: KeyIndex,
    val ttl: UByte = 0xFF.toUByte(),
    val distributionTimeoutBase: UShort = 118u, // 20 minutes
    val distributionTransferMode: TransferMode = TransferMode.PUSH,
    val updatePolicy: FirmwareUpdatePolicy = FirmwareUpdatePolicy.VERIFY_AND_APPLY,
    val firmwareImageIndex: UShort,
    val multicastAddress: DistributionMulticastAddress,
) : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = FirmwareDistributionStatus.opCode

    @OptIn(ExperimentalUuidApi::class)
    override val parameters: ByteArray
        get() {
            val data = applicationKeyIndex.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    ttl.toByteArray() +
                    distributionTimeoutBase.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    ((distributionTransferMode.value) or (updatePolicy.value shl 2)).toByteArray() +
                    firmwareImageIndex.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
            return when {
                multicastAddress is VirtualAddress -> data + multicastAddress.uuid.toByteArray()
                else -> data + UnassignedAddress.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
            }
        }

    /**
     * Convenience constructor to create a FirmwareDistributionStart message.
     *
     * @param firmwareImageIndex       Index of the firmware image in the Firmware Images List state
     *                                 to use during firmware image distribution. The maximum
     *                                 supported Firmware Images List Size can be obtained using
     *                                 [FirmwareDistributionCapabilitiesGet] message.
     * @param groupAddress             Multicast Address used in a Firmware Image Distribution. The
     *                                 value of the Distribution Multicast Address field shall be a
     *                                 [GroupAddress], [FixedGroupAddress], [VirtualAddress] or an
     *                                 [UnassignedAddress]. If the value of the Distribution
     *                                 Multicast Address state is the Unassigned address, then
     *                                 messages are not sent to a multicast address.
     * @param applicationKeyIndex      Index of the application key used in a firmware image
     *                                 distribution used in a firmware image distribution.
     * @param ttl                      Time To Live (TTL) value used in a firmware image distribution.
     * @param distributionTransferMode Transfer mode used in a firmware image distribution.
     * @param updatePolicy             Update policy used in a firmware image distribution.
     * @param distributionTimeoutBase  The value that is used to calculate when firmware image
     *                                 distribution will be suspended. The Timeout is calculated
     *                                 using the following formula:
     *                                 Timeout = (10,000 × (Timeout Base + 2)) + (100 × Transfer TTL)
     *                                 milliseconds.
     */
    constructor(
        firmwareImageIndex: UShort,
        groupAddress: GroupAddress,
        applicationKeyIndex: KeyIndex,
        ttl: UByte = 0xFF.toUByte(),
        distributionTransferMode: TransferMode = TransferMode.PUSH,
        updatePolicy: FirmwareUpdatePolicy = FirmwareUpdatePolicy.VERIFY_AND_APPLY,
        distributionTimeoutBase: UShort = 118u, // 20 minutes
    ) : this(
        firmwareImageIndex = firmwareImageIndex,
        multicastAddress = groupAddress,
        applicationKeyIndex = applicationKeyIndex,
        ttl = ttl,
        distributionTransferMode = distributionTransferMode,
        updatePolicy = updatePolicy,
        distributionTimeoutBase = distributionTimeoutBase
    )

    /**
     * Convenience constructor to create a FirmwareDistributionStart message.
     *
     * @param firmwareImageIndex       Index of the firmware image in the Firmware Images List state
     *                                 to use during firmware image distribution. The maximum
     *                                 supported Firmware Images List Size can be obtained using
     *                                 [FirmwareDistributionCapabilitiesGet] message.
     * @param labelUuid                Label Uuid used in a firmware image distribution.
     * @param applicationKeyIndex      Index of the application key used in a firmware image
     *                                 distribution used in a firmware image distribution.
     * @param ttl                      Time To Live (TTL) value used in a firmware image distribution.
     * @param distributionTransferMode Transfer mode used in a firmware image distribution.
     * @param updatePolicy             Update policy used in a firmware image distribution.
     * @param distributionTimeoutBase  The value that is used to calculate when firmware image
     *                                 distribution will be suspended. The Timeout is calculated
     *                                 using the following formula:
     *                                 Timeout = (10,000 × (Timeout Base + 2)) + (100 × Transfer TTL)
     *                                 milliseconds.
     */
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        firmwareImageIndex: UShort,
        labelUuid: Uuid,
        applicationKeyIndex: KeyIndex,
        ttl: UByte = 0xFF.toUByte(),
        distributionTransferMode: TransferMode = TransferMode.PUSH,
        updatePolicy: FirmwareUpdatePolicy = FirmwareUpdatePolicy.VERIFY_AND_APPLY,
        distributionTimeoutBase: UShort = 118u, // 20 minutes
    ) : this(
        firmwareImageIndex = firmwareImageIndex,
        multicastAddress = VirtualAddress(uuid = labelUuid),
        applicationKeyIndex = applicationKeyIndex,
        ttl = ttl,
        distributionTransferMode = distributionTransferMode,
        updatePolicy = updatePolicy,
        distributionTimeoutBase = distributionTimeoutBase
    )

    /**
     * Convenience constructor to create a FirmwareDistributionStart message.
     *
     * @param firmwareImageIndex       Index of the firmware image in the Firmware Images List state
     *                                 to use during firmware image distribution. The maximum
     *                                 supported Firmware Images List Size can be obtained using
     *                                 [FirmwareDistributionCapabilitiesGet] message.
     * @param fixedGroupAddress        Multicast Address used in a Firmware Image Distribution.
     * @param applicationKeyIndex      Index of the application key used in a firmware image
     *                                 distribution used in a firmware image distribution.
     * @param ttl                      Time To Live (TTL) value used in a firmware image distribution.
     * @param distributionTransferMode Transfer mode used in a firmware image distribution.
     * @param updatePolicy             Update policy used in a firmware image distribution.
     * @param distributionTimeoutBase  The value that is used to calculate when firmware image
     *                                 distribution will be suspended. The Timeout is calculated
     *                                 using the following formula:
     *                                 Timeout = (10,000 × (Timeout Base + 2)) + (100 × Transfer TTL)
     *                                 milliseconds.
     */
    constructor(
        firmwareImageIndex: UShort,
        fixedGroupAddress: FixedGroupAddress,
        applicationKeyIndex: KeyIndex,
        ttl: UByte = 0xFF.toUByte(),
        distributionTransferMode: TransferMode = TransferMode.PUSH,
        updatePolicy: FirmwareUpdatePolicy = FirmwareUpdatePolicy.VERIFY_AND_APPLY,
        distributionTimeoutBase: UShort = 118u, // 20 minutes
    ) : this(
        firmwareImageIndex = firmwareImageIndex,
        multicastAddress = fixedGroupAddress,
        applicationKeyIndex = applicationKeyIndex,
        ttl = ttl,
        distributionTransferMode = distributionTransferMode,
        updatePolicy = updatePolicy,
        distributionTimeoutBase = distributionTimeoutBase
    )

    /**
     * Convenience constructor to create a FirmwareDistributionStart message.
     *
     * @param firmwareImageIndex       Index of the firmware image in the Firmware Images List state
     *                                 to use during firmware image distribution. The maximum
     *                                 supported Firmware Images List Size can be obtained using
     *                                 [FirmwareDistributionCapabilitiesGet] message.
     * @param unassignedAddress        Multicast Address used in a Firmware Image Distribution.
     * @param applicationKeyIndex      Index of the application key used in a firmware image
     *                                 distribution used in a firmware image distribution.
     * @param ttl                      Time To Live (TTL) value used in a firmware image distribution.
     * @param distributionTransferMode Transfer mode used in a firmware image distribution.
     * @param updatePolicy             Update policy used in a firmware image distribution.
     * @param distributionTimeoutBase  The value that is used to calculate when firmware image
     *                                 distribution will be suspended. The Timeout is calculated
     *                                 using the following formula:
     *                                 Timeout = (10,000 × (Timeout Base + 2)) + (100 × Transfer TTL)
     *                                 milliseconds.
     */
    constructor(
        firmwareImageIndex: UShort,
        unassignedAddress: UnassignedAddress,
        applicationKeyIndex: KeyIndex,
        ttl: UByte = 0xFF.toUByte(),
        distributionTransferMode: TransferMode = TransferMode.PUSH,
        updatePolicy: FirmwareUpdatePolicy = FirmwareUpdatePolicy.VERIFY_AND_APPLY,
        distributionTimeoutBase: UShort = 118u, // 20 minutes
    ) : this(
        firmwareImageIndex = firmwareImageIndex,
        multicastAddress = unassignedAddress,
        applicationKeyIndex = applicationKeyIndex,
        ttl = ttl,
        distributionTransferMode = distributionTransferMode,
        updatePolicy = updatePolicy,
        distributionTimeoutBase = distributionTimeoutBase
    )

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x8319u

        @OptIn(ExperimentalUuidApi::class)
        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 10 || it.size == 24 }
            ?.let { params ->
                FirmwareDistributionStart(
                    applicationKeyIndex = params.getUShort(
                        offset = 0,
                        order = ByteOrder.LITTLE_ENDIAN
                    ),
                    ttl = params[2].toUByte(),
                    distributionTimeoutBase = params.getUShort(
                        offset = 3,
                        order = ByteOrder.LITTLE_ENDIAN
                    ),
                    distributionTransferMode = TransferMode.from(
                        value = (params[5] shr 6).toUByte()
                    ) ?: return@let null,
                    updatePolicy = FirmwareUpdatePolicy.from(
                        value = ((params[5] shr 5) and 0x01).toUByte()
                    ) ?: return@let null,
                    firmwareImageIndex = params.getUShort(
                        offset = 6,
                        order = ByteOrder.LITTLE_ENDIAN
                    ),
                    multicastAddress = when (params.size == 24) {
                        true -> VirtualAddress(Uuid.fromByteArray(params.sliceArray(indices = 8 until 24)))
                        false -> UnassignedAddress
                    }
                )
            }
    }
}