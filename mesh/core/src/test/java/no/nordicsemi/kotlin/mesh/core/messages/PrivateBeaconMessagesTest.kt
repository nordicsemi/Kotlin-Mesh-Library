package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.PrivateBeaconGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.PrivateBeaconSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.PrivateBeaconStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.PrivateGattProxyGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.PrivateGattProxySet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.PrivateGattProxyStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.PrivateNodeIdentityGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.PrivateNodeIdentitySet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.PrivateNodeIdentityStatus
import no.nordicsemi.kotlin.mesh.core.model.FeatureState
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests the Private Beacon, Private GATT Proxy and Private Node Identity config messages. */
class PrivateBeaconMessagesTest {

    @Test
    fun testOpCodes() {
        assertEquals(0x8060u, PrivateBeaconGet.opCode)
        assertEquals(0x8061u, PrivateBeaconSet.opCode)
        assertEquals(0x8062u, PrivateBeaconStatus.opCode)
        assertEquals(0x8063u, PrivateGattProxyGet.opCode)
        assertEquals(0x8064u, PrivateGattProxySet.opCode)
        assertEquals(0x8065u, PrivateGattProxyStatus.opCode)
        assertEquals(0x8066u, PrivateNodeIdentityGet.opCode)
        assertEquals(0x8067u, PrivateNodeIdentitySet.opCode)
        assertEquals(0x8068u, PrivateNodeIdentityStatus.opCode)
    }

    @Test
    fun testResponseOpCodes() {
        assertEquals(PrivateBeaconStatus.opCode, PrivateBeaconGet().responseOpCode)
        assertEquals(PrivateBeaconStatus.opCode, PrivateBeaconSet(true).responseOpCode)
        assertEquals(PrivateGattProxyStatus.opCode, PrivateGattProxyGet().responseOpCode)
        assertEquals(PrivateGattProxyStatus.opCode, PrivateGattProxySet(true).responseOpCode)
        assertEquals(PrivateNodeIdentityStatus.opCode, PrivateNodeIdentityGet(0u).responseOpCode)
        assertEquals(
            PrivateNodeIdentityStatus.opCode,
            PrivateNodeIdentitySet(0u, true).responseOpCode
        )
    }

    // Private Beacon

    @Test
    fun testPrivateBeaconGet() {
        assertNull(PrivateBeaconGet().parameters)
        assertNotNull(PrivateBeaconGet.init(byteArrayOf()))
        assertNull(PrivateBeaconGet.init(byteArrayOf(0x00)))
        assertNull(PrivateBeaconGet.init(null))
    }

    @Test
    fun testPrivateBeaconSetEncoding() {
        assertContentEquals(byteArrayOf(0x01), PrivateBeaconSet(true).parameters)
        assertContentEquals(byteArrayOf(0x00), PrivateBeaconSet(false).parameters)
        assertContentEquals(
            byteArrayOf(0x01, 0x3C),
            PrivateBeaconSet.enabledWithSteps(0x3Cu).parameters
        )
        // 600 s -> 60 steps
        assertContentEquals(
            byteArrayOf(0x01, 60),
            PrivateBeaconSet.enabledWithInterval(600).parameters
        )
        // Clamped to 10..2550 s
        assertEquals(1u.toUByte(), PrivateBeaconSet.enabledWithInterval(1).steps)
        assertEquals(255u.toUByte(), PrivateBeaconSet.enabledWithInterval(100000).steps)
    }

    @Test
    fun testPrivateBeaconSetDecoding() {
        val withoutInterval = PrivateBeaconSet.init(byteArrayOf(0x01))!!
        assertTrue(withoutInterval.enabled)
        assertNull(withoutInterval.steps)
        assertNull(withoutInterval.interval)

        val withInterval = PrivateBeaconSet.init(byteArrayOf(0x00, 0x05))!!
        assertFalse(withInterval.enabled)
        assertEquals(5u.toUByte(), withInterval.steps)
        assertEquals(RandomUpdateIntervalSteps.Interval(5u), withInterval.interval)

        assertEquals(
            RandomUpdateIntervalSteps.EveryTime,
            PrivateBeaconSet.init(byteArrayOf(0x01, 0x00))!!.interval
        )
    }

