package no.nordicsemi.kotlin.mesh.core.exception

sealed class MeshNetworkException : Exception() {
    protected fun readResolve(): Any = this
}

/** Thrown when a given key index is out of range. A valid key index must range from 0 to 4095. */
class KeyIndexOutOfRange : MeshNetworkException()

/** Thrown when a given key index is already in use. */
class DuplicateKeyIndex : MeshNetworkException()

/** Thrown when the length of a key is not 16-bytes */
class InvalidKeyLength : MeshNetworkException()

/** Thrown when a given key is in use. */
class KeyInUse : MeshNetworkException()

/** Thrown when a network and a node does not contain any network key. */
class NoNetworkKeysAdded : MeshNetworkException()

/** Thrown when a Network property cannot be removed. */
class CannotRemove : MeshNetworkException()

/** Thrown when a node already exists. */
class NodeAlreadyExists : MeshNetworkException()

/** Thrown when no provisioner is available in the mesh network. */
class NoLocalProvisioner : MeshNetworkException()

/** Thrown when no unicast address range is allocated to a provisioner. */
class ProvisionerAlreadyExists : MeshNetworkException()

/** Thrown when any allocated range of a provisioner is already allocated. */
class RangeAlreadyAllocated : MeshNetworkException()

/** Thrown when any allocated range of a new provisioner overlaps with an existing one. */
class OverlappingProvisionerRanges : MeshNetworkException()

/** Thrown when a given address does not belong to an allocated range. */
class AddressNotInAllocatedRanges : MeshNetworkException()

/** Thrown when a given address is in use by a node or it's elements. */
class AddressAlreadyInUse : MeshNetworkException()

/** Thrown when no unicast addresses available to be allocated. */
class NoAddressesAvailable : MeshNetworkException()

/** Thrown when no unicast address range is allocated to a provisioner. */
class NoUnicastRangeAllocated : MeshNetworkException()

/** Thrown when a given group already exists. */
class GroupAlreadyExists : MeshNetworkException()

/** Thrown when a given group is in use. */
class GroupInUse : MeshNetworkException()

/** Thrown when no group range is allocated to a provisioner. */
class NoGroupRangeAllocated : MeshNetworkException()

/** Thrown when a given scene already exists. */
class SceneAlreadyExists : MeshNetworkException()

/** Thrown when a given scene is in use. */
class SceneInUse : MeshNetworkException()

/** Thrown when no scene range is allocated to a provisioner. */
class NoSceneRangeAllocated : MeshNetworkException()

/** Thrown when no scene number is available to be allocated. */
class NoSceneNumberAvailable : MeshNetworkException()

/** Thrown when at least one network key is not selected when exporting a partial network. */
class AtLeastOneNetworkKeyMustBeSelected : MeshNetworkException()

/** Thrown when at least one provisioner is not selected when exporting a partial network. */
class AtLeastOneProvisionerMustBeSelected : MeshNetworkException()

/** Thrown when an invalid pdu type is received*/
class InvalidPduType : MeshNetworkException()

/** Thrown when an invalid pdu is received*/
class InvalidPdu : MeshNetworkException()

/** Thrown when setting too small IV Index. The new IV Index must be greater than or equal to the
 * previous one. */
class IvIndexTooSmall : MeshNetworkException()

/**
 * Security exception thrown when level of security of the network key doesn't match with the
 * security used when provisioning a node.
 */
class SecurityException : MeshNetworkException()

/**
 * Thrown when the Json deserializing encounters an error.
 *
 * @property error            Error message.
 * @property throwable        Throwable exception.
 */
@ConsistentCopyVisibility
data class ImportError internal constructor(
    val error: String,
    val throwable: Throwable,
) : MeshNetworkException()

/** Thrown when a network property does not belong to the current network. */
class DoesNotBelongToNetwork : MeshNetworkException()

/** Thrown when no network is initialized **/
class NoNetwork : MeshNetworkException()