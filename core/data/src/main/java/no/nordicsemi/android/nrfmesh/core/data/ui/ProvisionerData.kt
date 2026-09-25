package no.nordicsemi.android.nrfmesh.core.data.ui

import no.nordicsemi.android.nrfmesh.core.common.KeyIdGenerator
import no.nordicsemi.kotlin.mesh.core.model.GroupRange
import no.nordicsemi.kotlin.mesh.core.model.Provisioner
import no.nordicsemi.kotlin.mesh.core.model.SceneRange
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastRange
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ProvisionerData is a data class that represents a Provisioner in the Mesh network.
 *
 * @param name                         Name of the provisioner.
 * @param uuid                         UUID of the provisioner.
 * @param address                      Unicast address of the provisioner.
 * @param ttl                          Default TTL value for the provisioner.
 * @param deviceKey                    Device key of the provisioner in hexadecimal string format.
 * @param unicastRanges                List of unicast address ranges allocated to the provisioner.
 * @param groupRanges                  List of group address ranges allocated to the provisioner.
 * @param sceneRanges                  List of scene ranges allocated to the provisioner.
 * @param hasConfigurationCapabilities Indicates if the provisioner has configuration capabilities.
 * @param id                           A unique identifier for the ProvisionerData instance.
 *
 */
@OptIn(ExperimentalUuidApi::class)
data class ProvisionerData(
    val name: String,
    val uuid: Uuid,
    val address: UnicastAddress?,
    val ttl: Int,
    val deviceKey: String? = null,
    val unicastRanges: List<UnicastRange> = emptyList(),
    val groupRanges: List<GroupRange> = emptyList(),
    val sceneRanges: List<SceneRange> = emptyList(),
    val hasConfigurationCapabilities: Boolean = false,
    val id: Long = KeyIdGenerator.nextId()
) {
    @OptIn(ExperimentalStdlibApi::class)
    constructor(provisioner: Provisioner) : this(
        name = provisioner.name,
        uuid = provisioner.uuid,
        address = provisioner.node?.primaryUnicastAddress,
        ttl = provisioner.node?.defaultTTL?.toInt() ?: 0,
        deviceKey = provisioner.node?.deviceKey?.toHexString()?.uppercase(),
        unicastRanges = provisioner.allocatedUnicastRanges.toList(),
        groupRanges = provisioner.allocatedGroupRanges.toList(),
        sceneRanges = provisioner.allocatedSceneRanges.toList(),
        hasConfigurationCapabilities = provisioner.hasConfigurationCapabilities
    )
}