@file:Suppress("unused", "SERIALIZER_TYPE_INCOMPATIBLE", "PropertyName")

package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nordicsemi.kotlin.mesh.core.model.serialization.FeaturesSerializer

/**
 * Features represents the functionality of a [Node] that is determined by the set features that the
 * node supports.
 *
 * @property relay      Ability to receive and retransmit mesh messages over the advertising bearer
 *                      to enable larger networks. Null if the current [FeatureState] of the [Relay]
 *                      feature is unknown.
 * @property proxy      Ability to receive and retransmit mesh messages between GATT and advertising
 *                      bearers. Null if the current [FeatureState] of the [Proxy] feature is
 *                      unknown.
 * @property friend     Ability to operate within a mesh network at significantly reduced receiver
 *                      duty cycles only in conjunction with a node supporting the Friend feature.
 *                      Null if the current [FeatureState] of the [Friend] feature is unknown.
 * @property lowPower   Ability to help a node supporting the Low Power feature to operate by
 *                      storing messages destined for those nodes. Null if the current
 *                      [FeatureState] of the [LowPower] feature is unknown.
 */
@ConsistentCopyVisibility
@Serializable(with = FeaturesSerializer::class)
data class Features internal constructor(
    @SerialName(value = "relay")
    internal var _relay: Relay? = null,
    @SerialName(value = "proxy")
    internal var _proxy: Proxy? = null,
    @SerialName(value = "friend")
    internal var _friend: Friend? = null,
    @SerialName(value = "lowPower")
    internal var _lowPower: LowPower? = null
) {
    val relay: Relay?
        get() = _relay
    val proxy: Proxy?
        get() = _proxy
    val friend: Friend?
        get() = _friend
    val lowPower: LowPower?
        get() = _lowPower

    val rawValue = ((relay?.state?.value ?: 2) shr 0 or
            (proxy?.state?.value ?: 2) shr 1 or
            (friend?.state?.value ?: 2) shr 2 or
            (lowPower?.state?.value ?: 2) shr 3).toUShort()

    /**
     * Constructs a Features object from the given raw value.
     *
     * @param rawValue  Raw value of the features.
     */
    internal constructor(rawValue: UShort) : this(
        _relay = Relay(state = FeatureState.from(value = rawValue.toInt() shl 0)),
        _proxy = Proxy(state = FeatureState.from(value = rawValue.toInt() shl 1)),
        _friend = Friend(state = FeatureState.from(value = rawValue.toInt() shl 2)),
        _lowPower = LowPower(state = FeatureState.from(value = rawValue.toInt() shl 3))
    )

    /**
     * Converts the features to an array of [Feature]s.
     */
    fun toList(): List<Feature> = listOf(
        relay ?: Relay(state = FeatureState.Unsupported),
        proxy ?: Proxy(state = FeatureState.Unsupported),
        friend ?: Friend(state = FeatureState.Unsupported),
        lowPower ?: LowPower(state = FeatureState.Unsupported)
    )

    override fun toString(): String = toList().joinToString()

    internal companion object {

        /**
         * Constructs a Features object from the given mask.
         *
         * Note: The state of the following features is unknown until the corresponding Config...Get
         * message is sent to the node. However, if the Low Power state is enabled it cannot be
         * disabled.
         *
         * @param mask  Raw value of the features.
         */
        fun init(mask: UShort) = Features(
            _relay = if (mask.toInt() and 0x01 == 0)
                Relay(state = FeatureState.Unsupported)
            else null,
            _proxy = if (mask.toInt() and 0x02 == 0)
                Proxy(state = FeatureState.Unsupported)
            else null,
            _friend = if (mask.toInt() and 0x04 == 0)
                Friend(state = FeatureState.Unsupported)
            else null,
            _lowPower = LowPower(
                state = if (mask.toInt() and 0x08 == 0)
                    FeatureState.Unsupported
                else FeatureState.Enabled
            )
        )
    }
}

