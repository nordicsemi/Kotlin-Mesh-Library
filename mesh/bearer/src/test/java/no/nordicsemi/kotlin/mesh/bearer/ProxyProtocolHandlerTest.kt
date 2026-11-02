package no.nordicsemi.kotlin.mesh.bearer

import org.junit.Assert
import org.junit.Test

class ProxyProtocolHandlerTest {

    @Test
    fun segment() {
        val expectedMessage =
            "430339C31CD3EFB53FD664443882AA4E4E4DDED7BE6063D16E5EA84CBF09" +
                    "8337205C8D0854AE88FC98873FE58B699FD4638924C3D21824C0CCD64722" +
                    "C38F1535C73D6FC8"
        val message =
            ("0339c31cd3efb53fd664443882aa4e4e4dded7be6063d16e5ea84cbf0937205c8d0854ae88fc9887" +
                    "3fe58b699fd4638924c3d21824c0ccd647228f1535c73d6fc8").uppercase()
        val handler = ProxyProtocolHandler()
        val actualMessage =
            handler.segment(data = message.toByteArray(), type = PduType.PROVISIONING_PDU, mtu = 30)
                .joinToString("") { it.toHexString() }
        Assert.assertEquals(expectedMessage, actualMessage)
    }

    @Test
    fun reassemble() {
        val expectedMessage =
            ("0339c31cd3efb53fd664443882aa4e4e4dded7be6063d16e5ea84cbf0937205c8d0854ae88fc9887" +
                    "3fe58b699fd4638924c3d21824c0ccd647228f1535c73d6fc8").uppercase()
        val messages = listOf(
            "430339C31CD3EFB53FD664443882AA4E4E4DDED7BE6063D16E5EA84CBF09".toByteArray(),
            "8337205C8D0854AE88FC98873FE58B699FD4638924C3D21824C0CCD64722".toByteArray(),
            "C38F1535C73D6FC8".toByteArray()
        )
        val handler = ProxyProtocolHandler()
        var actualMessage = ""
        messages.forEach { message ->
            handler.reassemble(message)?.let {
                actualMessage += it.data.toHexString()
            }
        }
        Assert.assertEquals(expectedMessage, actualMessage)
    }
}