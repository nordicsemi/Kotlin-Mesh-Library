package no.nordicsemi.android.nrfmesh.feature.dfu.util

import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdatePolicy
import no.nordicsemi.kotlin.mesh.core.messages.TransferMode
import no.nordicsemi.kotlin.mesh.core.model.DistributionMulticastAddress
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress

/**
 * Firmware update parameters.
 *
 * @property ttl          Time to live.
 * @property timeoutBase  Base timeout.
 * @property mode Transfer mode.
 * @property policy Update policy.
 * @property address      Address.
 */
data class DfuParameters(
    val ttl: UByte = 7u,
    val timeoutBase: UShort = 0u,
    val mode: TransferMode = TransferMode.PUSH,
    val policy: FirmwareUpdatePolicy = FirmwareUpdatePolicy.VERIFY_AND_APPLY,
    val address: DistributionMulticastAddress = UnassignedAddress,
)