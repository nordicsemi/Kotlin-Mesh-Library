package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.model.AllNodes
import no.nordicsemi.kotlin.mesh.core.model.FeatureState
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatPublication
import no.nordicsemi.kotlin.mesh.core.model.Proxy
import no.nordicsemi.kotlin.mesh.core.model.Relay
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigHeartbeatPublicationFeaturesTest {

    private fun set(vararg features: no.nordicsemi.kotlin.mesh.core.model.Feature) =
        ConfigHeartbeatPublicationSet(
            networkKeyIndex = 0x0001u,
            destination = AllNodes,
            countLog = 0x05u,
            periodLog = 0x03u,
            ttl = 0x07u,
            features = features.toList()
        )

    @Test
    fun `set encodes selected features in the features field`() {
        val params = set(Relay(FeatureState.Enabled), Proxy(FeatureState.Disabled)).parameters
        assertEquals(9, params.size)
        // destination (0xFFFF), countLog, periodLog, ttl, features (relay|proxy = 0x0003), key 1
        assertContentEquals(
            byteArrayOf(
                0xFF.toByte(), 0xFF.toByte(), 0x05, 0x03, 0x07, 0x03, 0x00, 0x01, 0x00
            ),
            params
        )
    }

    @Test
    fun `set with no features encodes zero features`() {
        val params = set().parameters
        assertEquals(0x00, params[5].toInt())
        assertEquals(0x00, params[6].toInt())
    }

    @Test
    fun `set ignores unsupported features`() {
        val params = set(Relay(FeatureState.Unsupported), Proxy(FeatureState.Enabled)).parameters
        assertEquals(0x02, params[5].toInt())
    }

    @Test
    fun `enablesFeatureTriggeredPublication does not throw for partial feature lists`() {
        assertTrue(set(Proxy(FeatureState.Enabled)).enablesFeatureTriggeredPublication)
        assertFalse(set().enablesFeatureTriggeredPublication)
    }

    @Test
    fun `status with all features unsupported does not enable feature triggered publishing`() {
        val status = ConfigHeartbeatPublicationStatus.init(
            byteArrayOf(0x00, 0xFF.toByte(), 0xFF.toByte(), 0x05, 0x03, 0x07, 0x00, 0x00, 0x00, 0x00)
        )!!
        val publication = HeartbeatPublication(status)
        assertTrue(publication.isEnabled)
        assertFalse(publication.isFeatureTriggeredPublishingEnabled)
    }

    @Test
    fun `status with a feature enabled enables feature triggered publishing`() {
        val status = ConfigHeartbeatPublicationStatus.init(
            byteArrayOf(0x00, 0xFF.toByte(), 0xFF.toByte(), 0x05, 0x03, 0x07, 0x02, 0x00, 0x00, 0x00)
        )!!
        assertTrue(HeartbeatPublication(status).isFeatureTriggeredPublishingEnabled)
    }

    @Test
    fun `disabled publication never reports feature triggered publishing`() {
        val status = ConfigHeartbeatPublicationStatus(
            destination = UnassignedAddress,
            features = listOf(Relay(FeatureState.Enabled))
        )
        assertFalse(HeartbeatPublication(status).isFeatureTriggeredPublishingEnabled)
    }

    @Test
    fun `status only holds the features that trigger heartbeats`() {
        // relay (bit 0) and low power (bit 3)
        val status = ConfigHeartbeatPublicationStatus.init(
            byteArrayOf(0x00, 0xFF.toByte(), 0xFF.toByte(), 0x05, 0x03, 0x07, 0x09, 0x00, 0x00, 0x00)
        )!!
        assertEquals(
            listOf<no.nordicsemi.kotlin.mesh.core.model.Feature>(
                Relay(FeatureState.Enabled),
                no.nordicsemi.kotlin.mesh.core.model.LowPower(FeatureState.Enabled)
            ),
            status.features
        )
    }

    @Test
    fun `status without features has an empty list`() {
        val status = ConfigHeartbeatPublicationStatus.init(
            byteArrayOf(0x00, 0xFF.toByte(), 0xFF.toByte(), 0x05, 0x03, 0x07, 0x00, 0x00, 0x00, 0x00)
        )!!
        assertTrue(status.features.isEmpty())
    }

    @Test
    fun `set parsed from parameters only holds the features that trigger heartbeats`() {
        val parsed = ConfigHeartbeatPublicationSet.init(set(Proxy(FeatureState.Enabled)).parameters)!!
        assertEquals(listOf<no.nordicsemi.kotlin.mesh.core.model.Feature>(Proxy(FeatureState.Enabled)), parsed.features)
        assertTrue(parsed.enablesFeatureTriggeredPublication)
    }
}
