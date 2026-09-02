package no.nordicsemi.android.nrfmesh.feature.dfu.util

import no.nordicsemi.android.nrfmesh.core.common.UpdatedFirmwareInformation
import no.nordicsemi.android.nrfmesh.core.common.firmwareDistributionServer
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareInformation
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdateAdditionalInformation
import no.nordicsemi.kotlin.mesh.core.model.Node

/**
 * Target for the firmware update.
 *
 * @property node            Node to update.
 * @property entries         Firmware entries.
 * @property isSelected      Whether the target is selected.
 */
internal data class Target(val node: Node, var entries: FirmwareEntries) {
    val isSelected: Boolean
        get() = (entries as? FirmwareEntries.Ready)?.entries?.any { it.isSelected } == true

    val selectedReceiver: Receiver?
        get() {
            val address = node.model(modelId = firmwareDistributionServer)
                ?.parentElement
                ?.unicastAddress
                ?: return null
            val ready = entries as? FirmwareEntries.Ready ?: return null
            val selectedIndex = ready.entries.firstOrNull { it.isSelected }?.index ?: return null
            return Receiver(address = address, imageIndex = selectedIndex)
        }
}

internal sealed interface FirmwareEntries {

    /** Model needs configuration. Application Key is not bound to the Model. */
    data object ConfigurationRequired : FirmwareEntries

    /** Firmware Image entries can be downloaded. */
    data object Configured : FirmwareEntries

    /** The app is downloading Firmware Image entries and checks available updates. */
    data object Downloading : FirmwareEntries

    /** The Firmware Image entries are ready to be displayed. */
    data class Ready(val entries: List<FirmwareEntry>) : FirmwareEntries

    /** Operation resulted with an error. */
    data class Error(val message: String) : FirmwareEntries

    val count: Int
        get() = when (this) {
            is Ready -> entries.size
            is Error -> 1
            else -> 0
        }

    operator fun get(index: Int): FirmwareEntry? =
        (this as? Ready)?.entries?.getOrNull(index)

    /** Replacement for the Swift subscript setter: returns a new value instead of mutating. */
    fun with(index: Int, entry: FirmwareEntry?): FirmwareEntries {
        val ready = this as? Ready ?: return this
        if (index !in ready.entries.indices) return this
        return Ready(
            ready.entries.toMutableList().apply {
                if (entry != null) this[index] = entry else removeAt(index)
            }
        )
    }
}

/**
 * Firmware entry.
 *
 * @property index           Index of the entry.
 * @property firmware        Firmware information.
 * @property status          Status of the entry.
 * @property availableUpdate Available update for the entry.
 */
internal data class FirmwareEntry(
    val index: UByte,
    val firmware: FirmwareInformation,
    val status: Status = Status.Unselected,
    val availableUpdate: UpdatedFirmwareInformation? = null,
) {
    val isSelected: Boolean
        get() = status is Status.Selected
}

/**
 * Status of the firmware entry.
 */
internal sealed interface Status {
    /** Firmware entry is not selected. */
    data object Unselected : Status
    /** Firmware entry is selected. */
    data object CheckingMetadata : Status
    /** Firmware entry is selected. */
    data class Selected(val additionalInformation: FirmwareUpdateAdditionalInformation) : Status
    /** Firmware entry is error. */
    data class Error(val message: String) : Status
}