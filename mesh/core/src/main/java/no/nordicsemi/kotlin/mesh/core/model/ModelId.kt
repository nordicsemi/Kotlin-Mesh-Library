@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nordicsemi.kotlin.data.HexString
import no.nordicsemi.kotlin.mesh.core.model.serialization.ModelIdSerializer

/**
 * Represents Model ID of a Bluetooth mesh model.
 *
 * @property id                         16-bit company identifier and the 16-bit model identifier
 *                                      where the company identifier being the 2-most significant
 *                                      bytes. In the case of a Bluetooth SIG defined model, the
 *                                      company identifier is 0.
 * @property isBluetoothSigAssigned     True if the model is a Bluetooth SIG defined model.
 */
@Serializable(with = ModelIdSerializer::class)
sealed class ModelId {
    @Deprecated("A vendor model whose Company Identifier is 0x0000 has the same id as the SIG model with the same Model Identifier. Use toHex() instead.")
    @SerialName(value = "modelId")
    abstract val id: UInt
    val isBluetoothSigAssigned: Boolean
        get() = this is SigModelId

    /**
     * Converts ModelID to hex.
     *
     * @param prefix0x If true prefixes hex value with 0x.
     */
    fun toHex(prefix0x: Boolean = false): String = when (this) {
        is SigModelId -> "%04X".format(id.toShort())
        is VendorModelId -> "%08X".format(id.toInt())
    }.also {
        return if (prefix0x) "0x$it" else it
    }

    companion object {

        /**
         * Converts a [HexString] encoded model ID to a [ModelId].
         * Expects the HexString to be encoded in big-endian.
         *
         * @return [ModelId] instance.
         * @throws NumberFormatException if the string is not 4 or 8 hexadecimal characters.
         */
        @Throws(NumberFormatException::class)
        fun HexString.decode(): ModelId = when (length) {
            4 -> SigModelId(modelIdentifier = toUShort(radix = 16))
            8 -> VendorModelId(
                modelIdentifier = substring(startIndex = 4).toUShort(radix = 16),
                companyIdentifier = substring(startIndex = 0, endIndex = 4).toUShort(radix = 16),
            )

            else -> throw NumberFormatException(
                "A model ID must be 4 or 8 hexadecimal characters."
            )
        }

        /**
         * Converts a [UInt] encoded model ID to a [ModelId].
         *
         * @return [ModelId] instance.
         */
        @Deprecated(
            "Incorrectly decodes a vendor model whose Company Identifier is 0x0000 as a SIG model. " +
                    "Use HexString.decode() instead."
        )
        fun UInt.decode(): ModelId = when (this and 0xFFFF0000u) {
            0u -> SigModelId(modelIdentifier = this.toUShort())
            else -> VendorModelId(id = this)
        }
    }
}

/**
 * Wrapper class for 16-bit Bluetooth SIG model identifier.
 *
 * @property modelIdentifier 16-bit model identifier.
 */
@Serializable
class SigModelId(
    val modelIdentifier: UShort,
) : ModelId() {
    @Deprecated("A vendor model whose Company Identifier is 0x0000 has the same id as the SIG model with the same Model Identifier. Use toHex() instead.")
    @SerialName(value = "modelId")
    override val id: UInt = modelIdentifier.toUInt()

    override fun toString(): String =
        "SigModelId(${
            modelIdentifier.toHexString(
                format = HexFormat {
                    number {
                        prefix = "0x"
                        minLength = 4
                        removeLeadingZeros = true
                    }
                }
            )
        })"

    override fun equals(other: Any?): Boolean {
        return other is SigModelId
                && modelIdentifier == other.modelIdentifier
    }

    override fun hashCode(): Int = modelIdentifier.hashCode()
}

/**
 * Wrapper class for 32-bit vendor model identifier.
 *
 * @property modelIdentifier    16-bit model identifier.
 * @property companyIdentifier  16-bit company identifier.
 */
@Serializable
class VendorModelId(
    @Deprecated("A vendor model whose Company Identifier is 0x0000 has the same id as the SIG model with the same Model Identifier. Use toHex() instead.")
    @SerialName(value = "modelId")
    override val id: UInt,
) : ModelId() {
    val companyIdentifier: UShort = ((id and 0xFFFF0000u) shr 16).toUShort()
    val modelIdentifier: UShort = (id and 0x0000FFFFu).toUShort()

    /**
     * Constructs a VendorModelId from the given model identifier and company identifier.
     *
     * @param modelIdentifier    16-bit model identifier.
     * @param companyIdentifier  16-bit company identifier.
     * @constructor Creates a VendorModelId.
     */
    constructor(
        modelIdentifier: UShort,
        companyIdentifier: UShort,
    ) : this((modelIdentifier.toUInt()) or (companyIdentifier.toUInt() shl 16))

    override fun toString(): String {
        return "VendorModelId(${
                    modelIdentifier.toHexString(
                        format = HexFormat {
                            number {
                                prefix = "0x"
                                minLength = 4
                                upperCase = true
                            }
                        }
                    )
                }, " +
                "companyIdentifier: ${
                    companyIdentifier.toHexString(
                        format = HexFormat {
                            number {
                                prefix = "0x"
                                minLength = 4
                                upperCase = true
                            }
                        }
                    )
                })"
    }

    override fun equals(other: Any?): Boolean {
        return other is VendorModelId
                && companyIdentifier == other.companyIdentifier
                && modelIdentifier == other.modelIdentifier
    }

    override fun hashCode(): Int {
        var result = 31 * companyIdentifier.hashCode()
        result = 31 * result + modelIdentifier.hashCode()
        return result
    }
}