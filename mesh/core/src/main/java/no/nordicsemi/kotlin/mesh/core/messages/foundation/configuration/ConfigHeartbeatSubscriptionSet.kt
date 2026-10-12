@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatSubscriptionDestination
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatSubscriptionSource
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress
import no.nordicsemi.kotlin.data.ByteOrder

/**
 * This message is used to set the Heartbeat Subscription state of an element. The response received
 * to this message will be a [ConfigHeartbeatSubscriptionStatus] message.
 *
 * @property source      Source address for Heartbeat subscription messages.
 * @property destination Destination address for Heartbeat subscription messages.
 * @property periodLog   Period between Heartbeat messages.
 *
 * @constructor Creates a ConfigHeartbeatPublicationGet message.
 */
class ConfigHeartbeatSubscriptionSet(
    val source: HeartbeatSubscriptionSource,
    val destination: HeartbeatSubscriptionDestination,
    val periodLog: UByte,
) : AcknowledgedConfigMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = ConfigHeartbeatSubscriptionStatus.opCode
    override val parameters = source.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            destination.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            periodLog.toByte()

    val isSubscriptionEnabled = source !is UnassignedAddress && destination !is UnassignedAddress
            && periodLog > 0u

    /**
     * Constructs a ConfigHeartbeatSubscriptionSet message that will disable receiving and
     * processing of Heartbeat messages.
     */
    constructor() : this(
        source = UnassignedAddress,
        destination = UnassignedAddress,
        periodLog = 0u
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "ConfigHeartbeatSubscriptionSet(source: ${
        source.address.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )
    }, destination: ${
        destination.address.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )
    }, periodLog: $periodLog)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode: UInt = 0x803Bu

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 5
        }?.let { params ->
            ConfigHeartbeatSubscriptionSet(
                source = MeshAddress.create(
                    address = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                ) as HeartbeatSubscriptionSource,
                destination = MeshAddress.create(
                    address = params.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN)
                ) as HeartbeatSubscriptionDestination,
                periodLog = params[4].toUByte()
            )
        }
    }
}