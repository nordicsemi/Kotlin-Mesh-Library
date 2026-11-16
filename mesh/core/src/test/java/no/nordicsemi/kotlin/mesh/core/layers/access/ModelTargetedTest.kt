package no.nordicsemi.kotlin.mesh.core.layers.access

import kotlinx.coroutines.runBlocking
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import no.nordicsemi.kotlin.mesh.core.model.AllNodes
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.TestPropertiesStorage
import no.nordicsemi.kotlin.mesh.core.model.TestStorage
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests which messages are delivered to a model. In particular, a message sent to a unicast
 * address that is not the one of the model's element, e.g. to another local element, must be
 * ignored instead of failing.
 */
class ModelTargetedTest {

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

    // A model on the primary element that is subscribed to the groups 0xC023 and 0xC024.
    private val element = meshNetwork.nodes.first { it.name == "Bedside Light (Brian)" }
        .elements.first()
    private val model: Model = element.models.first { model ->
        model.subscribe.any { it == GroupAddress(0xC024u) }
    }

    @Test
    fun testMessageSentToTheElementIsDelivered() {
        assertTrue(model.isTargetedBy(destination = element.unicastAddress, element = element))
    }

    @Test
    fun testMessageSentToAnotherUnicastAddressIsIgnored() {
        val other = UnicastAddress((element.unicastAddress.address + 5u).toUShort())
        assertFalse(model.isTargetedBy(destination = other, element = element))
    }

    @Test
    fun testMessageSentToSubscribedGroupIsDelivered() {
        assertTrue(model.isTargetedBy(destination = GroupAddress(0xC024u), element = element))
    }

    @Test
    fun testMessageSentToGroupNotSubscribedToIsIgnored() {
        assertFalse(model.isTargetedBy(destination = GroupAddress(0xC025u), element = element))
    }

    @Test
    fun testMessageSentToAllNodesIsDeliveredToPrimaryElement() {
        assertTrue(model.isTargetedBy(destination = AllNodes, element = element))
    }

    @Test
    fun testMessageSentToSubscribedVirtualAddressIsDelivered() {
        val catFlap = meshNetwork.nodes.first { it.name == "Cat Flap" }.elements.first()
        val virtual = catFlap.models.first { model ->
            model.subscribe.any { it is VirtualAddress }
        }
        val address = virtual.subscribe.filterIsInstance<VirtualAddress>().first()
        assertTrue(virtual.isTargetedBy(destination = address, element = catFlap))
    }
}
