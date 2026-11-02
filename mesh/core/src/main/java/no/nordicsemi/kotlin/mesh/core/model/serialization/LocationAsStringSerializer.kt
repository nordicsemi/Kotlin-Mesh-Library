
package no.nordicsemi.kotlin.mesh.core.model.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import no.nordicsemi.kotlin.mesh.core.exception.ImportError
import no.nordicsemi.kotlin.mesh.core.model.Location

/**
 * Custom JSON serializer/deserializer for all properties formatted as a 4-digit hexadecimal string.
 */
internal object LocationAsStringSerializer : KSerializer<Location> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(serialName = "UShortSerializer", kind = PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Location = runCatching {
        Location.from(decoder.decodeString().toUInt(radix = 16).toUShort())
    }.getOrElse {
        throw ImportError(
            "Error while deserializing Location " +
                    "${(decoder as JsonDecoder).decodeJsonElement()}", it
        )
    }

    override fun serialize(encoder: Encoder, value: Location) {
        encoder.encodeString(value = value.value.toHexString(format = HexFormat.UpperCase))
    }
}