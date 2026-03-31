@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.data.getUInt
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import java.net.URL
import java.nio.ByteOrder

/**
 * The Firmware ID state identifies a firmware image on the Node or on any subsystem
 * within the Node.
 *
 * The Firmware ID consists of a Company Identifier and an optional vendor-specific version identifier
 * and is used to identify the firmware image on a Node.
 *
 * The Firmware ID is used by the Firmware Distribution Server to query new firmware image
 * based on the current Firmware ID. If should identify the device type and firmware version.
 * - seeAlso: For Zephyr and nRF Connect SDK implementation see
 * [Firmware images documentation](https://docs.nordicsemi.com/bundle/ncs-latest/page/zephyr/connectivity/bluetooth/api/mesh/dfu.html#firmware_images).
 *
 * @property companyIdentifier The 16-bit Company Identifier (CID) assigned by the Bluetooth SIG.
 *                             Company Identifiers are published in [Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/).
 *
 * @property version           Vendor-specific information describing the firmware binary package.
 *                             The version information shall be 0-106 bytes long.
 *                             Use [versionString] to get a human-readable version string if the
 *                             version is following Zephyr build versioning scheme
 *                             (`UInt8, UInt8, UInt16, UInt32`).
 * @property bytes             Returns the Firmware ID as a byte array. This array can be used to
 *                             check and obtain updated firmware images using HTTPS.
 *
 *
 */
data class FirmwareId(val companyIdentifier: UShort, val version: ByteArray = byteArrayOf()) {
    /**
     * Returns the Firmware ID as a byte array. This array can be used to check and obtain updated
     * firmware images using HTTPS.
     */
    val bytes: ByteArray
        get() = companyIdentifier.toByteArray(order = ByteOrder.LITTLE_ENDIAN) + version

    constructor(companyIdentifier: UShort) : this(companyIdentifier, byteArrayOf())

    constructor(data: ByteArray) : this(
        companyIdentifier = if (data.size >= 2)
            ((data[1].toInt() shl 8) or (data[0].toInt() and 0xFF)).toUShort()
        else 0u,
        version = if (data.size > 2) data.copyOfRange(2, data.size) else byteArrayOf()
    )

    /**
     * Returns the version string in the format `major.minor.revision+build`, skipping the build
     * number if it is 0.
     *
     * If the [version] is 1, 2, 4 or 8 bytes long it is interpreted as: (UInt8, UInt8, UInt16, UInt32).
     *
     * If [version] is empty, `null` is returned. If the number of bytes is
     * different from 1, 2, 4 or 8, the `version` is returned as a hex string with "0x" prefix.
     */
    val versionString: String?
        get() {
            if (version.isEmpty()) return null
            var major: UByte = 0u
            var minor: UByte = 0u
            var revision: UShort = 0u
            var build: UInt = 0u
            when (version.size) {
                8 -> build = version.getUInt(offset = 4, order = ByteOrder.LITTLE_ENDIAN)
                4 -> revision = version.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN)
            }
            if (version.size >= 2) {
                minor = version[1].toUByte()
            }
            major = version[0].toUByte()
            return if (build == 0u) "$major.$minor.$revision" else "$major.$minor.$revision+$build"
        }

    val debugDescription: String
        get() = "FirmwareId(companyId: 0x${
            companyIdentifier.toString(16)
        }, version: 0x${version.toHexString(HexFormat.UpperCase)}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FirmwareId

        if (companyIdentifier != other.companyIdentifier) return false
        if (!version.contentEquals(other.version)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = companyIdentifier.hashCode()
        result = 31 * result + version.contentHashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + (versionString?.hashCode() ?: 0)
        result = 31 * result + debugDescription.hashCode()
        return result
    }
}

/**
 * The Firmware Information entry identifies the information for a firmware subsystem on the Node
 * from the Firmware Information List state.
 *
 * @property currentFirmwareId The Firmware ID of the current firmware image on the Node.
 * @property updateUri         URI used to retrieve a new firmware image (optional).
 */
data class FirmwareInformation(val currentFirmwareId: FirmwareId, val updateUri: URL?) {
    val debugDescription: String
        get() {
            val companyId = "0x${currentFirmwareId.companyIdentifier.toString(16)}"
            val versionStr = currentFirmwareId.versionString
                ?: (if (currentFirmwareId.version.isEmpty()) "nil" else "0x${
                    currentFirmwareId.version.joinToString(
                        ""
                    ) { "%02X".format(it) }
                }")
            return "FirmwareInformation(companyId: $companyId, version: $versionStr, updateUri: ${updateUri?.toString() ?: "nil"})"
        }
}

