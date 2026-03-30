package no.nordicsemi.kotlin.mesh.core.messages

interface BLOBMessageInitializer : BaseMeshMessageInitializer, HasOpCode

/**
 * Status codes used by the BLOB Transfer Server and the BLOB Transfer Client models.
 */
enum class BLOBTransferMessageStatus(val value: UByte) {
    /**
     * The message was successfully processed.
     */
    SUCCESS(0x00u),

    /**
     * The Block Number field value is not within the range of the blocks  being transferred.
     */
    INVALID_BLOCK_NUMBER(0x01u),

    /**
     * The block size is smaller than the size indicated by the Min Block Size Log state or is
     * larger than the size indicated by the Max Block Size Log state.
     */
    INVALID_BLOCK_SIZE(0x02u),

    /**
     * The chunk size exceeds the size indicated by the Max Chunk Size state, or the number of
     * chunks exceeds the number specified by the Max Total Chunks state.
     */
    INVALID_CHUNK_SIZE(0x03u),

    /**
     * The operation cannot be performed while the server is in the current phase.
     */
    WRONG_PHASE(0x04u),

    /**
     * A parameter value in the message cannot be accepted.
     */
    INVALID_PARAMETER(0x05u),

    /**
     * The message contains a BLOB ID value that is not expected.
     */
    WRONG_BLOB_ID(0x06u),

    /**
     * There is not enough space available in memory to receive the BLOB.
     */
    BLOB_TOO_LARGE(0x07u),

    /**
     * The transfer mode is not supported by the BLOB Transfer Server model.
     */
    UNSUPPORTED_TRANSFER_MODE(0x08u),

    /**
     * An internal error occurred on the node.
     */
    INTERNAL_ERROR(0x09u),

    /**
     * The requested information cannot be provided while the server is in the current phase.
     */
    INFORMATION_UNAVAILABLE(0x0Au);

    val debugDescription: String
        get() = when (this) {
            SUCCESS -> "Success"
            INVALID_BLOCK_NUMBER -> "Invalid Block Number"
            INVALID_BLOCK_SIZE -> "Invalid Block Size"
            INVALID_CHUNK_SIZE -> "Invalid Chunk Size"
            WRONG_PHASE -> "Wrong Phase"
            INVALID_PARAMETER -> "Invalid Parameter"
            WRONG_BLOB_ID -> "Wrong BLOB ID"
            BLOB_TOO_LARGE -> "BLOB Too Large"
            UNSUPPORTED_TRANSFER_MODE -> "Unsupported Transfer Mode"
            INTERNAL_ERROR -> "Internal Error"
            INFORMATION_UNAVAILABLE -> "Information Unavailable"
        }

    companion object {

        /**
         * Returns the BLOBTransferMessageStatus for the given value.
         *
         * @param value Value of the status.
         * @return BLOBTransferMessageStatus
         */
        fun from(value: UByte) = entries.find { it.value == value }
    }
}


/**
 * The Transfer Mode state indicates the mode of the BLOB transfer.
 */
enum class TransferMode(val value: UByte) {
    /**
     * No Active Transfer.
     */
    NO_ACTIVE_TRANSFER(0x00u),

    /**
     * Push BLOB Transfer mode.
     */
    PUSH(0x01u),

    /**
     * Pull BLOB Transfer mode.
     */
    PULL(0x02u);

    val debugDescription: String
        get() = when (this) {
            NO_ACTIVE_TRANSFER -> "No Active Transfer"
            PUSH -> "Push"
            PULL -> "Pull"
        }

    companion object {
        fun from(value: UByte): TransferMode? =
            entries.find { it.value == value }
    }
}