package no.nordicsemi.android.nrfmesh.feature.dfu

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Https
import androidx.compose.runtime.Composable


/**
 * Defines a set of options for firmware update.
 */
enum class FirmwareUpdateOptions {
    /** Firmware update using SMP*/
    SMP,

    /** Firmware update using a blob.*/
    BLOB,

    /** Firmware update using HTTPS.*/
    HTTPS;

    fun description() = when (this) {
        SMP -> "SMP"
        BLOB -> "BLOB"
        HTTPS -> "HTTPS"
    }
}

@Composable
fun FirmwareUpdateOptions.icon() = when (this) {
    FirmwareUpdateOptions.SMP -> Icons.Outlined.Email
    FirmwareUpdateOptions.BLOB -> Icons.Outlined.DataObject
    FirmwareUpdateOptions.HTTPS -> Icons.Outlined.Https
}