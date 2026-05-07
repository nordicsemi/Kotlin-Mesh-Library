@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import java.nio.ByteOrder

/**
 * The Config Low Power Node PollTimeout Get is an acknowledged message used to get the current
 * value of PollTimeout timer of the Low Power node within a Friend Node. The message is sent to a
 * Friend Node that has claimed to be handling messages by sending ACKs On Behalf Of (OBO) the
 * indicated Low Power Node. This message should only be sent to a Node that has the Friend feature
 * supported and enabled.
 *
 * @property address The address of the Low Power node.
 */
class ConfigLowPowerNodePollTimeoutGet(val address: Address) : AcknowledgedConfigMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = ConfigLowPowerNodePollTimeoutStatus.opCode
    override val parameters: ByteArray = address.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    /**
     * Constructs a [ConfigLowPowerNodePollTimeoutGet] message for a given low power node address.
     *
     * @param address The address of the Low Power node.
     */
    constructor(address: MeshAddress) : this(address = address.address)

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() =
        "ConfigLowPowerNodePollTimeoutGet(address: 0x${address.toHexString(HexFormat.UpperCase)})"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x802Du

        /**
         * Initializes the [ConfigLowPowerNodePollTimeoutGet] based on the given parameters.
         *
         * @param parameters Message parameters.
         * @return ConfigKeyRefreshPhaseGet or null if the parameters are invalid.
         */
        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let {
                ConfigLowPowerNodePollTimeoutGet(
                    address = it.getUShort(
                        offset = 0,
                        order = ByteOrder.LITTLE_ENDIAN
                    )
                )
            }
    }
}