package no.nordicsemi.android.nrfmesh.feature.dfu.util

import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress

/** Represents a receiver in the firmware update process. */
data class Receiver(
    /** The Unicast Address of the Element with the Firmware Update Server model on the Receiver. */
    val address: UnicastAddress,
    /** The index of the image being updated. */
    val imageIndex: UByte,
    /** The status of the receiver. */
    val status: Status = Status.Idle,
) {
    /** The status of the firmware update process. */
    sealed interface Status {

        /** The receiver is idle and not currently updating. */
        data object Idle : Status

        /** The receiver is currently distributing the firmware update. */
        data class Distribution(override val progress: Int, val speedBytesPerSecond: Float) : Status

        /** The receiver is in `verificationSucceeded` state. */
        data object Verified : Status

        /** The receiver is in `applyingUpdate` or `applySuccess` state. */
        data object Applied : Status

        /** The receiver is in `verificationFailed`, `applyFailed` or `transferCanceled` state. */
        data object Failure : Status

        val progress: Int
            get() = when (this) {
                is Idle -> -1
                is Distribution -> progress
                is Verified -> 100
                is Applied -> 100
                is Failure -> 0
            }
    }
}