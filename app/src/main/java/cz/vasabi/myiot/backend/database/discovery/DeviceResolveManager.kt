package cz.vasabi.myiot.backend.database.discovery

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import cz.vasabi.myiot.backend.logging.logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class DeviceResolveManager(private val nsdManager: NsdManager) {
    private val channel = Channel<Pair<CompletableDeferred<NsdServiceInfo>, NsdServiceInfo>>()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            for ((future, info) in channel) {
                val deviceResolver = DeviceResolver {
                    future.complete(it)
                }
                nsdManager.resolveService(info, deviceResolver)
                deviceResolver.isDone.join()
            }
        }
    }

    suspend fun resolveAsync(info: NsdServiceInfo): Deferred<NsdServiceInfo> {
        val future: CompletableDeferred<NsdServiceInfo> = CompletableDeferred()
        channel.send(Pair(future, info))
        return future
    }

    class DeviceResolver(private val onDeviceResolved: (NsdServiceInfo) -> Unit) :
        NsdManager.ResolveListener {
        var isDone = Job()

        override fun onResolveFailed(p0: NsdServiceInfo?, p1: Int) {
            logger.debug("failed to resolve service $p1 $p0")
            isDone.complete()
        }

        override fun onServiceResolved(p0: NsdServiceInfo?) {
            logger.debug("resolved service $p0")
            if (p0 == null) return
            onDeviceResolved(p0)
            isDone.complete()
        }
    }
}