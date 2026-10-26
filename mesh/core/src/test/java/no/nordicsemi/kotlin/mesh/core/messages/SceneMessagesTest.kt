package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneDelete
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneDeleteUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRecall
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRecallUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRegisterGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRegisterStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStatusCode
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStore
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStoreUnacknowledged
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SceneMessagesTest {

    private val transition = TransitionParameters(
        transitionTime = TransitionTime(rawValue = 0x45u),
        delay = 0x0Au
    )

    @Test
    fun testOpCodes() {
        assertEquals(0x8241u, SceneGet.opCode)
        assertEquals(0x8242u, SceneRecall.opCode)
        assertEquals(0x8243u, SceneRecallUnacknowledged.opCode)
        assertEquals(0x8244u, SceneRegisterGet.opCode)
        assertEquals(0x8245u, SceneRegisterStatus.opCode)
        assertEquals(0x8246u, SceneStore.opCode)
        assertEquals(0x8247u, SceneStoreUnacknowledged.opCode)
        assertEquals(0x5Eu, SceneStatus.opCode)
        assertEquals(0x829Eu, SceneDelete.opCode)
        assertEquals(0x829Fu, SceneDeleteUnacknowledged.opCode)
    }

    @Test
    fun testGetMessagesHaveNoParameters() {
        assertNull(SceneGet().parameters)
        assertNull(SceneRegisterGet().parameters)
        assertNotNull(SceneGet.init(null))
        assertNotNull(SceneRegisterGet.init(null))
        assertNull(SceneGet.init(byteArrayOf(0x00)))
        assertNull(SceneRegisterGet.init(byteArrayOf(0x00)))
    }

    @Test
    fun testGetResponseOpCodes() {
        assertEquals(SceneStatus.opCode, SceneGet().responseOpCode)
        assertEquals(SceneRegisterStatus.opCode, SceneRegisterGet().responseOpCode)
        assertEquals(SceneStatus.opCode, SceneRecall(sceneNumber = 1u).responseOpCode)
        assertEquals(SceneRegisterStatus.opCode, SceneStore(sceneNumber = 1u).responseOpCode)
        assertEquals(SceneRegisterStatus.opCode, SceneDelete(sceneNumber = 1u).responseOpCode)
    }

    @Test
    fun testSceneStoreEncodeDecode() {
        val message = SceneStore(sceneNumber = 0x1234u)
        // Scene numbers are little endian.
        assertContentEquals(byteArrayOf(0x34, 0x12), message.parameters)
        assertEquals(0x1234u.toUShort(), SceneStore.init(message.parameters)?.sceneNumber)
        assertEquals(
            0x1234u.toUShort(),
            SceneStoreUnacknowledged.init(message.parameters)?.sceneNumber
        )
    }

    @Test
    fun testSceneDeleteEncodeDecode() {
        val message = SceneDelete(sceneNumber = 0x00FFu)
        assertContentEquals(byteArrayOf(0xFF.toByte(), 0x00), message.parameters)
        assertEquals(0x00FFu.toUShort(), SceneDelete.init(message.parameters)?.sceneNumber)
        assertEquals(
            0x00FFu.toUShort(),
            SceneDeleteUnacknowledged.init(message.parameters)?.sceneNumber
        )
    }

    @Test
    fun testZeroSceneNumberIsRejected() {
        assertFailsWith<IllegalArgumentException> { SceneStore(sceneNumber = 0u) }
        assertFailsWith<IllegalArgumentException> { SceneStoreUnacknowledged(sceneNumber = 0u) }
        assertFailsWith<IllegalArgumentException> { SceneDelete(sceneNumber = 0u) }
        assertFailsWith<IllegalArgumentException> { SceneDeleteUnacknowledged(sceneNumber = 0u) }
        assertFailsWith<IllegalArgumentException> { SceneRecall(sceneNumber = 0u) }
        assertFailsWith<IllegalArgumentException> { SceneRecallUnacknowledged(sceneNumber = 0u) }

        val zero = byteArrayOf(0x00, 0x00)
        assertNull(SceneStore.init(zero))
        assertNull(SceneDelete.init(zero))
        assertNull(SceneRecall.init(byteArrayOf(0x00, 0x00, 0x01)))
    }

    @Test
    fun testInvalidLengthsAreRejected() {
        assertNull(SceneStore.init(null))
        assertNull(SceneStore.init(byteArrayOf(0x01)))
        assertNull(SceneDelete.init(byteArrayOf(0x01, 0x00, 0x00)))
        assertNull(SceneRecall.init(byteArrayOf(0x01, 0x00)))
        assertNull(SceneRecall.init(byteArrayOf(0x01, 0x00, 0x01, 0x45)))
        assertNull(SceneStatus.init(byteArrayOf(0x00, 0x01)))
        assertNull(SceneStatus.init(byteArrayOf(0x00, 0x01, 0x00, 0x02, 0x00)))
        assertNull(SceneRegisterStatus.init(byteArrayOf(0x00, 0x01)))
        assertNull(SceneRegisterStatus.init(byteArrayOf(0x00, 0x01, 0x00, 0x02)))
    }

    @Test
    fun testSceneRecallWithoutTransition() {
        val message = SceneRecall(tid = 0x07u, sceneNumber = 0x0102u)
        assertContentEquals(byteArrayOf(0x02, 0x01, 0x07), message.parameters)

        val decoded = SceneRecall.init(message.parameters)
        assertNotNull(decoded)
        assertEquals(0x0102u.toUShort(), decoded.sceneNumber)
        assertEquals(0x07u.toUByte(), decoded.tid)
        assertNull(decoded.transitionTime)
        assertNull(decoded.delay)
    }

    @Test
    fun testSceneRecallWithTransition() {
        val message = SceneRecall(tid = 0x07u, sceneNumber = 0x0102u, transitionParams = transition)
        assertContentEquals(byteArrayOf(0x02, 0x01, 0x07, 0x45, 0x0A), message.parameters)

        val decoded = SceneRecall.init(message.parameters)
        assertNotNull(decoded)
        assertEquals(0x0102u.toUShort(), decoded.sceneNumber)
        assertEquals(0x07u.toUByte(), decoded.tid)
        assertEquals(transition.transitionTime, decoded.transitionTime)
        assertEquals(transition.delay, decoded.delay)
    }

    @Test
    fun testSceneRecallUnacknowledgedEncodeDecode() {
        val message = SceneRecallUnacknowledged(
            tid = 0x09u,
            sceneNumber = 0x0003u,
            transitionParams = transition
        )
        assertContentEquals(byteArrayOf(0x03, 0x00, 0x09, 0x45, 0x0A), message.parameters)

        val decoded = SceneRecallUnacknowledged.init(message.parameters)
        assertNotNull(decoded)
        assertEquals(0x0003u.toUShort(), decoded.sceneNumber)
        assertEquals(0x09u.toUByte(), decoded.tid)
        assertEquals(transition.transitionTime, decoded.transitionTime)
        assertEquals(transition.delay, decoded.delay)

        val short = SceneRecallUnacknowledged.init(byteArrayOf(0x03, 0x00, 0x09))
        assertNotNull(short)
        assertNull(short.transitionTime)
    }

    @Test
    fun testSceneStatusWithoutTarget() {
        val message = SceneStatus(status = SceneStatusCode.SUCCESS, currentScene = 0x0201u)
        assertContentEquals(byteArrayOf(0x00, 0x01, 0x02), message.parameters)
        assertTrue(message.isSuccess)

        val decoded = SceneStatus.init(message.parameters)
        assertNotNull(decoded)
        assertEquals(SceneStatusCode.SUCCESS, decoded.status)
        assertEquals(0x0201u.toUShort(), decoded.currentScene)
        assertNull(decoded.targetScene)
        assertNull(decoded.remainingTime)
    }

    @Test
    fun testSceneStatusWithTarget() {
        val message = SceneStatus(
            status = SceneStatusCode.SUCCESS,
            currentScene = 0x0001u,
            targetScene = 0x0302u,
            remainingTime = TransitionTime(rawValue = 0x45u)
        )
        assertContentEquals(
            byteArrayOf(0x00, 0x01, 0x00, 0x02, 0x03, 0x45),
            message.parameters
        )

        val decoded = SceneStatus.init(message.parameters)
        assertNotNull(decoded)
        assertEquals(0x0001u.toUShort(), decoded.currentScene)
        assertEquals(0x0302u.toUShort(), decoded.targetScene)
        assertEquals(TransitionTime(rawValue = 0x45u), decoded.remainingTime)
    }

    @Test
    fun testSceneStatusErrorCodes() {
        val notFound = SceneStatus.init(byteArrayOf(0x02, 0x00, 0x00))
        assertNotNull(notFound)
        assertEquals(SceneStatusCode.SCENE_NOT_FOUND, notFound.status)
        assertFalse(notFound.isSuccess)
        assertEquals("Scene Not Found", notFound.message)

        // Reserved status codes are not decoded.
        assertNull(SceneStatus.init(byteArrayOf(0x03, 0x00, 0x00)))
    }

    @Test
    fun testSceneRegisterStatusEncodeDecode() {
        val message = SceneRegisterStatus(
            status = SceneStatusCode.SUCCESS,
            currentScene = 0x0001u,
            scenes = listOf(0x0001u, 0x0102u, 0xFFFFu)
        )
        assertContentEquals(
            byteArrayOf(0x00, 0x01, 0x00, 0x01, 0x00, 0x02, 0x01, 0xFF.toByte(), 0xFF.toByte()),
            message.parameters
        )

        val decoded = SceneRegisterStatus.init(message.parameters)
        assertNotNull(decoded)
        assertEquals(SceneStatusCode.SUCCESS, decoded.status)
        assertEquals(0x0001u.toUShort(), decoded.currentScene)
        assertEquals(listOf<UShort>(0x0001u, 0x0102u, 0xFFFFu), decoded.scenes)
    }

    @Test
    fun testSceneRegisterStatusWithEmptyRegister() {
        val message = SceneRegisterStatus(
            status = SceneStatusCode.SCENE_REGISTER_FULL,
            currentScene = 0x0000u,
            scenes = emptyList()
        )
        assertContentEquals(byteArrayOf(0x01, 0x00, 0x00), message.parameters)

        val decoded = SceneRegisterStatus.init(message.parameters)
        assertNotNull(decoded)
        assertEquals(SceneStatusCode.SCENE_REGISTER_FULL, decoded.status)
        assertTrue(decoded.scenes.isEmpty())
        assertFalse(decoded.isSuccess)
    }
}
