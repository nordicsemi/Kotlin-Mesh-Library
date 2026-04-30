package no.nordicsemi.android.nrfmesh.feature.settings

import no.nordicsemi.android.nrfmesh.core.common.KeyIdGenerator
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Defines a data object that is used to display the ui state of the Settings List.
 *
 * @param name             Name of the network.
 * @param provisioners     Provisioners in the network.
 * @param networkKeys      Network keys in the network.
 * @param appKeys          Application keys in the network.
 * @param scenes           Scenes in the network.
 * @param timestamp        Timestamp when the network was last modified.
 */

@OptIn(ExperimentalTime::class)
data class SettingsListData(
    val name: String,
    val provisioners: Int,
    val networkKeys: Int,
    val appKeys: Int,
    val scenes: Int,
    val timestamp: Instant,
) {
    /**
     * Constructs a [SettingsListData] object from the given [MeshNetwork].
     */
    constructor(network: MeshNetwork) : this(
        name = network.name,
        provisioners = network.provisioners.size,
        networkKeys = network.networkKeys.size,
        appKeys = network.applicationKeys.size,
        scenes = network.scenes.size,
        timestamp = network.timestamp
    )
}
