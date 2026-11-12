package no.nordicsemi.kotlin.mesh.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FeaturesTest {

    @Test
    fun testFeatureRawValues() {
        assertEquals(0x01u.toUShort(), Relay(FeatureState.Enabled).rawValue)
        assertEquals(0x02u.toUShort(), Proxy(FeatureState.Enabled).rawValue)
        assertEquals(0x04u.toUShort(), Friend(FeatureState.Enabled).rawValue)
        assertEquals(0x08u.toUShort(), LowPower(FeatureState.Enabled).rawValue)
        assertEquals(0u.toUShort(), Proxy(FeatureState.Unsupported).rawValue)
    }

    @Test
    fun testRawValueOfFeatures() {
        val features = Features(
            _relay = Relay(FeatureState.Enabled),
            _proxy = Proxy(FeatureState.Unsupported),
            _friend = Friend(FeatureState.Enabled),
            _lowPower = LowPower(FeatureState.Unsupported)
        )
        assertEquals(0x05u.toUShort(), features.rawValue)
    }

    @Test
    fun testRawValueTreatsUnknownStateAsSupported() {
        assertEquals(0x0Fu.toUShort(), Features().rawValue)
    }

    @Test
    fun testFeaturesFromRawValue() {
        val features = Features(rawValue = 0x000Au.toUShort())
        assertEquals(FeatureState.Unsupported, features.relay?.state)
        assertEquals(FeatureState.Enabled, features.proxy?.state)
        assertEquals(FeatureState.Unsupported, features.friend?.state)
        assertEquals(FeatureState.Enabled, features.lowPower?.state)
    }

    @Test
    fun testRawValueRoundTrip() {
        for (raw in 0..0x0F) {
            assertEquals(raw.toUShort(), Features(rawValue = raw.toUShort()).rawValue)
        }
    }

    @Test
    fun testNoFeaturesFromZero() {
        assertEquals(0u.toUShort(), Features(rawValue = 0u).rawValue)
    }
}
