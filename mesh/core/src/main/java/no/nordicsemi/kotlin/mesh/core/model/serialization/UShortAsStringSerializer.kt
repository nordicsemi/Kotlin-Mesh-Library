package no.nordicsemi.kotlin.mesh.core.model.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import no.nordicsemi.kotlin.mesh.core.exception.ImportError

/**
 * Custom JSON serializer/deserializer for all properties formatted as a 4-digit hexadecimal string.
 */
internal object UShortAsStringSerializer : KSerializer<UShort> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(serialName = "UShortSerializer", kind = PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UShort = runCatching {
        decoder.decodeString().toUInt(radix = 16).toUShort()
    }.getOrElse {
        throw ImportError(
            "Error while deserializing 16-bit value " +
                    "${(decoder as JsonDecoder).decodeJsonElement()}", it
        )
    }


    override fun serialize(encoder: Encoder, value: UShort) {
        encoder.encodeString(value = value.toHexString(format = HexFormat.UpperCase))
    }
}