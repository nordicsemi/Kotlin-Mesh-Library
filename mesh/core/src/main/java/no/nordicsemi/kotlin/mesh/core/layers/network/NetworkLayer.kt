@file:Suppress("MemberVisibilityCanBePrivate", "unused", "UNUSED_PARAMETER")

package no.nordicsemi.kotlin.mesh.core.layers.network

import kotlinx.coroutines.sync.Mutex
import no.nordicsemi.kotlin.mesh.bearer.BearerError
import no.nordicsemi.kotlin.mesh.bearer.PduType
import no.nordicsemi.kotlin.mesh.bearer.gatt.GattBearer
import no.nordicsemi.kotlin.mesh.core.ProxyFilter
import no.nordicsemi.kotlin.mesh.core.layers.NetworkManager
import no.nordicsemi.kotlin.mesh.core.layers.ReceivedMessage
import no.nordicsemi.kotlin.mesh.core.layers.lowertransport.AccessMessage
import no.nordicsemi.kotlin.mesh.core.layers.lowertransport.ControlMessage
import no.nordicsemi.kotlin.mesh.core.layers.lowertransport.LowerTransportPdu
import no.nordicsemi.kotlin.mesh.core.layers.lowertransport.SegmentedMessage
import no.nordicsemi.kotlin.mesh.core.messages.proxy.FilterStatus
import no.nordicsemi.kotlin.mesh.core.messages.proxy.ProxyConfigurationMessage
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.KeyDistribution
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.UsingNewKeys
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import no.nordicsemi.kotlin.mesh.core.model.boundTo
import no.nordicsemi.kotlin.mesh.core.model.maxUnicastAddress
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.Logger
import kotlin.concurrent.timer
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

/**
 * Network Layer of the mesh networking stack
 *
 * @property networkManager Network manager containing the different layers of the mesh networking
 *                          stack.
 * @constructor Constructs the network layer.
 */
internal class NetworkLayer(private val networkManager: NetworkManager) {
    private val meshNetwork: MeshNetwork
        get() = networkManager.meshNetwork
    private val logger: Logger?
        get() = networkManager.logger
    private val mutex = Mutex()
    private val secureProperties
        get() = networkManager.securePropertiesStorage
    internal var proxyNetworkKey: NetworkKey? = null
    private val networkMessageCache = mutableMapOf<ByteArray, Any?>()

    /**
     * This method handles the received PDU of given type and passes it to Upper Transport Layer.
     *
     * @param incomingPdu  Data received.
     * @param type         PDU type.
     */
    suspend fun handle(incomingPdu: ByteArray, type: PduType): ReceivedMessage? {
        // Discard provisioning pdus as they are handled by the provisioning manager
        if (type == PduType.PROVISIONING_PDU) return null

        // Secure Network Beacons can repeat whenever the device connects to a new Proxy.
        if (type != PduType.MESH_BEACON) {
            // Ensure the PDU has not been handled already.
            require(networkMessageCache[incomingPdu] == null) {
                logger?.d(LogCategory.NETWORK) { "PDU already handled" }
                return null
            }
            networkMessageCache[incomingPdu] = null
        }

        // Try decoding the pdu.
        when (type) {
            PduType.NETWORK_PDU -> {
                val networkPdu = NetworkPduDecoder.decode(
                    pdu = incomingPdu,
                    pduType = type,
                    meshNetwork = meshNetwork
                )
                return if (networkPdu != null) {
                    logger?.i(LogCategory.NETWORK) { "$networkPdu received" }
                    networkManager.lowerTransportLayer.handle(networkPdu = networkPdu)?.let {
                        ReceivedMessage(
                            source = networkPdu.source,
                            destination = networkPdu.destination,
                            message = it
                        )
                    }
                } else {
                    logger?.w(LogCategory.NETWORK) { "Failed to decrypt network pdu" }
                    null
                }
            }
            // This condition always return null because mesh beacons are not responded to by the
            // lib as of now.
            PduType.MESH_BEACON -> {
                NetworkBeaconPduDecoder.decode(pdu = incomingPdu, meshNetwork = meshNetwork)?.let {
                    logger?.i(LogCategory.NETWORK) {
                        "$it received, authenticated using key: ${it.networkKey}"
                    }
                    // TODO possible late init property initialization error
                    try {
                        handle(networkBeacon = it)
                    } catch (e: Exception) {
                        logger?.e(LogCategory.NETWORK) { "Failed to handle beacon $e" }
                    }
                    return null
                }
                UnprovisionedDeviceBeaconDecoder.decode(pdu = incomingPdu)?.let {
                    logger?.i(LogCategory.NETWORK) { "$it received" }
                    handle(beacon = it)
                    return null
                }
                logger?.w(LogCategory.NETWORK) { "Failed to decrypt mesh beacon pdu" }
                return null
            }

            PduType.PROXY_CONFIGURATION -> {
                return NetworkPduDecoder.decode(
                    pdu = incomingPdu,
                    pduType = type,
                    meshNetwork = meshNetwork
                )?.let {
                    logger?.i(LogCategory.NETWORK) { "$it received" }
                    handle(proxyPdu = it)
                } ?: run {
                    logger?.w(LogCategory.NETWORK) { "Unable to decode network pdu" }
                    null
                }
            }

            else -> return null
        }
    }

