@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.shl
import no.nordicsemi.kotlin.data.ushr
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigRelaySet
import no.nordicsemi.kotlin.mesh.core.model.FeatureState
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.Relay
import no.nordicsemi.kotlin.mesh.core.model.RelayRetransmit
import kotlin.experimental.and

/**
 * Defines a message that's message sent as a response to a [ConfigRelayGet] or [ConfigRelaySet].
 *
 * @property state Feature state of the [Relay] feature.
 * @property count Number of retransmissions. Possible values are 0...7, which correspond
 *                 to 1 - 8 transmissions in total.
 * @property steps Number of 10-millisecond steps between transmissions, decremented by 1.
 *                 Possible values are 0...31, which correspond to 10 ms to 320 ms in 10 ms
 *                 steps.
 */
data class ConfigRelayStatus(
    val state: FeatureState,
    val count: UByte,
    val steps: UByte,
) : ConfigResponse {
    override val opCode = Initializer.opCode
    override val parameters = byteArrayOf(
        state.value.toByte(),
        (((count and 0x07u) or (steps shl 3)).toByte())
    )

    /**
     * Constructs a [ConfigRelayStatus] message.
     *
     * @param count Number of retransmissions, in range 1..8.
     * @param interval Interval between transmissions, in milliseconds, in range 10..320 with step of 10 ms.
     */
    constructor(count: Int, interval: Int) : this(
        state = FeatureState.Enabled,
        count = (count - 1).toUByte(),
        steps = ((interval / 10) - 1).toUByte()
    )

    /**
     * Constructs a [ConfigRelayStatus] message.
     *
     * @param relayRetransmit The relay retransmit parameters.
     */
    constructor(relayRetransmit: RelayRetransmit) : this(relayRetransmit.count, relayRetransmit.interval)

    /**
     * Constructs a [ConfigRelayStatus] message.
     *
     * @param node The node containing the relay retransmit parameters.
     */
    constructor(node: Node) : this(
        state = node.features.relay?.state ?: FeatureState.Unsupported,
        count = ((node.relayRetransmit?.count ?: 1) - 1).toUByte(),
        steps = ((node.relayRetransmit?.interval ?: 10) / 10 - 1).toUByte()
    )

    init {
        require(count in 0u..7u) { "Count must be in range 0..7" }
        require(steps in 0u..31u) { "Steps must be in range 0..31" }
    }

    override fun toString() = "ConfigRelayStatus(state: $state, count: $count, steps: $steps)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8028u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let { params ->
                val state = FeatureState.from(params[0].toUByte().toInt())
                ConfigRelayStatus(
                    state = state,
                    count = (params[1] and 0x07).toUByte(),
                    steps = (params[1] ushr 3).toUByte()
                )
            }

        /**
         * The status reporting that the Relay feature is not supported.
         */
        val unsupported = ConfigRelayStatus(state = FeatureState.Unsupported, count = 0u, steps = 0u)
    }
}