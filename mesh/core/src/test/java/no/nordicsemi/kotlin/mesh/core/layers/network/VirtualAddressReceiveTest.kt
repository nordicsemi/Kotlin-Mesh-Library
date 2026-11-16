package no.nordicsemi.kotlin.mesh.core.layers.network

import kotlinx.coroutines.runBlocking
import no.nordicsemi.kotlin.mesh.bearer.PduType
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import no.nordicsemi.kotlin.mesh.core.layers.AccessKeySet
import no.nordicsemi.kotlin.mesh.core.layers.access.AccessPdu
import no.nordicsemi.kotlin.mesh.core.layers.lowertransport.AccessMessage
import no.nordicsemi.kotlin.mesh.core.layers.uppertransport.UpperTransportPdu
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnOffSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.TestPropertiesStorage
import no.nordicsemi.kotlin.mesh.core.model.TestStorage
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests that messages sent to a virtual address can be received, i.e. that the destination of a
 * received PDU is resolved to the [VirtualAddress] of a group in the network, as the 16-bit value
 * carried by the PDU is not enough to create it.
 */
@OptIn(ExperimentalUuidApi::class)
class VirtualAddressReceiveTest {

    private val networkManager = MeshNetworkManager(
        storage = TestStorage(),
        secureProperties = TestPropertiesStorage(),
        ioDispatcher = kotlinx.coroutines.Dispatchers.IO
    )
    private val meshNetwork: MeshNetwork = runBlocking {
        networkManager.import(
            javaClass.classLoader.getResourceAsStream("cdb_json.json")!!.readAllBytes()
        )
    }

    // The network contains a group with a virtual address.
    private val virtualAddress = meshNetwork._groups
        .map { it.address }
        .filterIsInstance<VirtualAddress>()
        .first()
    private val applicationKey = meshNetwork.applicationKeys.first { it.boundNetKeyIndex == 0.toUShort() }
    private val networkKey = applicationKey.boundNetworkKey!!
    private val source = UnicastAddress(0x0001u)

    private fun accessMessage(destination: MeshAddress) = AccessMessage(
        source = source,
        destination = destination,
        networkKey = networkKey,
        ivIndex = meshNetwork.ivIndex.transmitIvIndex,
        upperTransportPdu = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8),
        transportMicSize = 4u,
        sequence = 1u,
        aid = applicationKey.aid
    )

    private fun encode(destination: MeshAddress) = NetworkPduDecoder.encode(
        lowerTransportPdu = accessMessage(destination),
        pduType = PduType.NETWORK_PDU,
        sequence = 1u,
        ttl = 5u
    )

    private fun decode(pdu: NetworkPdu) = NetworkPduDecoder.decode(
        pdu = pdu.pdu,
        pduType = PduType.NETWORK_PDU,
        meshNetwork = meshNetwork
    )

    @Test
    fun testNetworkPduToVirtualAddressIsDecoded() {
        val decoded = assertNotNull(decode(encode(virtualAddress)))
        assertEquals(virtualAddress, decoded.destination)
        assertEquals(source, decoded.source)
    }

    @Test
    fun testNetworkPduToUnknownVirtualAddressIsNotDecoded() {
        val unknown = VirtualAddress(uuid = Uuid.parse("00112233-4455-6677-8899-AABBCCDDEEFF"))
        assertNull(decode(encode(unknown)))
    }

    @Test
    fun testNetworkPduToGroupAddressIsDecoded() {
        val group = GroupAddress(0xC023u)
        assertEquals(group, assertNotNull(decode(encode(group))).destination)
    }

    @Test
    fun testNetworkPduToUnicastAddressIsDecoded() {
        val unicast = UnicastAddress(0x0002u)
        assertEquals(unicast, assertNotNull(decode(encode(unicast))).destination)
    }

    @Test
    fun testUpperTransportPduToVirtualAddressKeepsVirtualAddress() {
        val message = GenericOnOffSetUnacknowledged(on = true)
        val accessPdu = AccessPdu(
            message = message,
            userInitiated = true,
            source = source.address,
            destination = virtualAddress,
            opCode = GenericOnOffSetUnacknowledged.opCode,
            parameters = byteArrayOf(0x01, 0x05),
            accessPdu = byteArrayOf(0x82.toByte(), 0x03, 0x01, 0x05)
        )
        val upperTransportPdu = UpperTransportPdu.init(
            pdu = accessPdu,
            keySet = AccessKeySet(applicationKey = applicationKey),
            sequence = 1u,
            ivIndex = meshNetwork.ivIndex
        )

        val (decoded, _) = assertNotNull(
            UpperTransportPdu.decode(
                message = AccessMessage(pdu = upperTransportPdu, networkKey = networkKey),
                network = meshNetwork
            )
        )
        // The destination must be the virtual address, not a address created from its 16-bit value.
        assertEquals(virtualAddress, decoded.destination)
        assertEquals(accessPdu.accessPdu.toList(), decoded.accessPdu.toList())
    }
}
