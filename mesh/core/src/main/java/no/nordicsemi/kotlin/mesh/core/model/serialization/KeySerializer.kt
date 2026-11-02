
package no.nordicsemi.kotlin.mesh.core.model.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.data.toHexString
import no.nordicsemi.kotlin.mesh.core.exception.ImportError

/**
 * Custom JSON serializer/deserializer for byte arrays.
 */
internal object KeySerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(serialName = "Key", kind = PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder) = runCatching {
        decoder.decodeString().toByteArray()
    }.getOrElse {
        throw ImportError(
            "Error while deserializing Key " +
                    "${(decoder as JsonDecoder).decodeJsonElement()}", it
        )
    }

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(value.toHexString(format = HexFormat.UpperCase))
    }
}