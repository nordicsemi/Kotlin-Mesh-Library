package no.nordicsemi.kotlin.mesh.core.layers.access

import kotlinx.coroutines.runBlocking
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import no.nordicsemi.kotlin.mesh.core.model.AllFriends
import no.nordicsemi.kotlin.mesh.core.model.AllNodes
import no.nordicsemi.kotlin.mesh.core.model.AllProxies
import no.nordicsemi.kotlin.mesh.core.model.AllRelays
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tests which messages are delivered to a model. In particular, a message sent to a unicast
 * address that is not the one of the model's element, e.g. to another local element, must be
 * ignored instead of failing.
 */
@OptIn(ExperimentalUuidApi::class)
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

    // A model on a secondary element, which is not subscribed to anything.
    private val secondaryElement = meshNetwork.nodes.first { it.name == "Bedroom Light" }
        .elements[1]
    private val secondaryModel: Model = secondaryElement.models.first()

    @Test
    fun testMessageSentToAllNodesIsIgnoredBySecondaryElement() {
        assertFalse(
            secondaryModel.isTargetedBy(destination = AllNodes, element = secondaryElement)
        )
    }

    @Test
    fun testMessageSentToSecondaryElementIsDelivered() {
        assertTrue(
            secondaryModel.isTargetedBy(
                destination = secondaryElement.unicastAddress,
                element = secondaryElement
            )
        )
    }

    @Test
    fun testMessageSentToPrimaryElementIsIgnoredBySecondaryElement() {
        val primary = secondaryElement.parentNode!!.primaryElement!!
        assertFalse(
            secondaryModel.isTargetedBy(destination = primary.unicastAddress, element = secondaryElement)
        )
    }

    @Test
    fun testMessageSentToSecondaryElementIsIgnoredByPrimaryElement() {
        assertFalse(
            model.isTargetedBy(
                destination = element.parentNode!!.elements[0].unicastAddress.let {
                    UnicastAddress((it.address + 1u).toUShort())
                },
                element = element
            )
        )
    }

    @Test
    fun testMessageSentToUnsubscribedFixedGroupsIsIgnored() {
        listOf(AllProxies, AllFriends, AllRelays).forEach {
            assertFalse(model.isTargetedBy(destination = it, element = element))
        }
    }

    @Test
    fun testMessageSentToUnsubscribedVirtualAddressIsIgnored() {
        val address = VirtualAddress(Uuid.parse("00000000-0000-0000-0000-000000000001"))
        assertFalse(model.isTargetedBy(destination = address, element = element))
    }

    @Test
    fun testMessageSentToVirtualAddressIsIgnoredByModelSubscribedToAGroup() {
        // The model is subscribed to groups only, so a virtual address must not match.
        val address = VirtualAddress(Uuid.parse("EA57D9F1-975D-4EBD-A78A-37D189AC58F4"))
        assertFalse(model.isTargetedBy(destination = address, element = element))
    }

    @Test
    fun testMessageSentToGroupIsIgnoredByModelSubscribedToAVirtualAddress() {
        val catFlap = meshNetwork.nodes.first { it.name == "Cat Flap" }.elements.first()
        val virtual = catFlap.models.first { model -> model.subscribe.any { it is VirtualAddress } }
        assertFalse(virtual.isTargetedBy(destination = GroupAddress(0xC024u), element = catFlap))
    }

    @Test
    fun testMessageSentToUnsubscribedVirtualAddressIsIgnoredByModelSubscribedToAnother() {
        val catFlap = meshNetwork.nodes.first { it.name == "Cat Flap" }.elements.first()
        val virtual = catFlap.models.first { model -> model.subscribe.any { it is VirtualAddress } }
        val address = VirtualAddress(Uuid.parse("00000000-0000-0000-0000-000000000002"))
        assertFalse(virtual.isTargetedBy(destination = address, element = catFlap))
    }

    @Test
    fun testMessageSentToEveryUnicastAddressOfOtherNodesIsIgnored() {
        meshNetwork.nodes
            .filter { it.uuid != element.parentNode!!.uuid }
            .flatMap { it.elements }
            .forEach {
                assertFalse(model.isTargetedBy(destination = it.unicastAddress, element = element))
            }
    }
}
