@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package no.nordicsemi.kotlin.mesh.provisioning

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.bearer.BearerError
import no.nordicsemi.kotlin.mesh.bearer.Pdu
import no.nordicsemi.kotlin.mesh.bearer.PduType
import no.nordicsemi.kotlin.mesh.bearer.provisioning.ProvisioningBearer
import no.nordicsemi.kotlin.mesh.core.exception.NoLocalProvisioner
import no.nordicsemi.kotlin.mesh.core.exception.NoUnicastRangeAllocated
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastRange
import no.nordicsemi.kotlin.mesh.crypto.Algorithms
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.Logger
import no.nordicsemi.kotlin.mesh.provisioning.bearer.send
import kotlin.uuid.ExperimentalUuidApi

/**
 * Provisioning manager is responsible for provisioning new devices to a mesh network.
 *
 * @property unprovisionedDevice          Unprovisioned device to be provisioned.
 * @property meshNetwork                  Mesh network to which the device will be provisioned.
 * @property bearer                       Bearer used to send provisioning PDUs.
 * @property configuration                Provisioning configuration used to provision the device.
 * @property suggestedUnicastAddress      Suggested unicast address to be assigned to the device.
 * @property logger                       Logger for the provisioning manager.
 */
class ProvisioningManager(
    private val unprovisionedDevice: UnprovisionedDevice,
    private val meshNetwork: MeshNetwork,
    val bearer: ProvisioningBearer,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    lateinit var configuration: ProvisioningParameters
    var logger: Logger? = null

    var suggestedUnicastAddress: UnicastAddress? = null
        private set

    init {
        // Ensures that the mesh network has at least one provisioner added and a unicast address
        // range is allocated.
        meshNetwork.localProvisioner?.let {
            require(it.allocatedUnicastRanges.isNotEmpty()) {
                logger?.e(LogCategory.PROVISIONING) { "No unicast ranges allocated" }
                throw NoUnicastRangeAllocated()
            }
        } ?: run {
            logger?.e(LogCategory.PROVISIONING) { "No local provisioner" }
            throw NoLocalProvisioner()
        }

        // Ensures the provided bearer supports provisioning PDUs.
        require(bearer.supports(PduType.PROVISIONING_PDU)) {
            logger?.e(LogCategory.PROVISIONING) {
                "Bearer does not support provisioning pdu"
            }
            throw BearerError.PduTypeNotSupported()
        }
    }

    /**
     * Starts the provisioning process with the given attention timer.
     *
     * @param attentionTimer Attention timer value in seconds.
     * @return Flow of provisioning states that could be used to observe and continue/cancel the
     *         provisioning process.
     * @throws UnsupportedDevice If the device does not support the required algorithms.
     * @throws NoAddressAvailable If the device does not have any unicast address available.
     * @throws InvalidAddress If the device has an invalid unicast address.
     * @throws InvalidPdu If the device has sent an invalid PDU.
     * @throws InvalidPublicKey If the device has sent an invalid public key.
     * @throws InvalidOobValueFormat If the device has sent an invalid OOB value.
     * @throws RemoteError If the device has sent an error.
     * @throws ProvisioningError If the provisioning process failed.
     * @throws BearerError.Closed If the bearer is closed.
     */
    @OptIn(ExperimentalUuidApi::class)
    @Throws(
        UnsupportedDevice::class,
        NoAddressAvailable::class,
        InvalidAddress::class,
        InvalidPdu::class,
        InvalidPublicKey::class,
        InvalidOobValueFormat::class,
        RemoteError::class,
        ProvisioningError::class,
        BearerError.Closed::class
    )
    fun provision(attentionTimer: UByte) = flow {
        try {
            // Is there bearer open?
            require(bearer.isOpen) {
                logger?.e(LogCategory.PROVISIONING) { "Bearer closed" }
                throw BearerError.Closed()
            }
            // Emit the current state.
            emit(value = ProvisioningState.RequestingCapabilities)

            // Initialize Provisioning data.
            val provisioningData = ProvisioningData()

            // Sends the provisioning invite and awaits for the capabilities.
            val capabilities = awaitCapabilities(
                invite = ProvisioningRequest.Invite(attentionTimer),
                provisioningData = provisioningData
            )
            configuration = ProvisioningParameters.defaultFrom(
                capabilities = capabilities,
                meshNetwork = meshNetwork,
            )
            suggestedUnicastAddress = configuration.unicastAddress

            // We use a mutex here to wait for the user to either start or cancel the provisioning.
            val mutex = Mutex(locked = true)
            // Emit to the user that the capabilities have been received.
            emit(
                value = ProvisioningState.CapabilitiesReceived(
                    capabilities = capabilities,
                    defaultParameters = configuration,
                    start = { configuration ->
                        this@ProvisioningManager.configuration = configuration
                        mutex.unlock()
                    },
                    cancel = { mutex.unlock() },
                )
            )
            mutex.lock()

            // Checks if the device supports the required algorithms
            require(capabilities.algorithms.any { Algorithms.algorithms.contains(element = it) }) {
                throw UnsupportedDevice()
            }

            // Is the Unicast address valid?
            require(
                isUnicastAddressValid(
                    unicastAddress = configuration.unicastAddress,
                    numberOfElements = capabilities.numberOfElements,
                )
            ) {
                logger?.e(LogCategory.PROVISIONING) { "Unicast address is not valid" }
                throw InvalidAddress()
            }

            // Try generating Private and Public keys. This may fail if the given algorithm is not
            // supported.
            provisioningData.generateKeys(algorithm = configuration.algorithm)

            emit(value = ProvisioningState.Provisioning)
            provisioningData.prepare(
                networkKey = configuration.networkKey,
                ivIndex = meshNetwork.ivIndex,
                unicastAddress = configuration.unicastAddress,
            )

            ProvisioningRequest.Start(configuration = configuration).also { start ->
                logger?.v(LogCategory.PROVISIONING) { "Sending $start" }
                send(request = start).also { provisioningData.accumulate(data = it) }
            }

            // If the device's Public Key was obtained OOB, we are now ready to calculate the device's
            // Shared Secret, if not we need send the provisioner public key and wait for the device's
            // Public Key.
            val key = when (configuration.publicKey) {
                is PublicKey.OobPublicKey -> (configuration.publicKey as PublicKey.OobPublicKey).key
                else -> {
                    awaitProvisioneePublicKey(
                        request = ProvisioningRequest.PublicKey(provisioningData.provisionerPublicKey),
                        provisioningData = provisioningData
                    )
                }
            }
            provisioningData.apply {
                onDevicePublicKeyReceived(
                    key = key,
                    usingOob = configuration.publicKey is PublicKey.OobPublicKey
                )
                accumulate(data = key)
            }

            requestAuthentication(
                method = configuration.authMethod,
                sizeInBytes = provisioningData.algorithm.length shr 3,
                onAuthValueReceived = provisioningData::onAuthValueReceived,
                mutex = mutex
            )?.also { action ->
                emit(value = ProvisioningState.AuthActionRequired(action = action))
                when (action) {
                    is AuthAction.DisplayNumber, is AuthAction.DisplayAlphaNumeric -> {
                        mutex.unlock()
                        awaitInputComplete()
                        emit(value = ProvisioningState.InputComplete)
                    }

                    is AuthAction.ProvideStaticKey,
                    is AuthAction.ProvideNumeric,
                    is AuthAction.ProvideAlphaNumeric,
                        -> mutex.lock()
                }
            }

            val confirmation = awaitConfirmation(
                request = ProvisioningRequest.Confirmation(
                    confirmation = provisioningData.provisionerConfirmation
                )
            )
            provisioningData.onDeviceConfirmationReceived(confirmation = confirmation)

            val random = awaitRandom(
                request = ProvisioningRequest.Random(
                    random = provisioningData.provisionerRandom
                )
            )
            provisioningData.onDeviceRandomReceived(random = random)

            require(provisioningData.checkIfConfirmationsMatch()) {
                logger?.e(LogCategory.PROVISIONING) { "Confirmations do not match" }
                emit(value = ProvisioningState.Failed(error = ConfirmationFailed()))
                throw ConfirmationFailed()
            }

            val data = ProvisioningRequest.Data(
                encryptedDataWithMic = provisioningData.encryptedProvisioningDataWithMic
            )
            logger?.v(LogCategory.PROVISIONING) { "Sending $data" }
            send(request = data)

            awaitComplete().also {
                emit(value = ProvisioningState.Complete)
                // If the node was reprovisioned we need to remove it from the network and add it
                // again.
                // In version 0.9.1 and before the behavior of reprovisioning node was as follows
                // - If the same node was reprovisioned, the library threw an error after
                //   provisioning was complete without adding it to the network
                // However, post 0.9.1 the library was explicitly remove any existing nodes upon
                // reprovisioning and adds the new node to the network.
                val node = Node(
                    name = unprovisionedDevice.name,
                    uuid = unprovisionedDevice.uuid,
                    deviceKey = provisioningData.deviceKey,
                    unicastAddress = configuration.unicastAddress,
                    elementCount = capabilities.numberOfElements,
                    assignedNetworkKey = configuration.networkKey,
                    security = provisioningData.security
                )
                meshNetwork.remove(uuid = node.uuid)
                meshNetwork.add(node = node)
            }

        } catch (error: RemoteError) {
            emit(ProvisioningState.Failed(error))
        }
    }

    /**
     * Waits for the capabilities response from the device.
     *
     * @param invite           Provisioning invite to send.
     * @param provisioningData The provisioning data to accumulate the received PDUs.
     * @throws InvalidPdu If the received PDU is invalid.
     * @throws NoAddressAvailable If the device does not have any unicast addresses available.
     */
    @Throws(InvalidPdu::class, NoAddressAvailable::class)
    private suspend fun awaitCapabilities(
        invite: ProvisioningRequest.Invite,
        provisioningData: ProvisioningData,
    ): ProvisioningCapabilities {
        logger?.v(LogCategory.PROVISIONING) { "Sending $invite" }
        send(request = invite)
            .also { provisioningData.accumulate(data = it) }
        val response = ProvisioningResponse.from(pdu = awaitBearerPdu().data)
            .apply {
                logger?.v(LogCategory.PROVISIONING) { "Received $this" }
                require(this is ProvisioningResponse.Capabilities) {
                    logger?.e(LogCategory.PROVISIONING) {
                        "Provisioning failed with error: ${InvalidPdu()}"
                    }
                    throw InvalidPdu()
                }
                provisioningData.accumulate(data = pdu.sliceArray(indices = 1 until pdu.size))
            } as ProvisioningResponse.Capabilities
        return response.capabilities
    }

    /**
     * Waits for the device's Public Key.
     *
     * @param request                Provisioner's Public Key.
     * @param provisioningData   Provisioning data to accumulate the received PDUs.
     * @throws RemoteError If the device returns an error.
     * @throws InvalidPdu If the received PDU is invalid.
     * @throws InvalidPublicKey If the device's Public Key is invalid.
     */
    @Throws(InvalidPdu::class, RemoteError::class, InvalidPublicKey::class)
    private suspend fun awaitProvisioneePublicKey(
        request: ProvisioningRequest.PublicKey,
        provisioningData: ProvisioningData,
    ): ByteArray {
        logger?.v(LogCategory.PROVISIONING) { "Sending $request" }
        send(request).also { provisioningData.accumulate(data = it) }
        val response = ProvisioningResponse.from(pdu = awaitBearerPdu().data).also { response ->
            logger?.v(LogCategory.PROVISIONING) { "Received $response" }
            if (response is ProvisioningResponse.Failed) {
                logger?.e(LogCategory.PROVISIONING) {
                    "Provisioning failed with error: ${response.error}"
                }
                throw RemoteError(response.error)
            }
            require(response is ProvisioningResponse.PublicKey) {
                throw InvalidPdu()
            }
            // Errata E1650 added an extra validation step to ensure the received public key is
            // the same as the provisioner's public key.
            require(!response.key.contentEquals(request.publicKey)) {
                throw InvalidPublicKey()
            }
        } as ProvisioningResponse.PublicKey
        return response.key
    }

    /**
     * Waits for the user to provide the authentication value.
     *
     * @param method                  Authentication method.
     * @param sizeInBytes             Size of auth value in bytes based on the algorithm
     * @param onAuthValueReceived     Lambda to be invoked upon receiving/generating auth value
     * @param mutex                   Mutex to unlock when the user has provided the authentication
     *                                value.
     * @throws InvalidOobValueFormat If the user provided an invalid
     */
    @Throws(InvalidOobValueFormat::class)
    private fun requestAuthentication(
        method: AuthenticationMethod,
        sizeInBytes: Int,
        onAuthValueReceived: (authValue: ByteArray) -> Unit,
        mutex: Mutex,
    ): AuthAction? = when (method) {
        AuthenticationMethod.NoOob -> {
            onAuthValueReceived(ByteArray(sizeInBytes) { 0x00 })
            null
        }

        AuthenticationMethod.StaticOob -> AuthAction.ProvideStaticKey(length = sizeInBytes) {
            require(it.size == sizeInBytes) {
                throw InvalidOobValueFormat()
            }
            onAuthValueReceived(it)
            mutex.unlock()
        }

        is AuthenticationMethod.OutputOob -> when (method.action) {
            OutputAction.OUTPUT_ALPHANUMERIC ->
                AuthAction.ProvideAlphaNumeric(method.length) {
                    val input = it.toByteArray(charset = Charsets.US_ASCII)
                    val authValue = input + ByteArray(size = sizeInBytes - input.size)
                    onAuthValueReceived(
                        authValue.sliceArray(0 until sizeInBytes)
                    )
                    mutex.unlock()
                }
            // BLINK,BEEP,VIBRATE,OUTPUT_NUMERIC
            else -> AuthAction.ProvideNumeric(method.length, method.action) {
                val input = it.toByteArray()
                val authValue = ByteArray(size = sizeInBytes - input.size) + input
                onAuthValueReceived(authValue)
                mutex.unlock()
            }
        }

        is AuthenticationMethod.InputOob -> when (method.action) {
            InputAction.INPUT_ALPHANUMERIC -> {
                AuthAction.DisplayAlphaNumeric(
                    text = AuthenticationMethod.randomAlphaNumeric(
                        length = method.length.toInt()
                    )
                ).also {
                    val input = it.text.toByteArray(charset = Charsets.US_ASCII)
                    val authValue = input + ByteArray(size = sizeInBytes - input.size)
                    onAuthValueReceived(authValue)
                }
            }
            // PUSH, TWIST, INPUT_NUMERIC
            else -> AuthAction.DisplayNumber(
                number = AuthenticationMethod.randomInt(
                    length = method.length.toInt()
                ).toUInt(),
                action = method.action
            ).also {
                val input = it.number.toByteArray()
                val authValue = ByteArray(size = sizeInBytes - input.size) + input
                onAuthValueReceived(authValue)
            }
        }
    }

    /**
     * Waits for the input complete response from the device.
     *
     * @throws RemoteError If the device returns an error.
     */
    @Throws(RemoteError::class)
    private suspend fun awaitInputComplete(): ProvisioningResponse.InputComplete {
        val response = ProvisioningResponse.from(
            pdu = awaitBearerPdu().data
        ).apply {
            logger?.v(LogCategory.PROVISIONING) { "Received $this" }
            if (this is ProvisioningResponse.Failed) {
                logger?.e(LogCategory.PROVISIONING) {
                    "Provisioning failed with error: $error"
                }
                throw RemoteError(error)
            }
        } as ProvisioningResponse.InputComplete
        return response
    }

    /**
     * Waits for the confirmation response from the device.
     *
     * @param request  Confirmation value to send to the device.
     * @throws RemoteError If the device returns an error.
     */
    @Throws(RemoteError::class)
    private suspend fun awaitConfirmation(
        request: ProvisioningRequest.Confirmation,
    ): ByteArray {
        logger?.v(LogCategory.PROVISIONING) { "Sending $request" }
        send(request)
        val response = ProvisioningResponse.from(
            pdu = awaitBearerPdu().data
        ).also {
            logger?.v(LogCategory.PROVISIONING) { "Received $it" }
            if (it is ProvisioningResponse.Failed) {
                logger?.e(LogCategory.PROVISIONING) {
                    "Provisioning failed with error: $it.error"
                }
                throw RemoteError(it.error)
            }
        } as ProvisioningResponse.Confirmation
        // Errata E1650 added an extra validation step to ensure the received public key is
        // the same as the provisioner's public key.
        require(!response.confirmation.contentEquals(request.confirmation)) {
            throw InvalidConfirmation()
        }
        return response.confirmation
    }

    /**
     * Waits for the provisioning random response from the device.
     *
     * @param request  Random value to send to the device.
     * @throws RemoteError If the device returns an error.
     */
    @Throws(RemoteError::class)
    private suspend fun awaitRandom(request: ProvisioningRequest.Random): ByteArray {
        logger?.v(LogCategory.PROVISIONING) { "Sending $request" }
        send(request)
        return (ProvisioningResponse.from(
            pdu = awaitBearerPdu().data
        ).apply {
            logger?.v(LogCategory.PROVISIONING) { "Received $this" }
            if (this is ProvisioningResponse.Failed) {
                logger?.e(LogCategory.PROVISIONING) {
                    "Provisioning failed with error: $error"
                }
                throw RemoteError(error)
            }
        } as ProvisioningResponse.Random).random
    }

    /**
     * Waits for the provisioning complete response from the device.
     *
     * @throws RemoteError If the device returns an error.
     */
    @Throws(RemoteError::class)
    private suspend fun awaitComplete() = ProvisioningResponse.from(
        pdu = awaitBearerPdu().data
    ).apply {
        logger?.v(LogCategory.PROVISIONING) { "Received $this" }
        if (this is ProvisioningResponse.Failed) {
            logger?.e(LogCategory.PROVISIONING) {
                "Provisioning failed with error: $error"
            }
            throw RemoteError(error)
        }
    } as ProvisioningResponse.Complete

    private fun isPublicKeyValid(provisionerPublicKey: ByteArray, devicePublicKey: ByteArray) {
        require(!provisionerPublicKey.contentEquals(devicePublicKey)) {
            throw InvalidPublicKey()
        }
    }

    /**
     * Checks if the unicast address valid.
     *
     * @param unicastAddress     Unicast address to be checked.
     * @param numberOfElements   Number of elements in the node.
     * @return true if the address is valid, false otherwise.
     */
    fun isUnicastAddressValid(unicastAddress: UnicastAddress, numberOfElements: Int) =
        meshNetwork.localProvisioner?.let { provisioner ->
            val range = UnicastRange(unicastAddress, numberOfElements)
            meshNetwork.isAddressRangeAvailable(range) && provisioner.hasAllocatedRange(range)
        } ?: false

    /**
     * Sends the provisioning request to the device over the Bearer specified in the
     * constructor.
     *
     * @param request Provisioning request to be sent.
     */
    private suspend fun send(request: ProvisioningRequest): ByteArray {
        bearer.send(request)
        return request.pdu.let { it.sliceArray(1 until it.size) }
    }

    /**
     * Awaits and returns the first Provisioning PDU received over the Bearer.
     *
     * @return First Provisioning PDU received over the Bearer.
     */
    private suspend fun awaitBearerPdu(): Pdu = bearer.pdus.first {
        it.type == PduType.PROVISIONING_PDU
    }
}