    @Test
    fun testPrivateBeaconSetInvalid() {
        assertNull(PrivateBeaconSet.init(null))
        assertNull(PrivateBeaconSet.init(byteArrayOf()))
        assertNull(PrivateBeaconSet.init(byteArrayOf(0x02)))
        assertNull(PrivateBeaconSet.init(byteArrayOf(0x01, 0x01, 0x01)))
    }

    @Test
    fun testPrivateBeaconStatus() {
        assertContentEquals(byteArrayOf(0x01, 0x00), PrivateBeaconStatus(true).parameters)
        assertContentEquals(byteArrayOf(0x00, 0x0A), PrivateBeaconStatus(false, 10u).parameters)
        assertContentEquals(
            byteArrayOf(0x01, 0x03),
            PrivateBeaconStatus.withInterval(enabled = true, seconds = 30).parameters
        )

        val status = PrivateBeaconStatus.init(byteArrayOf(0x01, 0x3C))!!
        assertTrue(status.enabled)
        assertEquals(60u.toUByte(), status.steps)
        assertEquals(RandomUpdateIntervalSteps.Interval(60u), status.interval)
        assertEquals(
            RandomUpdateIntervalSteps.EveryTime,
            PrivateBeaconStatus.init(byteArrayOf(0x00, 0x00))!!.interval
        )

        assertNull(PrivateBeaconStatus.init(null))
        assertNull(PrivateBeaconStatus.init(byteArrayOf(0x01)))
        assertNull(PrivateBeaconStatus.init(byteArrayOf(0x01, 0x01, 0x01)))
        assertNull(PrivateBeaconStatus.init(byteArrayOf(0x02, 0x01)))
    }

    // Private GATT Proxy

    @Test
    fun testPrivateGattProxyGet() {
        assertNull(PrivateGattProxyGet().parameters)
        assertNotNull(PrivateGattProxyGet.init(byteArrayOf()))
        assertNull(PrivateGattProxyGet.init(byteArrayOf(0x00)))
        assertNull(PrivateGattProxyGet.init(null))
    }

    @Test
    fun testPrivateGattProxySet() {
        assertContentEquals(byteArrayOf(0x01), PrivateGattProxySet(true).parameters)
        assertContentEquals(byteArrayOf(0x00), PrivateGattProxySet(false).parameters)
        assertTrue(PrivateGattProxySet.init(byteArrayOf(0x01))!!.enabled)
        assertFalse(PrivateGattProxySet.init(byteArrayOf(0x00))!!.enabled)
        assertNull(PrivateGattProxySet.init(byteArrayOf(0x02)))
        assertNull(PrivateGattProxySet.init(byteArrayOf()))
        assertNull(PrivateGattProxySet.init(byteArrayOf(0x00, 0x00)))
        assertNull(PrivateGattProxySet.init(null))
    }

    @Test
    fun testPrivateGattProxyStatus() {
        assertContentEquals(
            byteArrayOf(0x02),
            PrivateGattProxyStatus(FeatureState.Unsupported).parameters
        )
        assertEquals(FeatureState.Disabled, PrivateGattProxyStatus.init(byteArrayOf(0x00))!!.state)
        assertEquals(FeatureState.Enabled, PrivateGattProxyStatus.init(byteArrayOf(0x01))!!.state)
        assertEquals(
            FeatureState.Unsupported,
            PrivateGattProxyStatus.init(byteArrayOf(0x02))!!.state
        )
        assertEquals(FeatureState.Unsupported, PrivateGattProxyStatus.unsupported.state)
        assertNull(PrivateGattProxyStatus.init(byteArrayOf(0x03)))
        assertNull(PrivateGattProxyStatus.init(byteArrayOf()))
        assertNull(PrivateGattProxyStatus.init(byteArrayOf(0x00, 0x00)))
        assertNull(PrivateGattProxyStatus.init(null))
    }