    /**
     * This method tries to send the Lower Transport Message of given type to the given destination
     * address. If the local Provisioner does not exist, or does not have Unicast Address assigned,
     * this method does nothing.
     *
     * @param pdu       Lower Transport PDU to be sent.
     * @param type      PDU type.
     * @param ttl       Initial TTL (Time To Live) value of the message.
     * @throws BearerError.Closed when the bearer is closed.
     */
    @Throws(BearerError.Closed::class)
    suspend fun send(pdu: LowerTransportPdu, type: PduType, ttl: UByte) {
        // Compared to the iOS implementation, we check if a message is an AccessMessage and is not
        // a SegmentedMessage. This allows us to reuse the sequence number from the upper transport
        // layer for non-segmented Access Messages. However, if the PDU is a SegmentedMessage, each
        // segment will have its own sequence number incremented.
        val sequence = when {
            pdu is AccessMessage && pdu !is SegmentedMessage -> pdu.sequence
            else -> nextSequenceNumber(address = pdu.source as UnicastAddress)
        }
        val networkPdu = NetworkPduDecoder.encode(
            lowerTransportPdu = pdu,
            pduType = type,
            sequence = sequence,
            ttl = ttl
        )
        logger?.i(LogCategory.NETWORK) {
            "Sending $networkPdu (encrypted using ${networkPdu.key.name})"
        }
        // Loopback interface
        if (shouldLoopback(networkPdu = networkPdu)) {
            networkManager.handle(incomingPdu = networkPdu.pdu, type = type)
            // Messages sent with TTL = 1 will only be sent locally.
            require(ttl != 1.toUByte()) { return }
            if (isLocalUnicastAddress(address = networkPdu.destination.address)) {
                // No need to send messages targeting local Unicast Addresses.
                return
            }
            // If the message was sent locally, don't report Bearer closed error.
            try {
                networkManager.bearer?.send(pdu = networkPdu.pdu, type = type)
            } catch (e: Exception) {
                // Ignore the error because the message was sent locally.
            }
        } else {
            // Messages sent with TTL = 1 will only be sent locally.
            require(ttl != 1.toUByte()) { return }
            try {
                networkManager.bearer?.send(pdu = networkPdu.pdu, type = type)
                    ?: throw BearerError.Closed()
            } catch (e: Exception) {
                if (e is BearerError.Closed) {
                    proxyNetworkKey = null
                }
                throw e
            }
        }

        // Unless a GATT Bearer is used, the Network PDUs should be sent multiple times if
        // Network Transmit has been set for the local Provisioner's Node
        if (type == PduType.NETWORK_PDU && networkManager.bearer is GattBearer) {
            meshNetwork.localProvisioner?.node?.networkTransmit
                ?.takeIf { it.count > 1 }
                ?.let { networkTransmit ->
                    var count = networkTransmit.count
                    timer(period = networkTransmit.intervalAsMilliseconds) {
                        // networkManager.transmitter?.send(pdu = networkPdu.pdu, type = type)
                        count -= 1
                        if (count == 0)
                            cancel()
                    }
                }
        }
    }

    /**
     * Sends the Proxy Configuration Message. The Proxy Filter object will be notified about the
     * success or a failure.
     *
     * @param message The Proxy Configuration message to be sent.
     */
    suspend fun send(message: ProxyConfigurationMessage): ProxyConfigurationMessage? {
        proxyNetworkKey?.let { networkKey ->
            val source = meshNetwork.localProvisioner?.node?.primaryUnicastAddress
                ?: UnicastAddress(address = maxUnicastAddress)
            logger?.i(LogCategory.PROXY) {
                "Sending $message from: ${source.toHexString()} to 0000"
            }
            val pdu = ControlMessage.init(
                message = message,
                source = source,
                networkKey = networkKey,
                ivIndex = meshNetwork.ivIndex
            )
            logger?.i(LogCategory.NETWORK) { "Sending $pdu" }

            try {
                send(pdu = pdu, type = PduType.PROXY_CONFIGURATION, ttl = pdu.ttl)
                networkManager.proxy.onManagerDidDeliverMessage(message = message)
                return networkManager.awaitProxyMessageResponse()
                    ?.message as ProxyConfigurationMessage
            } catch (exception: Exception) {
                if (exception is BearerError.Closed) {
                    proxyNetworkKey = null
                }
                networkManager.proxy.onManagerFailedToDeliverMessage(
                    message = message,
                    error = exception
                )
            }
        } ?: networkManager.proxy.onManagerFailedToDeliverMessage(
            message = message,
            error = BearerError.Closed()
        )
        return null
    }