/**
 * Represents a type feature.
 *
 * @property state    Defines the state of the feature.
 * @property rawValue Raw value of the feature state.
 */
@Serializable
sealed class Feature {
    abstract val state: FeatureState
    abstract val rawValue: UShort

    val isEnabled: Boolean
        get() = state.isEnabled

    val isSupported: Boolean
        get() = state.isSupported
}

/**
 * Relay feature is the ability to receive and retransmit mesh messages over the advertising
 * bearer to enable larger networks.
 *
 * @property state State of the relay feature.
 */
@Serializable
data class Relay(override val state: FeatureState) : Feature() {
    override val rawValue: UShort = (state.value shr 0).toUShort()

    override fun toString() = "Relay($state)"
}

/**
 * Proxy feature is the ability to receive and retransmit mesh messages between GATT and
 * advertising bearers.
 *
 * @property state State of the proxy feature.
 */
@Serializable
data class Proxy(override var state: FeatureState) : Feature() {
    override val rawValue: UShort = (state.value shr 1).toUShort()

    override fun toString() = "Proxy($state)"
}

/**
 * Friend feature is the ability to operate within a mesh network at significantly
 * reduced receiver duty cycles only in conjunction with a node supporting the Friend feature.
 *
 * @property state State of friend feature.
 */
@Serializable
data class Friend(override val state: FeatureState) : Feature() {
    override val rawValue: UShort = (state.value shr 2).toUShort()

    override fun toString() = "Friend($state)"
}

/**
 * LowPower feature is the ability to help a node supporting the Low Power feature
 * to operate by storing messages destined for those nodes.
 *
 * @property state State of low power feature.
 */
@Serializable
data class LowPower(override val state: FeatureState) : Feature() {
    override val rawValue: UShort = (state.value shr 3).toUShort()

    override fun toString() = "LowPower($state)"
}

/**
 * FeatureState describes the state of a given [Feature].
 *
 * @property value 0 = disabled, 1 = enabled, 2 = unsupported
 * @property isEnabled Returns true if the feature is enabled or false otherwise.
 * @property isSupported Returns true if the feature is enabled or disabled and NOT unsupported.
 */
@Serializable
sealed class FeatureState(val value: Int) {

    /** Disabled state. */
    @Serializable
    data object Disabled : FeatureState(value = DISABLED)

    /** Enabled state. */
    @Serializable
    data object Enabled : FeatureState(value = ENABLED)

    /** Unsupported state. */
    @Serializable
    data object Unsupported : FeatureState(value = UNSUPPORTED)

    val isEnabled: Boolean
        get() = this is Enabled

    val isSupported: Boolean
        get() = this !is Unsupported

    override fun toString() = when (this) {
        Disabled -> "Disabled"
        Enabled -> "Enabled"
        Unsupported -> "Unsupported"
    }

    companion object {
        private const val DISABLED = 0
        private const val ENABLED = 1
        private const val UNSUPPORTED = 2

        /**
         * Returns the feature state for a given a feature.
         *
         * @param value                         Integer value describing the state.
         * @throws IllegalArgumentException     if the feature value is not 0, 1 or 2.
         */
        @Throws(IllegalArgumentException::class)
        fun from(value: Int) = when (value) {
            DISABLED -> Disabled
            ENABLED -> Enabled
            UNSUPPORTED -> Unsupported
            else -> throw IllegalArgumentException(
                "Feature value should be from $DISABLED, $ENABLED or $UNSUPPORTED!"
            )
        }
    }
}

/**
 * Converts an list of [Feature]s to a raw value.
 */
fun List<Feature>.toUShort(): UShort {
    var rawValue: UShort = 0u
    for (feature in this) {
        rawValue = rawValue or feature.rawValue
    }
    return rawValue
}
/**
 * Converts an list of Features to a [Features] object.
 */
fun List<Feature>.toFeatures() = Features(
    _relay = this[0] as Relay,
    _proxy = this[1] as Proxy,
    _friend = this[2] as Friend,
    _lowPower = this[3] as LowPower
)