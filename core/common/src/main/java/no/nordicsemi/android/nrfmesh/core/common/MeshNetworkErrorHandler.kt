package no.nordicsemi.android.nrfmesh.core.common

import android.content.res.Resources
import no.nordicsemi.kotlin.mesh.core.exception.AddressAlreadyInUse
import no.nordicsemi.kotlin.mesh.core.exception.AddressNotInAllocatedRanges
import no.nordicsemi.kotlin.mesh.core.exception.AtLeastOneNetworkKeyMustBeSelected
import no.nordicsemi.kotlin.mesh.core.exception.AtLeastOneProvisionerMustBeSelected
import no.nordicsemi.kotlin.mesh.core.exception.CannotRemove
import no.nordicsemi.kotlin.mesh.core.exception.DoesNotBelongToNetwork
import no.nordicsemi.kotlin.mesh.core.exception.DuplicateKeyIndex
import no.nordicsemi.kotlin.mesh.core.exception.GroupAlreadyExists
import no.nordicsemi.kotlin.mesh.core.exception.GroupInUse
import no.nordicsemi.kotlin.mesh.core.exception.ImportError
import no.nordicsemi.kotlin.mesh.core.exception.InvalidKeyLength
import no.nordicsemi.kotlin.mesh.core.exception.KeyInUse
import no.nordicsemi.kotlin.mesh.core.exception.KeyIndexOutOfRange
import no.nordicsemi.kotlin.mesh.core.exception.NoAddressesAvailable
import no.nordicsemi.kotlin.mesh.core.exception.NoGroupRangeAllocated
import no.nordicsemi.kotlin.mesh.core.exception.NoNetworkKeysAdded
import no.nordicsemi.kotlin.mesh.core.exception.NoSceneRangeAllocated
import no.nordicsemi.kotlin.mesh.core.exception.NoUnicastRangeAllocated
import no.nordicsemi.kotlin.mesh.core.exception.NodeAlreadyExists
import no.nordicsemi.kotlin.mesh.core.exception.OverlappingProvisionerRanges
import no.nordicsemi.kotlin.mesh.core.exception.ProvisionerAlreadyExists
import no.nordicsemi.kotlin.mesh.core.exception.SceneAlreadyExists
import no.nordicsemi.kotlin.mesh.core.exception.SceneInUse

// TODO needs a better name, toString was getting overshadowed.
fun Throwable.convertToString(resources: Resources) = when (this) {
    is KeyIndexOutOfRange -> resources.getString(R.string.key_index_out_of_range)
    is DuplicateKeyIndex -> resources.getString(R.string.duplicate_key_index)
    is InvalidKeyLength -> resources.getString(R.string.invalid_key_length)
    is KeyInUse -> resources.getString(R.string.key_in_use)
    is NoNetworkKeysAdded -> resources.getString(R.string.no_network_keys_added)
    is CannotRemove -> resources.getString(R.string.cannot_remove)
    is NodeAlreadyExists -> resources.getString(R.string.node_already_exists)
    is ProvisionerAlreadyExists -> resources.getString(R.string.provisioner_already_exists)
    is OverlappingProvisionerRanges -> resources.getString(R.string.overlapping_provisioner_ranges)
    is AddressNotInAllocatedRanges -> resources.getString(R.string.address_not_in_allocated_ranges)
    is AddressAlreadyInUse -> resources.getString(R.string.address_already_in_use)
    is NoAddressesAvailable -> resources.getString(R.string.no_addresses_available)
    is NoUnicastRangeAllocated -> resources.getString(R.string.no_unicast_range_allocated)
    is GroupAlreadyExists -> resources.getString(R.string.group_already_exists)
    is GroupInUse -> resources.getString(R.string.group_in_use)
    is NoGroupRangeAllocated -> resources.getString(R.string.no_group_range_allocated)
    is SceneAlreadyExists -> resources.getString(R.string.scene_already_exists)
    is SceneInUse -> resources.getString(R.string.scene_in_use)
    is NoSceneRangeAllocated -> resources.getString(R.string.no_scene_range_allocated)
    is AtLeastOneNetworkKeyMustBeSelected -> resources.getString(
        R.string.at_least_one_net_key_must_be_selected
    )
    is AtLeastOneProvisionerMustBeSelected -> resources.getString(
        R.string.at_least_one_provisioner_must_be_selected
    )
    is ImportError -> error
    is DoesNotBelongToNetwork -> resources.getString(R.string.does_not_belong_to_network)
    else -> localizedMessage ?: resources.getString(R.string.unknown_error)
}