    /**
     * Returns the next outgoing sequence number for the given local source address.
     *
     * @param address Local source address.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun nextSequenceNumber(address: UnicastAddress) =
        secureProperties.nextSequenceNumber(uuid = meshNetwork.uuid, address = address)

    /**
     * This method handles the Unprovisioned Device beacon. The current implementation does nothing,
     * as remote provisioning is currently not supported.
     *
     * @param beacon Received Unprovisioned Device beacon.
     *
     */
    private fun handle(beacon: UnprovisionedDeviceBeacon) {
        // TODO Handle unprovisioned device beacon
    }

    /**
     * This method handles PDUs containing network state.
     *
     * As of Mesh Protocol 1.1 these are Secure Network beacons and Private beacons. These beacons
     * will set the IV Index and IV Update Active flag and change the Key Refresh Phase based on the
     * information specified in them.
     */
    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    private suspend fun handle(networkBeacon: NetworkBeaconPdu) {
        // The network key the beacon was authenticated with.
        val networkKey = networkBeacon.networkKey

        if (meshNetwork.primaryNetworkKey != null && networkKey.isSecondary) {
            logger?.w(LogCategory.NETWORK) {
                "Discarding beacon for secondary network (key index: ${networkKey.index})"
            }

            if (proxyNetworkKey == null) {
                updateProxyFilter(networkKey)
                return
            }
        }

        val lastIvIndex = secureProperties.ivIndex(uuid = meshNetwork.uuid)
        val lastTransitionDate = lastIvIndex.transitionDate
        val isIvRecoveryActive = lastIvIndex.isIvUpdateActive

        val isIvTestModeActive = networkManager.networkParameters.ivUpdateTestMode
        val flag = networkManager.networkParameters.allowIvIndexRecoveryOver42

        if (networkBeacon.canOverWrite(
                target = lastIvIndex,
                updatedAt = lastTransitionDate,
                isIvRecoveryActive = isIvRecoveryActive,
                isIvTestModeActive = isIvTestModeActive,
                ivRecoveryOver42Allowed = flag
            )
        ) {
            meshNetwork.ivIndex = networkBeacon.ivIndex
            if (meshNetwork.ivIndex.index > lastIvIndex.index) {
                logger?.i(LogCategory.NETWORK) { "Applying ${meshNetwork.ivIndex}" }
            }
            meshNetwork.let {
                if (it.localProvisioner?.node != null &&
                    it.ivIndex.transmitIvIndex > lastIvIndex.transmitIvIndex
                ) {
                    logger?.i(LogCategory.NETWORK) { "Resetting local sequence numbers to 0" }
                    secureProperties.resetSequenceNumber(
                        uuid = meshNetwork.uuid,
                        address = it.localProvisioner!!.node!!._primaryUnicastAddress
                    )
                }
            }

            // iOS Lib stores iv index, transition date and the recovery flag separately.
            // According to the sample app implementation the whole iv index is stored after the
            // if statement below.
            if (lastIvIndex != meshNetwork.ivIndex) {
                meshNetwork.ivIndex = meshNetwork.ivIndex.copy(transitionDate = Clock.System.now())
                    .apply {
                        ivRecoveryFlag =
                            (index > (lastIvIndex.index + 1u)) &&
                                    !networkBeacon.ivIndex.isIvUpdateActive
                    }
            }
            // Store the last IV Index
            secureProperties.storeIvIndex(uuid = meshNetwork.uuid, ivIndex = meshNetwork.ivIndex)

            // If the Key Refresh procedure is in progress, and the new Network Key has already been
            // set, the key refresh flag indicates switching to phase 2
            if (networkKey.phase is KeyDistribution &&
                networkBeacon.validForKeyRefreshProcedure &&
                !networkBeacon.keyRefreshFlag
            ) networkKey.phase = UsingNewKeys

            // if the Key Refresh Procedure is in Phase 2, and the key refresh flag is set to false.
            if (networkKey.phase is UsingNewKeys &&
                networkBeacon.validForKeyRefreshProcedure &&
                networkBeacon.keyRefreshFlag
            ) {
                // Revoke the old network key
                networkKey.oldKey = null // This will set the phase to NormalOperation
                // ...and old application keys bound to it
                meshNetwork.applicationKeys.boundTo(networkKey).forEach { it.oldKey = null }
            }

        } else if (networkBeacon.ivIndex != lastIvIndex.previous) {
            val numberOfHoursSinceData = "${(Clock.System.now() - lastTransitionDate).inWholeHours}"
            logger?.w(LogCategory.NETWORK) {
                "Discarding beacon (${networkBeacon.ivIndex}, " +
                        "last ${lastIvIndex}, changed $numberOfHoursSinceData hours ago, " +
                        "test mode: ${networkManager.networkParameters.ivUpdateTestMode})"
            }
            return
        } // else,

        // The beacon was sent by a Node with a previous IV Index, that was not yet transition to
        // the one the local node has. Such an IV Index is still valid, at least for sometime.
        updateProxyFilter(networkKey)
    }

