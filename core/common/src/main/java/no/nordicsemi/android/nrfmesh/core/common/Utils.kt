@file:Suppress("unused")

package no.nordicsemi.android.nrfmesh.core.common

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
import no.nordicsemi.kotlin.mesh.core.exception.InvalidPduType
import no.nordicsemi.kotlin.mesh.core.exception.KeyInUse
import no.nordicsemi.kotlin.mesh.core.exception.KeyIndexOutOfRange
import no.nordicsemi.kotlin.mesh.core.exception.MeshNetworkException
import no.nordicsemi.kotlin.mesh.core.exception.NoAddressesAvailable
import no.nordicsemi.kotlin.mesh.core.exception.NoGroupRangeAllocated
import no.nordicsemi.kotlin.mesh.core.exception.NoLocalProvisioner
import no.nordicsemi.kotlin.mesh.core.exception.NoNetwork
import no.nordicsemi.kotlin.mesh.core.exception.NoNetworkKeysAdded
import no.nordicsemi.kotlin.mesh.core.exception.NoSceneRangeAllocated
import no.nordicsemi.kotlin.mesh.core.exception.NoUnicastRangeAllocated
import no.nordicsemi.kotlin.mesh.core.exception.NodeAlreadyExists
import no.nordicsemi.kotlin.mesh.core.exception.OverlappingProvisionerRanges
import no.nordicsemi.kotlin.mesh.core.exception.ProvisionerAlreadyExists
import no.nordicsemi.kotlin.mesh.core.exception.RangeAlreadyAllocated
import no.nordicsemi.kotlin.mesh.core.exception.SceneAlreadyExists
import no.nordicsemi.kotlin.mesh.core.exception.SceneInUse
import no.nordicsemi.kotlin.mesh.core.exception.SecurityException
import no.nordicsemi.kotlin.mesh.core.layers.access.AccessError
import no.nordicsemi.kotlin.mesh.logger.LogLevel
import no.nordicsemi.kotlin.mesh.provisioning.AddressNotSpecified
import no.nordicsemi.kotlin.mesh.provisioning.ConfirmationFailed
import no.nordicsemi.kotlin.mesh.provisioning.InvalidAddress
import no.nordicsemi.kotlin.mesh.provisioning.InvalidConfirmation
import no.nordicsemi.kotlin.mesh.provisioning.InvalidOobValueFormat
import no.nordicsemi.kotlin.mesh.provisioning.InvalidPublicKey
import no.nordicsemi.kotlin.mesh.provisioning.InvalidState
import no.nordicsemi.kotlin.mesh.provisioning.KeyGenerationFailed
import no.nordicsemi.kotlin.mesh.provisioning.NetworkKeyNotSpecified
import no.nordicsemi.kotlin.mesh.provisioning.NoAddressAvailable
import no.nordicsemi.kotlin.mesh.provisioning.ProvisioningError
import no.nordicsemi.kotlin.mesh.provisioning.RemoteError
import no.nordicsemi.kotlin.mesh.provisioning.UnsupportedDevice
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

/**
 * Helper object containing utility methods.
 */
object Utils {

    /**
     * Converts the [LogLevel] to an Android log level.
     *
     * @receiver LogLevel
     * @return the Android log level
     */
    fun LogLevel.toAndroidLogLevel(): Int = when (this) {
        LogLevel.VERBOSE -> android.util.Log.VERBOSE
        LogLevel.DEBUG -> android.util.Log.DEBUG
        LogLevel.INFO -> android.util.Log.INFO
        LogLevel.APPLICATION -> android.util.Log.INFO
        LogLevel.WARNING -> android.util.Log.WARN
        LogLevel.ERROR -> android.util.Log.ERROR
    }

    fun Throwable.describe(): String {
        return when (this) {
            is ProvisioningError -> when (this) {
                is AddressNotSpecified -> "Address not specified."
                is ConfirmationFailed -> "Confirmation Failed."
                is InvalidAddress -> "Invalid Address."
                is InvalidConfirmation -> "Invalid Confirmation."
                is InvalidOobValueFormat -> "Invalid OOB Value Format."
                is no.nordicsemi.kotlin.mesh.provisioning.InvalidPdu -> "Invalid PDU."
                is InvalidPublicKey -> "Invalid Public Key."
                is InvalidState -> "Invalid State."
                is KeyGenerationFailed -> "Key Generation Failed."
                is NetworkKeyNotSpecified -> "Network Key Not Specified."
                is NoAddressAvailable -> "No Address Available."
                is RemoteError -> "Remote Error."
                is UnsupportedDevice -> "Unsupported Device."
            }
            is AccessError -> this.toString()

            is MeshNetworkException -> when (this) {
                is AddressAlreadyInUse -> "Address already in use."
                is AddressNotInAllocatedRanges -> "Address not in allocated ranges."
                is AtLeastOneNetworkKeyMustBeSelected -> "At least one network key must be selected."
                is AtLeastOneProvisionerMustBeSelected -> "At least one provisioner must be selected."
                is CannotRemove -> "Cannot remove."
                is DoesNotBelongToNetwork -> "Does not belong to network."
                is DuplicateKeyIndex -> "Duplicate key index."
                is GroupAlreadyExists -> "Group already exists."
                is GroupInUse -> "Group in use."
                is ImportError -> "Import error: $error"
                is InvalidKeyLength -> "Invalid key length."
                is no.nordicsemi.kotlin.mesh.core.exception.InvalidPdu -> "Invalid PDU."
                is InvalidPduType -> "Invalid PDU type."
                is KeyInUse -> "Key in use."
                is KeyIndexOutOfRange -> "Key index out of range."
                is NoAddressesAvailable -> "Addresses not available."
                is NoGroupRangeAllocated -> "Group range not allocated."
                is NoLocalProvisioner -> "No local provisioner."
                is NoNetwork -> "No network."
                is NoNetworkKeysAdded -> "No network keys added."
                is NoSceneRangeAllocated -> "Scene range not allocated."
                is NoUnicastRangeAllocated -> "Unicast range not allocated."
                is NodeAlreadyExists -> "Node already exists."
                is OverlappingProvisionerRanges -> "Overlapping provisioner ranges."
                is ProvisionerAlreadyExists -> "Provisioner already exists."
                is RangeAlreadyAllocated -> "Range already allocated."
                is SceneAlreadyExists -> "Scene already exists."
                is SceneInUse -> "Scene in use."
                is SecurityException -> "Security exception."
                else -> "Unknown error: ${this.message}"
            }

            else -> "Unknown error: ${this.message}"
        }
    }

}

/**
 * Copies the provided text to the clipboard.
 *
 * @param scope   CoroutineScope] to launch the clipboard operation in
 * @param text    Text to copy
 * @param label   Label for the clipboard entry
 */
fun copyToClipboard(
    scope: CoroutineScope,
    clipboard: Clipboard,
    text: String,
    label: String,
) {
    scope.launch {
        val clip = ClipData.newPlainText(/* label = */ label, /* text = */ text)
        clipboard.setClipEntry(clipEntry = ClipEntry(clipData = clip))
    }
}

object KeyIdGenerator {
    @OptIn(ExperimentalAtomicApi::class)
    private val counter = AtomicLong(0)

    /**
     * Generates the next unique ID. This is a helper method used to generate unique IDs for keys in
     * a LazyColumn
     */
    @OptIn(ExperimentalAtomicApi::class)
    fun nextId() = counter.incrementAndFetch()
}