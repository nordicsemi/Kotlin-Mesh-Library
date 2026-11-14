package no.nordicsemi.kotlin.mesh.core.model.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import no.nordicsemi.kotlin.mesh.core.exception.ImportError
import no.nordicsemi.kotlin.mesh.core.model.Feature
import no.nordicsemi.kotlin.mesh.core.model.FeatureState
import no.nordicsemi.kotlin.mesh.core.model.Friend
import no.nordicsemi.kotlin.mesh.core.model.LowPower
import no.nordicsemi.kotlin.mesh.core.model.Proxy
import no.nordicsemi.kotlin.mesh.core.model.Relay

/**
 * Serializes the features of a heartbeat publication as an array of the names of the features that
 * trigger heartbeat messages, e.g. `["relay", "proxy"]`, as defined in the Mesh Configuration
 * Database format. Features that are not supported are omitted.
 *
 * For backwards compatibility, the polymorphic object format written by earlier versions is also
 * accepted when deserializing.
 */
internal object HeartbeatFeaturesSerializer : KSerializer<List<Feature>> {

    private const val RELAY = "relay"
    private const val PROXY = "proxy"
    private const val FRIEND = "friend"
    private const val LOW_POWER = "lowPower"

    private val delegate = ListSerializer(String.serializer())

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: List<Feature>) {
        val names = listOfNotNull(
            RELAY.takeIf { value.any { it is Relay && it.isSupported } },
            PROXY.takeIf { value.any { it is Proxy && it.isSupported } },
            FRIEND.takeIf { value.any { it is Friend && it.isSupported } },
            LOW_POWER.takeIf { value.any { it is LowPower && it.isSupported } }
        )
        encoder.encodeSerializableValue(delegate, names)
    }

    override fun deserialize(decoder: Decoder): List<Feature> = runCatching {
        val features = mutableListOf<Feature>()
        (decoder as JsonDecoder).decodeJsonElement().let { element ->
            (element as kotlinx.serialization.json.JsonArray).forEach { item ->
                when (item) {
                    is JsonPrimitive -> toFeature(item.content)?.let { features += it }
                    is JsonObject -> legacyToFeature(item)?.let { features += it }
                    else -> Unit
                }
            }
        }
        // Order is always relay, proxy, friend and low power.
        features.sortedBy { order(it) }
    }.getOrElse {
        throw ImportError("Error while deserializing heartbeat publication features", it)
    }

    private fun order(feature: Feature) = when (feature) {
        is Relay -> 0
        is Proxy -> 1
        is Friend -> 2
        is LowPower -> 3
    }

    private fun toFeature(name: String): Feature? = when (name) {
        RELAY -> Relay(FeatureState.Enabled)
        PROXY -> Proxy(FeatureState.Enabled)
        FRIEND -> Friend(FeatureState.Enabled)
        LOW_POWER -> LowPower(FeatureState.Enabled)
        else -> throw IllegalArgumentException("Unknown feature $name!")
    }

    /** Parses `{"type": "...Relay", "state": {"type": "...FeatureState.Enabled"}}`. */
    private fun legacyToFeature(item: JsonObject): Feature? {
        val type = item["type"]?.jsonPrimitive?.content ?: return null
        val state = item["state"]?.jsonObject?.get("type")?.jsonPrimitive?.content ?: return null
        if (state.endsWith("Unsupported")) return null
        val featureState = if (state.endsWith("Enabled")) FeatureState.Enabled
        else FeatureState.Disabled
        return when {
            type.endsWith(".Relay") -> Relay(featureState)
            type.endsWith(".Proxy") -> Proxy(featureState)
            type.endsWith(".Friend") -> Friend(featureState)
            type.endsWith(".LowPower") -> LowPower(featureState)
            else -> null
        }
    }
}