/**
 * The status codes for the Firmware Update Server model and the Firmware Update Client model.
 *
 * @property value            The status value.
 * @property debugDescription A human-readable description of the status code.
 */
enum class FirmwareUpdateMessageStatus(val value: UByte) {
    /**
     * The message was successfully processed.
     */
    SUCCESS(0x00u),

    /**
     * Insufficient resources on the Node.
     */
    INSUFFICIENT_RESOURCES(0x01u),

    /**
     * The operation cannot be performed while the server is in the current phase.
     */
    WRONG_PHASE(0x02u),

    /**
     * An internal error occurred on the Node.
     */
    INTERNAL_ERROR(0x03u),

    /**
     * The message contains a firmware index value that is not expected.
     */
    WRONG_FIRMWARE_INDEX(0x04u),

    /**
     * The metadata check failed.
     */
    METADATA_CHECK_FAILED(0x05u),

    /**
     * The server cannot start a firmware update.
     */
    TEMPORARILY_UNAVAILABLE(0x06u),

    /**
     * Another BLOB transfer is in progress.
     */
    BLOB_TRANSFER_BUSY(0x07u);

    val debugDescription: String
        get() = when (this) {
            SUCCESS -> "Success"
            INSUFFICIENT_RESOURCES -> "Insufficient Resources"
            WRONG_PHASE -> "Wrong Phase"
            INTERNAL_ERROR -> "Internal Error"
            WRONG_FIRMWARE_INDEX -> "Wrong Firmware Index"
            METADATA_CHECK_FAILED -> "Metadata Check Failed"
            TEMPORARILY_UNAVAILABLE -> "Temporarily Unavailable"
            BLOB_TRANSFER_BUSY -> "BLOB Transfer Busy"
        }
}

/**
 * The status codes for the Firmware Distribution Server model and the Firmware Distribution Client model.
 *
 * @property value            The status value.
 * @property debugDescription A human-readable description of the status code.
 */
enum class FirmwareDistributionMessageStatus(val value: UByte) {

    /**
     * The message was successfully processed.
     */
    SUCCESS(0x00u),

    /**
     * Insufficient resources on the Node.
     */
    INSUFFICIENT_RESOURCES(0x01u),

    /**
     * The operation cannot be performed while the server is in the current phase.
     */
    WRONG_PHASE(0x02u),

    /**
     * An internal error occurred on the Node.
     */
    INTERNAL_ERROR(0x03u),

    /**
     * The requested firmware image is not stored on the Distributor.
     */
    FIRMWARE_NOT_FOUND(0x04u),

    /**
     * The AppKey identified by the AppKey Index is not known to the Node.
     */
    INVALID_APP_KEY_INDEX(0x05u),

    /**
     * There are no Target nodes in the Distribution Receivers List state.
     */
    RECEIVERS_LIST_EMPTY(0x06u),

    /**
     * Another firmware image distribution is in progress.
     */
    BUSY_WITH_DISTRIBUTION(0x07u),

    /**
     * Another firmware image upload is in progress.
     */
    BUSY_WITH_UPLOAD(0x08u),

    /**
     * The URI scheme name indicated by the Update URI is no supported.
     */
    URI_NOT_SUPPORTED(0x09u),

    /**
     * The format of the Update URI is invalid.
     */
    URI_MALFORMED(0x0Au),

    /**
     * The URI is unreachable.
     */
    URI_UNREACHABLE(0x0Bu),

    /**
     * The Check Firmware OOB procedure did not find any new firmware.
     */
    NEW_FIRMWARE_NOT_AVAILABLE(0x0Cu),

    /**
     * The suspension of the Distribute Firmware procedure failed.
     */
    SUSPEND_FAILED(0x0Du);

    val debugDescription: String
        get() = when (this) {
            SUCCESS -> "Success"
            INSUFFICIENT_RESOURCES -> "Insufficient Resources"
            WRONG_PHASE -> "Wrong Phase"
            INTERNAL_ERROR -> "Internal Error"
            FIRMWARE_NOT_FOUND -> "Firmware Not Found"
            INVALID_APP_KEY_INDEX -> "Invalid AppKey Index"
            RECEIVERS_LIST_EMPTY -> "Receivers List Empty"
            BUSY_WITH_DISTRIBUTION -> "Busy With Distribution"
            BUSY_WITH_UPLOAD -> "Busy With Upload"
            URI_NOT_SUPPORTED -> "URI Not Supported"
            URI_MALFORMED -> "URI Malformed"
            URI_UNREACHABLE -> "URI Unreachable"
            NEW_FIRMWARE_NOT_AVAILABLE -> "New Firmware Not Available"
            SUSPEND_FAILED -> "Suspend Failed"
        }

