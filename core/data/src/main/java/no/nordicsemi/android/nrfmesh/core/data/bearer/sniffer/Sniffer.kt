package no.nordicsemi.android.nrfmesh.core.data.bearer.sniffer

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Process wide entry point of the feed to the nRF Mesh Sniffer app.
 *
 * Call [install] once, from the Application. Until then, PDUs are dropped.
 */
object Sniffer {
    @Volatile
    private var feed: SnifferFeed? = null

    /**
     * Starts the feed, if not started already.
     */
    @Synchronized
    fun install(context: Context) {
        if (feed != null) return
        feed = SnifferFeed(
            transport = SnifferProviderTransport(context.applicationContext),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        )
    }

    /**
     * Sink used by the bearers.
     */
    val sink = SnifferPduSink { pdu -> feed?.offer(pdu) }
}