    // Private Node Identity

    @Test
    fun testPrivateNodeIdentityGet() {
        // Net key index 0x123 is encoded as 2 octets, little endian.
        assertContentEquals(byteArrayOf(0x23, 0x01), PrivateNodeIdentityGet(0x123u).parameters)
        assertEquals(
            0x123u.toUShort(),
            PrivateNodeIdentityGet.init(byteArrayOf(0x23, 0x01))!!.networkKeyIndex
        )
        assertNull(PrivateNodeIdentityGet.init(byteArrayOf(0x23)))
        assertNull(PrivateNodeIdentityGet.init(byteArrayOf(0x23, 0x01, 0x00)))
        assertNull(PrivateNodeIdentityGet.init(null))
    }

    @Test
    fun testPrivateNodeIdentitySet() {
        assertContentEquals(
            byteArrayOf(0x23, 0x01, 0x01),
            PrivateNodeIdentitySet(0x123u, true).parameters
        )
        assertContentEquals(
            byteArrayOf(0x01, 0x00, 0x00),
            PrivateNodeIdentitySet(1u, false).parameters
        )
        val decoded = PrivateNodeIdentitySet.init(byteArrayOf(0x23, 0x01, 0x01))!!
        assertEquals(0x123u.toUShort(), decoded.networkKeyIndex)
        assertTrue(decoded.enabled)
        assertFalse(PrivateNodeIdentitySet.init(byteArrayOf(0x00, 0x00, 0x00))!!.enabled)
        assertNull(PrivateNodeIdentitySet.init(byteArrayOf(0x00, 0x00, 0x02)))
        assertNull(PrivateNodeIdentitySet.init(byteArrayOf(0x00, 0x00)))
        assertNull(PrivateNodeIdentitySet.init(byteArrayOf(0x00, 0x00, 0x01, 0x00)))
        assertNull(PrivateNodeIdentitySet.init(null))
    }

    @Test
    fun testPrivateNodeIdentityStatus() {
        val status = PrivateNodeIdentityStatus(
            status = ConfigMessageStatus.SUCCESS,
            networkKeyIndex = 0x123u,
            state = FeatureState.Enabled
        )
        assertContentEquals(byteArrayOf(0x00, 0x23, 0x01, 0x01), status.parameters)

        val decoded = PrivateNodeIdentityStatus.init(byteArrayOf(0x00, 0x23, 0x01, 0x01))!!
        assertEquals(ConfigMessageStatus.SUCCESS, decoded.status)
        assertEquals(0x123u.toUShort(), decoded.networkKeyIndex)
        assertEquals(FeatureState.Enabled, decoded.state)

        val failed = PrivateNodeIdentityStatus.init(byteArrayOf(0x04, 0x00, 0x00, 0x02))!!
        assertEquals(ConfigMessageStatus.INVALID_NET_KEY_INDEX, failed.status)
        assertEquals(FeatureState.Unsupported, failed.state)

        val rejected = PrivateNodeIdentityStatus(request = PrivateNodeIdentityGet(5u))
        assertEquals(FeatureState.Unsupported, rejected.state)
        assertEquals(5u.toUShort(), rejected.networkKeyIndex)
        assertTrue(rejected.isSuccess)
    }

    @Test
    fun testPrivateNodeIdentityStatusInvalid() {
        assertNull(PrivateNodeIdentityStatus.init(null))
        assertNull(PrivateNodeIdentityStatus.init(byteArrayOf(0x00, 0x00, 0x00)))
        assertNull(PrivateNodeIdentityStatus.init(byteArrayOf(0x00, 0x00, 0x00, 0x01, 0x00)))
        // Invalid state
        assertNull(PrivateNodeIdentityStatus.init(byteArrayOf(0x00, 0x00, 0x00, 0x03)))
        // Unknown status code
        assertNull(PrivateNodeIdentityStatus.init(byteArrayOf(0x7F, 0x00, 0x00, 0x01)))
    }
}