    /**
     * Updates the information about the Network Key known to the current Proxy Server. The Network
     * Key is required to send proxy Configuration Messages that can be decoded by the connected
     * Proxy.
     *
     * For new Proxy connections this method also initiates the Proxy Filter with preset.
     *
     * @param networkKey The Network Key known to the connected Proxy Server.
     */
    private suspend fun updateProxyFilter(networkKey: NetworkKey) {
        val justConnected = proxyNetworkKey == null

        // Keep the primary Network Key or the most recently received one from the connected Proxy
        // Server. This is to make sure (almost) that the Proxy Configuration messages are sent
        // encrypted with a key known to this Node.
        proxyNetworkKey = networkKey

        if (justConnected) {
            networkManager.proxy.onNewProxyConnected()
        }
    }

    /**
     * Handles the received Proxy Configuration PDU. This method parses the payload and instantiates
     * a message class. The message is passed to the [ProxyFilter] for processing.
     *
     * @param proxyPdu Received Proxy Configuration PDU.
     */
    private suspend fun handle(proxyPdu: NetworkPdu): ReceivedMessage? {
        val payload = proxyPdu.transportPdu
        require(payload.size > 1) { return null }

        val controlMessage = runCatching { ControlMessage.init(proxyPdu) }.getOrElse {
            logger?.w(LogCategory.NETWORK) { "Failed to decrypt proxy PDU: $it" }
            return null
        }
        logger?.i(LogCategory.NETWORK) {
            "$controlMessage received (decrypted using key: ${controlMessage.networkKey.name})"
        }

        return when (controlMessage.opCode) {
            FilterStatus.opCode -> {
                FilterStatus.init(parameters = controlMessage.upperTransportPdu)?.let { message ->
                    logger?.i(LogCategory.PROXY) {
                        "$message received from: ${
                            proxyPdu.source.address.toHexString(
                                format = HexFormat {
                                    number.prefix = "0x"
                                    upperCase = true
                                })
                        }, dest: ${
                            proxyPdu.destination.address.toHexString(
                                format = HexFormat {
                                    number.prefix = "0x"
                                    upperCase = true
                                }
                            )
                        }"
                    }
                    // Look for the proxy Node.
                    val proxyNode = meshNetwork.node(proxyPdu.source as UnicastAddress)
                    networkManager.proxy.handle(message = message, proxy = proxyNode)
                    ReceivedMessage(
                        source = proxyPdu.source,
                        destination = proxyPdu.destination,
                        message = message
                    )
                }
            }

            else -> {
                logger?.w(LogCategory.PROXY) {
                    "Unknown Proxy Configuration message (opCode: ${
                        controlMessage.opCode.toHexString(
                            format = HexFormat {
                                number.prefix = "0x"
                                upperCase = true
                            }
                        )
                    })"
                }
                null
            }
        }
    }

    /**
     * Check whether the given address is an address of an element belonging to the local Node.
     *
     * @param address Address to check.
     * @return `true` if the address belongs to an element in the local Node or `false` otherwise.
     */
    internal fun isLocalUnicastAddress(address: UnicastAddress) = isLocalUnicastAddress(
        address = address.address
    )

    /**
     * Check whether the given address is an address of an element belonging to the local Node.
     *
     * @param address Address to check.
     * @return `true` if the address belongs to an element in the local Node or `false` otherwise.
     */
    internal fun isLocalUnicastAddress(address: Address) =
        meshNetwork.localProvisioner?.node?.containsElementWithAddress(address) == true

    /**
     * Check if the given [NetworkPdu] should loop back for local processing.
     *
     * @param networkPdu Network PDU to check.
     * @return `true` if the PDU should be looped back or `false` otherwise.
     */
    private fun shouldLoopback(networkPdu: NetworkPdu) = networkPdu.destination is GroupAddress ||
            networkPdu.destination is VirtualAddress ||
            networkPdu.destination.takeIf { it is UnicastAddress }?.let { address ->
                isLocalUnicastAddress(address as UnicastAddress)
            } ?: false

}

