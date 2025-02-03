package uk.co.bbc.remoteconfig

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

interface RemoteConfigRepo<AC: Any, RC: Any> {
    val configFlow: Flow<Result<Status<AC, RC>>>
    fun retry()
}

inline fun <reified AC: Any, reified RC: Any> RemoteConfigRepo(
    noinline dataSource: () -> Result<ByteArray>,
    context: Context,
    configVersionName: String,
    cacheDir: String = "remote_config_cache",
    longTermCacheTimeout: Duration = 90.days,
): RemoteConfigRepo<AC, RC> {
    val cache = FallbackCacheDataSource(context, cacheDir, configVersionName, longTermCacheTimeout, dataSource)
    val pollingRepo = PollingRepository(cache)
    val repo = StatusRepo(
        srcDataFlow = pollingRepo.dataFlow,
    )
    val remoteConfigFlow = repo.remoteConfigFlow<AC, RC>().onEach {
        if(it.exceptionOrNull() is IllegalArgumentException) {
            cache.clearCache()
        }
    }
    return object : RemoteConfigRepo<AC, RC> {
        override val configFlow = remoteConfigFlow
        override fun retry() = pollingRepo.retry()
    }
}