    internal companion object {

        /**
         * Returns the [FirmwareDistributionMessageStatus] for the given [value].
         *
         * @param value The status value.
         * @return The [FirmwareDistributionMessageStatus] for the given [value].
         */
        fun from(value: UByte) = entries.firstOrNull { it.value == value }
    }
}

/**
 * The Update Phase state identifies the firmware update phase of the
 * Firmware Update Server.
 */
enum class FirmwareUpdatePhase(val value: UByte) {

    /**
     * Ready to start a Receive Firmware procedure.
     */
    IDLE(0x0u),

    /**
     * The Transfer BLOB procedure failed.
     */
    TRANSFER_ERROR(0x1u),

    /**
     * The "Receive Firmware" procedure is being executed.
     */
    TRANSFER_ACTIVE(0x2u),

    /** The Verify Firmware procedure is being executed. */
    VERIFYING_UPDATE(0x3u),

    /** The Verify Firmware procedure completed successfully. */
    VERIFICATION_SUCCEEDED(0x4u),

    /** The Verify Firmware procedure failed. */
    VERIFICATION_FAILED(0x5u),

    /** The Apply New Firmware procedure is being executed. */
    APPLYING_UPDATE(0x6u);

    /** Indicates whether the firmware update can be canceled.
     *
     * Send [FirmwareUpdateCancel] message to cancel the firmware update. Cancelling update deletes
     * any stored information about the update on a Firmware Update Server.
     */
    val isCancellable: Boolean
        get() = true // Firmware Update can be canceled in any state

    /** Indicates whether the firmware update can be started. */
    val canStart: Boolean
        get() = this == IDLE || this == TRANSFER_ERROR || this == VERIFICATION_FAILED

    /** Indicates whether the firmware update can be applied. */
    val canApply: Boolean
        get() = this == VERIFICATION_SUCCEEDED || this == APPLYING_UPDATE
}

/**
 * The Retrieved Update Phase field identifies the phase of the firmware update on the Firmware
 * Update Server.
 *
 * The value of the Retrieved Update Phase field is either the retrieved value of the Update Phase s
 * tate or a value set by the client.
 */
enum class RetrievedUpdatePhase(internal val value: UByte) {
    IDLE(0x0u),
    TRANSFER_ERROR(0x1u),
    TRANSFER_ACTIVE(0x2u),
    VERIFYING_UPDATE(0x3u),
    VERIFICATION_SUCCEEDED(0x4u),
    VERIFICATION_FAILED(0x5u),
    APPLYING_UPDATE(0x6u),
    TRANSFER_CANCELED(0x7u),
    APPLY_SUCCESS(0x8u),
    APPLY_FAILED(0x9u),
    UNKNOWN(0xAu);

    val debugDescription: String
        get() = when (this) {
            IDLE -> "Idle"
            TRANSFER_ERROR -> "Transfer Error"
            TRANSFER_ACTIVE -> "Transfer Active"
            VERIFYING_UPDATE -> "Verifying Update"
            VERIFICATION_SUCCEEDED -> "Verification Succeeded"
            VERIFICATION_FAILED -> "Verification Failed"
            APPLYING_UPDATE -> "Applying Update"
            TRANSFER_CANCELED -> "Transfer Canceled"
            APPLY_SUCCESS -> "Apply Success"
            APPLY_FAILED -> "Apply Failed"
            UNKNOWN -> "Unknown Phase"
        }
}

/**
 * The Distribution Phase state indicates the phase of a firmware image distribution
 * being performed by the Firmware Distribution Server.
 */
enum class FirmwareDistributionPhase(internal val value: UByte) {
    IDLE(0x00u),
    TRANSFER_ACTIVE(0x01u),
    TRANSFER_SUCCESS(0x02u),
    APPLYING_UPDATE(0x03u),
    COMPLETED(0x04u),
    FAILED(0x05u),
    CANCELING_UPDATE(0x06u),
    TRANSFER_SUSPENDED(0x07u);

    /**
     * Indicates whether the firmware distribution can be canceled.
     */
    val isCancellable: Boolean
        get() = true

