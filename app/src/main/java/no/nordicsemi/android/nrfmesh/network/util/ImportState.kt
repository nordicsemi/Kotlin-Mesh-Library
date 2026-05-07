package no.nordicsemi.android.nrfmesh.network.util

/**
 * Defines the state of the import process.
 */
internal sealed class ImportState {
    /**
     * Unknown state which is by default
     */
    data object Unknown : ImportState()

    /**
     * Defines that a network is being imported
     */
    data object Importing : ImportState()

    /**
     * Defines that the import has completed. If the import was successful, the error will be null.
     *
     * @param error The error that occurred during the import.
     */
    data class Completed(val error: Error? = null) : ImportState()
}