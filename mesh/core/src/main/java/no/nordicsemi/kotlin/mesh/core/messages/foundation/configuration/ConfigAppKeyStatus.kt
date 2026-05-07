@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.BaseMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetAndAppKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetAndAppKeyMessage.Companion.decodeNetAndAppKeyIndex
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetAndAppKeyMessage.Companion.encodeNetAndAppKeyIndex
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex

/**
 * Status declaring if the the [ConfigNetAndAppKeyMessage] operation succeeded or not.
 *
 * @property applicationKeyIndex Index of the application key.
 * @property networkKeyIndex     Index of the network key.
 * @property status              Status of the message.
 * @property opCode              Message op code.
 * @property parameters          Message parameters.
 * @constructor Constructs the ConfigAppKeyStatus message.
 */
class ConfigAppKeyStatus(
    override val applicationKeyIndex: KeyIndex,
    override val networkKeyIndex: KeyIndex,
    override val status: ConfigMessageStatus
) : ConfigResponse, ConfigStatusMessage, ConfigNetAndAppKeyMessage {

    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() = status.value.toByteArray() + encodeNetAndAppKeyIndex(
            appKeyIndex = applicationKeyIndex,
            netKeyIndex = networkKeyIndex
        )

    /**
     * Constructs the ConfigAppKeyStatus message.
     *
     * @param applicationKey Application key to confirm
     * @constructor Constructs the ConfigAppKeyStatus message.
     */
    constructor(applicationKey: ApplicationKey) : this(
        applicationKeyIndex = applicationKey.index,
        networkKeyIndex = applicationKey.boundNetKeyIndex,
        status = ConfigMessageStatus.SUCCESS
    )

    /**
     * Constructs the ConfigAppKeyStatus message.
     *
     * @param request [ConfigNetAndAppKeyMessage] operation that was sent to the mesh node.
     * @param status  [ConfigMessageStatus] for a given [request].
     * @constructor Constructs the ConfigAppKeyStatus message.
     */
    constructor(
        request: ConfigNetAndAppKeyMessage,
        status: ConfigMessageStatus
    ) : this(
        applicationKeyIndex = request.applicationKeyIndex,
        networkKeyIndex = request.networkKeyIndex,
        status = status
    )

    override fun toString() = "ConfigAppKeyStatus(networkKeyIndex: $networkKeyIndex, " +
            "applicationKeyIndex: $applicationKeyIndex, status: $status)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8003u

        /**
         * Initializes the ConfigAppKeyStatus message.
         *
         * @param parameters Message parameters.
         * @return ConfigAppKeyStatus or null if the parameters are invalid.
         */
        override fun init(parameters: ByteArray?): BaseMeshMessage? = parameters?.takeIf {
            it.size == 4
        }?.let { params ->
            val status = ConfigMessageStatus.from(
                value = params.first().toUByte()
            ) ?: return null
            val decodedNetAndAppKeyIndex = decodeNetAndAppKeyIndex(data = params, offset = 1)
            ConfigAppKeyStatus(
                applicationKeyIndex = decodedNetAndAppKeyIndex.applicationKeyIndex,
                networkKeyIndex = decodedNetAndAppKeyIndex.networkKeyIndex,
                status = status
            )
        }
    }
}