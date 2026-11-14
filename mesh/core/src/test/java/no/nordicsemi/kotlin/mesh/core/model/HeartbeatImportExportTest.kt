package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.coroutines.runBlocking
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeartbeatImportExportTest {

    private fun newManager() = MeshNetworkManager(
        storage = TestStorage(),
        secureProperties = TestPropertiesStorage(),
        ioDispatcher = kotlinx.coroutines.Dispatchers.IO
    )

    private val fixture = javaClass.classLoader
        .getResourceAsStream("cdb_json.json")!!.readAllBytes()

    @Test
    fun `heartbeat publication and subscription survive export and import`() = runBlocking {
        val manager = newManager()
        val network = manager.import(fixture)
        val publisher = network.nodes[0]
        val subscriber = network.nodes[1]
        val publication = HeartbeatPublication(
            address = GroupAddress(0xC000u),
            period = 64u, // periodLog 7
            ttl = 5u,
            index = 1u,
            features = listOf(Relay(state = FeatureState.Enabled))
        ).apply { periodLog = 7u }
        val subscription = HeartbeatSubscription(
            source = publisher.primaryUnicastAddress,
            destination = UnicastAddress(0x0001u)
        )
        publisher.heartbeatPublication = publication
        subscriber.heartbeatSubscription = subscription

        val json = String(assertNotNull(manager.export()))
        assertTrue(json.contains("heartbeatPub"), json)
        assertTrue(json.contains("heartbeatSub"), json)
        // Features are written as names and periodLog is not part of the format.
        assertTrue(
            json.contains(
                """"heartbeatPub":{"address":"C000","period":64,"ttl":5,"index":1,"features":["relay"]}"""
            ),
            json
        )

        val imported = newManager().import(json.toByteArray())
        val pub = assertNotNull(imported.node(publisher.uuid)?.heartbeatPublication)
        assertEquals(GroupAddress(0xC000u), pub.address)
        assertEquals(64.toUShort(), pub.period)
        assertEquals(5.toUByte(), pub.ttl)
        assertEquals(1.toUShort(), pub.index)
        assertEquals(listOf<Feature>(Relay(FeatureState.Enabled)), pub.features)
        assertEquals(7.toUByte(), pub.periodLog)

        val sub = assertNotNull(imported.node(subscriber.uuid)?.heartbeatSubscription)
        assertEquals(publisher.primaryUnicastAddress, sub.source)
        assertEquals(UnicastAddress(0x0001u), sub.destination)
        // Nodes without heartbeat info stay without it.
        assertNull(imported.nodes.first {
            it.uuid != publisher.uuid && it.uuid != subscriber.uuid
        }.heartbeatPublication)
    }

    private fun importWithPublication(pubJson: String): HeartbeatPublication = runBlocking {
        val json = String(fixture).replaceFirst(
            Regex(""""nodes"\s*:\s*\[\s*\{"""),
            """"nodes":[{"heartbeatPub":$pubJson,"""
        )
        val network = newManager().import(json.toByteArray())
        assertNotNull(network.nodes[0].heartbeatPublication)
    }

    @Test
    fun `publication with features as names is imported`() {
        val pub = importWithPublication(
            """{"address":"0001","period":32,"ttl":5,"index":0,"features":["relay","lowPower"]}"""
        )
        assertEquals(
            listOf<Feature>(Relay(FeatureState.Enabled), LowPower(FeatureState.Enabled)),
            pub.features
        )
        assertEquals(6.toUByte(), pub.periodLog)
        assertTrue(pub.isFeatureTriggeredPublishingEnabled)
    }

    @Test
    fun `publication with empty features is imported`() {
        val pub = importWithPublication(
            """{"address":"0001","period":0,"ttl":5,"index":0,"features":[]}"""
        )
        assertTrue(pub.features.isEmpty())
        assertEquals(0.toUByte(), pub.periodLog)
        assertFalse(pub.isFeatureTriggeredPublishingEnabled)
    }

    @Test
    fun `publication in the legacy features format is imported`() {
        val unsupported = """{"type":"no.nordicsemi.kotlin.mesh.core.model.%s","state":""" +
                """{"type":"no.nordicsemi.kotlin.mesh.core.model.FeatureState.%s"}}"""
        val pub = importWithPublication(
            """{"address":"0001","period":32,"ttl":5,"index":0,"periodLog":6,"features":[""" +
                    unsupported.format("Relay", "Enabled") + "," +
                    unsupported.format("Proxy", "Unsupported") + "]}"
        )
        assertEquals(listOf<Feature>(Relay(FeatureState.Enabled)), pub.features)
        assertEquals(6.toUByte(), pub.periodLog)
    }

    @Test
    fun `heartbeat ios json resource is imported and exported unchanged`() = runBlocking {
        val bytes = javaClass.classLoader.getResourceAsStream("heartbeat_ios.json")!!.readAllBytes()
        val manager = newManager()
        val network = manager.import(bytes)

        val pub = assertNotNull(network.nodes.mapNotNull { it.heartbeatPublication }.singleOrNull())
        assertEquals(0x0017.toUShort(), pub.address.address)
        assertEquals(512.toUShort(), pub.period)
        assertEquals(10.toUByte(), pub.periodLog)
        assertEquals(5.toUByte(), pub.ttl)
        assertEquals(
            listOf<Feature>(
                Relay(FeatureState.Enabled),
                Proxy(FeatureState.Enabled),
                Friend(FeatureState.Enabled),
                LowPower(FeatureState.Enabled)
            ),
            pub.features
        )
        assertTrue(pub.isFeatureTriggeredPublishingEnabled)

        val sub = assertNotNull(network.nodes.mapNotNull { it.heartbeatSubscription }.singleOrNull())
        assertEquals(0x0001.toUShort(), sub.source.address)
        assertEquals(0xFFFE.toUShort(), sub.destination.address)

        val json = String(assertNotNull(manager.export()))
        assertTrue(
            json.contains(
                """"heartbeatPub":{"address":"0017","period":512,"ttl":5,"index":0,""" +
                        """"features":["relay","proxy","friend","lowPower"]}"""
            ),
            json
        )
        assertTrue(
            json.contains(""""heartbeatSub":{"source":"0001","destination":"FFFE"}"""), json
        )
    }
}
