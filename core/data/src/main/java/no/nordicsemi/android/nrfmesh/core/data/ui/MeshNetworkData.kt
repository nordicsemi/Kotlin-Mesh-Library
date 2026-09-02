@file:Suppress("MemberVisibilityCanBePrivate", "unused", "PropertyName")

package no.nordicsemi.android.nrfmesh.core.data.ui

import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * MeshNetwork representing a Bluetooth mesh network.
 *
 * @property uuid                   128-bit Universally Unique Identifier (Uuid), which allows
 *                                  differentiation among multiple mesh networks.
 * @property name                   Human-readable name for the mesh network.
 * @property timestamp              Represents the last time the Mesh Object has been modified. The
 *                                  timestamp is based on Coordinated Universal Time.
 * @property partial                Indicates if this Mesh Configuration Database is part of a
 *                                  larger database.
 * @property networkKeys            List of network keys that includes information about network
 *                                  keys used in the mesh network.
 * @property applicationKeys        List of app keys that includes information about app keys used
 *                                  in the mesh network.
 * @property provisioners           List of known Provisioners and ranges of addresses that have
 *                                  been allocated to these Provisioners.
 * @property nodes                  List of nodes that includes information about mesh nodes in the
 *                                  mesh network.
 * @property groups                 List of groups that includes information about groups configured
 *                                  in the mesh network.
 * @property scenes                 List of scenes that includes information about scenes configured
 *                                  in the mesh network.
 * @property ivIndex                IV Index of the network received via the last Secure Network
 *                                  Beacon and its current state.
 * @constructor                     Creates a mesh network.
 */
@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
data class MeshNetworkData(
    val uuid: Uuid = Uuid.random(),
    val name: String,
    val provisioners: List<ProvisionerData> = mutableListOf(),
    val networkKeys: List<NetworkKeyData> = mutableListOf(),
    val applicationKeys: List<ApplicationKeyData> = mutableListOf(),
    val nodes: List<NodeData> = mutableListOf(),
    val groups: List<GroupData> = mutableListOf(),
    val scenes: List<SceneData> = mutableListOf(),
    val ivIndex: IvIndex,
    val timestamp: Instant,
    val partial: Boolean,
) {
    @OptIn(ExperimentalTime::class)
    constructor(network: MeshNetwork) : this(
        uuid = network.uuid,
        name = network.name,
        provisioners = network.provisioners.map { ProvisionerData(it) },
        networkKeys = network.networkKeys.map { NetworkKeyData(it) },
        applicationKeys = network.applicationKeys.map { ApplicationKeyData(it) },
        nodes = network.nodes.map { NodeData(it) },
        groups = network.groups.map { GroupData(it) },
        scenes = network.scenes.map { SceneData(it) },
        ivIndex = network.ivIndex,
        timestamp = network.timestamp,
        partial = network.partial
    )
}


