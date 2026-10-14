@file:Suppress("HasPlatformType")

package no.nordicsemi.kotlin.mesh.core.model.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import no.nordicsemi.kotlin.mesh.core.exception.ImportError
import no.nordicsemi.kotlin.mesh.core.util.Utils
import kotlin.uuid.Uuid

/**
 * Custom serializer/deserializer for UUID. Mesh Configuration Database contains certain UUIDs
 * as string with and without dashes and this Helper class encodes and decodes them accordingly.
 */

object UuidSerializer : KSerializer<Uuid> {


    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(serialName = "UuidSerializer", kind = PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Uuid = runCatching {
        Utils.decode(uuid = decoder.decodeString())
    }.getOrElse {
        throw ImportError(
            "Error while deserializing UUID " +
                    "${(decoder as JsonDecoder).decodeJsonElement()}", it
        )
    }

    override fun serialize(encoder: Encoder, value: Uuid) =
        encoder.encodeString(value = value.toString().uppercase())
}
