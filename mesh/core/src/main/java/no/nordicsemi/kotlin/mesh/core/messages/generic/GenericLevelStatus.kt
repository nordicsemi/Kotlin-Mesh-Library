package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.getShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.TransitionStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import java.nio.ByteOrder

/**
 * This message is a response to a [GenericLevelGet] and [GenericLevelSet] that contains the current
 * status of a GenericOnOffServer.
 *
 * @property level         Current level value of the Generic Level state.
 * @property targetLevel   Target value of the Generic Level state.
 * @property remainingTime The time that an element will take to transition to the target state from
 *                         the present state.
 */
class GenericLevelStatus(
    override val remainingTime: TransitionTime?,
    val level: Short,
    val targetLevel: Short?,
) : MeshResponse, UnacknowledgedMeshMessage, TransitionStatusMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = when (targetLevel != null && remainingTime != null) {
        true -> level.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                targetLevel.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                remainingTime.rawValue.toByteArray()

        else -> level.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
    }

    /**
     * Convenience constructor to create a GenericLevelStatus message with a level value.
     *
     * @param level The current level of the Generic Level state.
     */
    @Suppress("unused")
    constructor(level: Short) : this(remainingTime = null, level = level, targetLevel = null)

    override fun toString() = "GenericLevelStatus(level: $level" +
            if (targetLevel != null && remainingTime != null) {
                ", targetState: $targetLevel, remainingTime: $remainingTime)"
            } else ")"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8208u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 || it.size == 5 }
            ?.let { params ->
                GenericLevelStatus(
                    level = params.getShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    targetLevel = when (params.size) {
                        5 -> params.getShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN)
                        else -> null
                    },
                    remainingTime = when (params.size) {
                        5 -> TransitionTime(rawValue = params[4].toUByte())
                        else -> null
                    },
                )
            }
    }
}