package no.nordicsemi.android.nrfmesh.feature.dfu

/**
 * The steps of the firmware update, in the order they are shown.
 * */
internal enum class FirmwareUpdateStep {
    DistributorSelection,
    LePairing,
    FirmwareSelection,
    DistributionParameters,
    NodeConfiguration,
    Distribution;

    companion object {
        /** The last step, which shows the distribution. */
        val last = entries.last()
    }
}
