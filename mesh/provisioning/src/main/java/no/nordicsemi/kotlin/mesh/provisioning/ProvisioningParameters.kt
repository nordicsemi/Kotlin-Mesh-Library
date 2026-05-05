package no.nordicsemi.kotlin.mesh.provisioning

import no.nordicsemi.kotlin.mesh.core.exception.NoLocalProvisioner
import no.nordicsemi.kotlin.mesh.core.exception.NoNetworkKeysAdded
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.crypto.Algorithm
import no.nordicsemi.kotlin.mesh.crypto.Algorithm.Companion.strongest


/**
 * Configuration class that contains all the necessary information to provision a device.
 *
 * @property unicastAddress    Unicast address to be assigned to the device.
 * @property networkKey        Network key to be used for provisioning.
 * @property algorithm         Algorithm to be used for provisioning.
 * @property publicKey         Public key to be used for provisioning.
 * @property authMethod        Authentication method to be used for provisioning.
 * @throws NoNetworkKeysAdded  Exception thrown when there are no network keys added to the mesh
 *                             network.
 * @throws NoLocalProvisioner  Exception thrown when there is no local provisioner added to the mesh
 *                             network.
 * @throws NoAddressAvailable  Exception thrown when there is no available unicast address.
 */
data class ProvisioningParameters(
    val unicastAddress: UnicastAddress,
    val networkKey: NetworkKey,
    val algorithm: Algorithm,
    val publicKey: PublicKey,
    val authMethod: AuthenticationMethod,
) {
    companion object {
        /**
         * Creates a default [ProvisioningParameters] based on the provided [ProvisioningCapabilities]
         * for a given network and provisioner.
         */
        internal fun defaultFrom(
            capabilities: ProvisioningCapabilities,
            meshNetwork: MeshNetwork,
        ) = ProvisioningParameters(
            unicastAddress = meshNetwork.nextAvailableUnicastAddress(
                elementCount = capabilities.numberOfElements,
                provisioner = meshNetwork.localProvisioner ?: throw NoLocalProvisioner()
            ) ?: throw NoAddressAvailable(),
            networkKey = meshNetwork.networkKeys.firstOrNull() ?: throw NoNetworkKeysAdded(),
            algorithm = capabilities.algorithms.strongest(),
            publicKey = PublicKey.NoOobPublicKey,
            authMethod = capabilities.supportedAuthMethods.first()
        )
    }
}