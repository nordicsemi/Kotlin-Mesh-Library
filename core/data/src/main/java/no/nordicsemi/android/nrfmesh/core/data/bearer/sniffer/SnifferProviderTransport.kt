package no.nordicsemi.android.nrfmesh.core.data.bearer.sniffer

import android.content.ContentProviderClient
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log

/**
 * Delivers PDUs to the content provider of the nRF Mesh Sniffer app, when it is installed.
 *
 * The provider is acquired lazily, as an unstable client, so that the death of the Sniffer does not
 * affect nRF Mesh. When it is not available, or a call fails, the client is dropped and acquiring
 * it is retried no sooner than [retryIntervalMs] later, which makes this cheap when the Sniffer
 * is not installed and handles it being installed or removed at runtime.
 *
 * The Sniffer stores a PDU only while it is scanning, otherwise insert returns null. The provider
 * is guarded by the signature of the apps, so both have to be signed with the same certificate.
 * The PDU contents are never logged above debug level.
 *
 * @param context          Application context.
 * @param retryIntervalMs  Minimum time between attempts to acquire the provider.
 * @param elapsedRealtime  Monotonic clock in milliseconds.
 */
class SnifferProviderTransport(
    context: Context,
    private val retryIntervalMs: Long = RETRY_INTERVAL_MS,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
) : SnifferTransport {
    private val resolver = context.applicationContext.contentResolver
    private var client: ContentProviderClient? = null
    private var lastAttempt: Long? = null

    override fun deliver(pdu: SnifferPdu) {
        try {
            val client = client ?: acquire() ?: return
            val values = ContentValues().apply {
                pdu.toValues().forEach { (key, value) ->
                    when (value) {
                        is Long -> put(key, value)
                        is Int -> put(key, value)
                        is String -> put(key, value)
                        is ByteArray -> put(key, value)
                    }
                }
            }
            client.insert(URI, values)
        } catch (e: Throwable) {
            Log.d(TAG, "Delivering PDU to the Sniffer failed: $e")
            release()
        }
    }

    private fun acquire(): ContentProviderClient? {
        val now = elapsedRealtime()
        val last = lastAttempt
        if (last != null && now - last < retryIntervalMs) return null
        lastAttempt = now
        return resolver.acquireUnstableContentProviderClient(URI)?.also { client = it }
    }

    private fun release() {
        runCatching { client?.close() }
        client = null
        lastAttempt = elapsedRealtime()
    }

    companion object {
        private const val TAG = "SnifferFeed"
        const val AUTHORITY = "no.nordicsemi.android.mesh.sniffer.provider"
        val URI: Uri = Uri.parse("content://$AUTHORITY/pdus")
        const val RETRY_INTERVAL_MS = 5_000L
    }
}
