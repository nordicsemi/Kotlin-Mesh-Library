package no.nordicsemi.android.nrfmesh.feature.quickconfiguration

/** The pages of the Quick Configuration screen. */
internal enum class QuickConfigurationStep {
    Selection,
    Models,
    Configuration;

    companion object {
        /** The last step, which executes the tasks. */
        val last = entries.last()
    }
}