    /**
     * Indicates whether the firmware distribution is not in progress.
     */
    val isBusy: Boolean
        get() = this != IDLE && this != FAILED && this != COMPLETED && this != APPLYING_UPDATE

    /**
     * Indicates whether the firmware distribution can be suspended.
     */
    val isSuspendable: Boolean
        get() = this == TRANSFER_ACTIVE || this == TRANSFER_SUSPENDED

    /**
     * Indicates whether the new firmware can be applied to the Target Nodes.
     */
    val canApply: Boolean
        get() = this != IDLE && this != CANCELING_UPDATE && this != TRANSFER_ACTIVE &&
                this != TRANSFER_SUSPENDED && this != FAILED

    val debugDescription: String
        get() = when (this) {
            IDLE -> "Idle"
            TRANSFER_ACTIVE -> "Transfer Active"
            TRANSFER_SUCCESS -> "Transfer Success"
            APPLYING_UPDATE -> "Applying Update"
            COMPLETED -> "Completed"
            FAILED -> "Failed"
            CANCELING_UPDATE -> "Canceling Update"
            TRANSFER_SUSPENDED -> "Transfer Suspended"
        }

    internal companion object {

        /**
         * Returns the [FirmwareDistributionPhase] for the given [value].
         *
         * @param value The status value.
         * @return The [FirmwareDistributionPhase] for the given [value].
         */
        fun from(value: UByte) = entries.firstOrNull { it.value == value }
    }
}

/**
 * The Firmware Update Additional Information state identifies the Node state after successful
 * application of a verified firmware image.
 */
enum class FirmwareUpdateAdditionalInformation(internal val value: UByte) {
    /**
     * Node’s Composition Data state will not change.
     */
    COMPOSITION_DATA_UNCHANGED(0x0u),

    /**
     * Node’s Composition Data state will change, and Remote Provisioning is not supported. The new
     * Composition Data state value is effective after the Node is reprovisioned.
     */
    COMPOSITION_DATA_CHANGED_AND_RPR_UNSUPPORTED(0x1u),

    /**
     * Node’s Composition Data state will change, and Remote Provisioning is supported. The Node
     * supports remote provisioning and Composition Data Page 128.
     */
    COMPOSITION_DATA_CHANGED_AND_RPR_SUPPORTED(0x2u),

    /**
     * The Node will become unprovisioned after successful application of a verified firmware image.
     */
    DEVICE_UNPROVISIONED(0x3u);

    val debugDescription: String
        get() = when (this) {
            COMPOSITION_DATA_UNCHANGED -> "Composition Data Unchanged"
            COMPOSITION_DATA_CHANGED_AND_RPR_UNSUPPORTED -> "Composition Data Changed and Remote Provisioning Unsupported"
            COMPOSITION_DATA_CHANGED_AND_RPR_SUPPORTED -> "Composition Data Changed and Remote Provisioning Unsupported"
            DEVICE_UNPROVISIONED -> "Device Unprovisioned"
        }
}

/**
 * The Update Policy state indicates when to apply a new firmware image.
 */
enum class FirmwareUpdatePolicy(internal val value: UByte) {
    /**
     * The Firmware Distribution Server verifies that firmware image distribution completed
     * successfully but does not apply the update.
     */
    VERIFY_ONLY(0x00u),

    /**
     * The Firmware Distribution Server verifies that firmware image distribution completed
     * successfully and then applies the firmware update.
     */
    VERIFY_AND_APPLY(0x01u);

    val debugDescription: String
        get() = when (this) {
            VERIFY_ONLY -> "Verify Only"
            VERIFY_AND_APPLY -> "Verify And Apply"
        }

    internal companion object {

        /**
         * Returns the [FirmwareUpdatePolicy] for the given [value].
         *
         * @param value The status value.
         * @return The [FirmwareUpdatePolicy] for the given [value].
         */
        fun from(value: UByte) = entries.firstOrNull { it.value == value }
    }
}

/**
 * A message with Firmware Distribution Status.
 *
 * @property status    Status of the requested message.
 * @property isSuccess True if the message was successful or false otherwise.
 * @property message   Status message of the operation.
 */
interface FirmwareDistributionStatusMessage : StatusMessage {
    val status: FirmwareDistributionMessageStatus
    override val isSuccess: Boolean
        get() = status == FirmwareDistributionMessageStatus.SUCCESS
    override val message: String
        get() = status.debugDescription
}