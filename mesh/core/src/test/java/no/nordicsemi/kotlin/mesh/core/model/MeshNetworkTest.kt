package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.coroutines.runBlocking
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class MeshNetworkTest {

    private val networkManager = MeshNetworkManager(
        storage = TestStorage(),
        secureProperties = TestPropertiesStorage(),
        ioDispatcher = kotlinx.coroutines.Dispatchers.IO
    )
    private var meshNetwork: MeshNetwork
    private val group = Group("Test Group", GroupAddress(0xD000u))
    private val scene = Scene("Test Scene", 0x000Au)

    val jsonBytes = this.javaClass.classLoader
        .getResourceAsStream("cdb_json.json")?.readAllBytes()

    init {
        runBlocking {
            meshNetwork = networkManager.import(jsonBytes!!)
        }
    }

    @Test
    fun testNextAvailableUnicastAddressEmptyNetwork() {
        val meshNetwork = MeshNetwork(
            name = "Test Network",
            networkKeys = mutableListOf(NetworkKey())
        )
        val provisioner = Provisioner(uuid = Uuid.random()).apply {
            this.network = meshNetwork
            this.name = "Test Provisioner"
            this.allocate(
                range = UnicastAddress(address = 0x0001u)..UnicastAddress(address = 0x7F00u)
            )
        }

        val address = meshNetwork.nextAvailableUnicastAddress(
            elementCount = 6,
            provisioner = provisioner
        )
        assertNotNull(address)
        assertEquals(address, UnicastAddress(address = 1))
    }

    @Test
    fun testNextAvailableUnicastAddressBasicNetwork() {
        val meshNetwork = MeshNetwork(
            name = "Test Network",
            networkKeys = mutableListOf(NetworkKey())
        ).apply {
            add(node = Node(name = "Node 0", address = 1, elements = 9))
            add(node = Node(name = "Node 1", address = 10, elements = 9))
            add(node = Node(name = "Node 2", address = 20, elements = 9))
            add(node = Node(name = "Node 3", address = 30, elements = 9))
        }

        val provisioner = Provisioner(
            name = "Test Provisioner",
            allocatedUnicastRanges = mutableListOf(UnicastRange(100, 200))
        ).apply { this.network = meshNetwork }

        val address = meshNetwork.nextAvailableUnicastAddress(
            elementCount = 6,
            provisioner = provisioner
        )
        assertNotNull(address)
        assertEquals(expected = UnicastAddress(address = 100), actual = address)
    }

    @Test
    fun testNextAvailableUnicastAddressWithOffset() {
        val meshNetwork = MeshNetwork(
            name = "Test Network",
            networkKeys = mutableListOf(NetworkKey())
        ).apply {
            add(node = Node(name = "Node 0", address = 1, elements = 9))
            add(node = Node(name = "Node 1", address = 10, elements = 9))
            add(node = Node(name = "Node 2", address = 20, elements = 9))
            add(node = Node(name = "Node 3", address = 30, elements = 9))
            add(node = Node(name = "Node 4", address = 115, elements = 2))
        }

        val provisioner = Provisioner(
            name = "Test Provisioner",
            allocatedUnicastRanges = mutableListOf(UnicastRange(100, 200))
        ).apply { this.network = meshNetwork }

        val address1 = meshNetwork.nextAvailableUnicastAddress(
            offset = UnicastAddress(110),
            elementCount = 3,
            provisioner = provisioner
        )

        assertNotNull(address1)
        assertEquals(expected = UnicastAddress(address = 110), actual = address1)

        val address2 = meshNetwork.nextAvailableUnicastAddress(
            offset = UnicastAddress(110),
            elementCount = 6,
            provisioner = provisioner
        )

        assertNotNull(address2)
        assertEquals(expected = UnicastAddress(address = 117), actual = address2)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testNextAvailableUnicastAddressComplexNetwork() {
        val meshNetwork = MeshNetwork(
            name = "Test Network",
            networkKeys = mutableListOf(NetworkKey())
        ).apply {
            add(node = Node(name = "Node 0", address = 1, elements = 9))
            add(node = Node(name = "Node 1", address = 10, elements = 9))
            add(node = Node(name = "Node 2", address = 20, elements = 9))
            add(node = Node(name = "Node 3", address = 30, elements = 9))
            add(node = Node(name = "Node 4", address = 103, elements = 5))
        }

        val oldNode = Node(name = "Node 5", address = 110, elements = 2)
        meshNetwork.add(node = oldNode)
        assertFalse {
            meshNetwork.isAddressAvailable(address = UnicastAddress(111), elementCount = 2)
        }
        assertTrue {
            meshNetwork.isAddressAvailable(address = UnicastAddress(111), node = oldNode)
        }
        meshNetwork.remove(node = oldNode)

        val provisioner = Provisioner(
            name = "Test Provisioner",
            allocatedUnicastRanges = mutableListOf(UnicastRange(100, 120))
        ).apply { this.network = meshNetwork }

        val address = meshNetwork.nextAvailableUnicastAddress(
            elementCount = 6, provisioner = provisioner
        )

        assertNotNull(address)
        assertEquals(expected = UnicastAddress(address = 112), actual = address)

        // 110 and 111 cannot be assigned until IV Index changes by 2.
        assertFalse {
            meshNetwork.isAddressAvailable(address = UnicastAddress(110), elementCount = 3)
        }
        assertTrue {
            meshNetwork.isAddressAvailable(address = UnicastAddress(112), elementCount = 10)
        }

        meshNetwork.ivIndex = IvIndex(index = 1u, isIvUpdateActive = true)
        assertFalse {
            meshNetwork.isAddressAvailable(address = UnicastAddress(110), elementCount = 3)
        }

        meshNetwork.ivIndex = IvIndex(index = 1u, isIvUpdateActive = false)
        assertFalse {
            meshNetwork.isAddressAvailable(address = UnicastAddress(110), elementCount = 3)
        }

        // When IV Index is incremented by 2 since a Node was removed, the unicast Addresses may be
        // reused.
        meshNetwork.ivIndex = IvIndex(index = 2u, isIvUpdateActive = true)
        assertTrue {
            meshNetwork.isAddressAvailable(address = UnicastAddress(110), elementCount = 3)
        }
        assertEquals(
            expected = UnicastAddress(address = 108),
            actual = meshNetwork.nextAvailableUnicastAddress(
                elementCount = 6,
                provisioner = provisioner
            )
        )
    }

    @Test
    fun testNextAvailableUnicastAddressAdvancedNetwork() {
        val meshNetwork = MeshNetwork(
            name = "Test Network",
            networkKeys = mutableListOf(NetworkKey())
        ).apply {
            add(node = Node(name = "Node 0", address = 1, elements = 10))
            add(node = Node(name = "Node 1", address = 12, elements = 18))
            add(node = Node(name = "Node 2", address = 30, elements = 11))
            add(node = Node(name = "Node 3", address = 55, elements = 10))
            add(node = Node(name = "Node 4", address = 65, elements = 5))
            add(node = Node(name = "Node 5", address = 73, elements = 5))
        }

        val provisioner = Provisioner(
            name = "Test Provisioner",
            allocatedUnicastRanges = mutableListOf(
                UnicastRange(8, 38),
                UnicastRange(50, 100),
                UnicastRange(120, 150)
            )
        ).apply { this.network = meshNetwork }

        val address = meshNetwork.nextAvailableUnicastAddress(
            elementCount = 6, provisioner = provisioner
        )
        assertNotNull(address)
        assertEquals(expected = UnicastAddress(address = 78), actual = address)
    }

    @Test
    fun testNextAvailableUnicastAddressOne() {
        val meshNetwork = MeshNetwork(
            name = "Test Network",
            networkKeys = mutableListOf(NetworkKey())
        ).apply {
            add(node = Node(name = "Node 0", address = 1, elements = 10))
            add(node = Node(name = "Node 1", address = 12, elements = 18))
            add(node = Node(name = "Node 2", address = 30, elements = 11))
            add(node = Node(name = "Node 3", address = 55, elements = 10))
            add(node = Node(name = "Node 4", address = 65, elements = 5))
            add(node = Node(name = "Node 5", address = 73, elements = 5))
        }

        val provisioner = Provisioner(
            name = "Test Provisioner",
            allocatedUnicastRanges = mutableListOf(
                UnicastRange(8, 38),
                UnicastRange(50, 80)
            )
        ).apply { this.network = meshNetwork }

        val address = meshNetwork.nextAvailableUnicastAddress(
            elementCount = 1, provisioner = provisioner
        )
        assertNotNull(address)
        assertEquals(expected = UnicastAddress(address = 11), actual = address)
    }

    @Test
    fun testNextAvailableUnicastAddressNone() {
        val meshNetwork = MeshNetwork(
            name = "Test Network",
            networkKeys = mutableListOf(NetworkKey())
        ).apply {
            add(node = Node(name = "Node 0", address = 1, elements = 10))
            add(node = Node(name = "Node 1", address = 12, elements = 18))
            add(node = Node(name = "Node 2", address = 30, elements = 11))
            add(node = Node(name = "Node 3", address = 55, elements = 10))
            add(node = Node(name = "Node 4", address = 65, elements = 5))
            add(node = Node(name = "Node 5", address = 73, elements = 5))
        }

        val provisioner = Provisioner(
            name = "Test Provisioner",
            allocatedUnicastRanges = mutableListOf(
                UnicastRange(8, 38),
                UnicastRange(50, 80)
            )
        ).apply { this.network = meshNetwork }

        val address = meshNetwork.nextAvailableUnicastAddress(
            elementCount = 6, provisioner = provisioner
        )
        assertNull(address)
    }

    @Test
    fun testAddGroup() {
        meshNetwork.add(group)
        assertTrue(meshNetwork._groups.any { it.address == group.address })
    }

    @Test
    fun testRemoveGroup() {
        meshNetwork.add(group = group)
        meshNetwork.remove(group = group)
        assertEquals(
            expected = true,
            actual = meshNetwork._groups.none { it.address == group.address })
    }

    @Test
    fun testNextAvailableGroup() {
        val expectedGroupAddress = GroupAddress(0xC000u)
        val provisioner = meshNetwork._provisioners.last()
        val actualGroupAddress = meshNetwork.nextAvailableGroup(provisioner)
        assertEquals(expectedGroupAddress, actualGroupAddress)
    }

    @Test
    fun testAddScene() {
        meshNetwork.add(scene)
        assertTrue(meshNetwork._scenes.any { it.number == scene.number })
    }

    @Test
    fun testRemoveScene() {
        meshNetwork.add(scene)
        meshNetwork.remove(scene)
        assertEquals(true, meshNetwork._scenes.none { it.number == scene.number })
    }

    @Test
    fun testNextAvailableScene() {
        val expectedSceneNumber: SceneNumber = 1u
        val provisioner = meshNetwork._provisioners.last()
        val actualSceneNumber = meshNetwork.nextAvailableScene(provisioner)
        assertEquals(expectedSceneNumber, actualSceneNumber)
    }

    @Test
    fun testMoveProvisionerFromTo() {
        meshNetwork.add(provisioner = Provisioner(Uuid.random()).apply {
            this.network = meshNetwork
            this.name = "Test provisioner"
            this.allocate(UnicastAddress(0x7000u)..UnicastAddress(0x7F00u))
        })
        val provisioner = meshNetwork._provisioners.first()
        val to = meshNetwork._provisioners.size - 1
        meshNetwork.moveProvisioner(meshNetwork.provisioners.indexOf(provisioner), to)
        assertEquals(to, meshNetwork._provisioners.indexOf(provisioner))
    }

    @Test
    fun testMoveProvisionerTo() {
        meshNetwork.add(provisioner = Provisioner(Uuid.random()).apply {
            this.network = meshNetwork
            this.name = "Test provisioner"
            this.allocate(UnicastAddress(0x7000u)..UnicastAddress(0x7F00u))
        })
        val provisioner = meshNetwork._provisioners.first()
        val to = meshNetwork._provisioners.size - 1
        meshNetwork.move(provisioner, to)
        assertEquals(to, meshNetwork._provisioners.indexOf(provisioner))
    }

    @Test
    fun testAssign() {
        val expectedAddress = UnicastAddress(255u)
        val provisioner = meshNetwork._provisioners.first()
        provisioner.assign(expectedAddress)
        assertEquals(
            expectedAddress,
            meshNetwork.node(provisioner.uuid)?.primaryUnicastAddress
        )
    }

    @Test
    fun testDisableConfigurationCapabilities() {
        val provisioner = meshNetwork._provisioners.first()
        meshNetwork.disableConfigurationCapabilities(provisioner)
        assertEquals(null, meshNetwork.node(provisioner.uuid)?.primaryUnicastAddress)
    }

    @Test
    fun testIsRangeAvailableForAllocation() {
        val range = UnicastAddress(0x7000u)..UnicastAddress(0x7F00u)
        val provisioner = Provisioner(Uuid.random()).apply {
            this.network = meshNetwork
            this.name = "Test provisioner"
        }
        assertEquals(true, provisioner.isRangeAvailableForAllocation(range))
    }

    @Test
    fun testAreRangesAvailableForAllocation() {
        val range = UnicastAddress(0x7000u)..UnicastAddress(0x7F00u)
        meshNetwork.provisioners.first().allocate(range)
        val provisioner = Provisioner(Uuid.random()).apply {
            this.network = meshNetwork
            this.name = "Test provisioner"
        }
        assertEquals(
            false,
            provisioner.areRangesAvailableForAllocation(listOf(range))
        )
    }
}