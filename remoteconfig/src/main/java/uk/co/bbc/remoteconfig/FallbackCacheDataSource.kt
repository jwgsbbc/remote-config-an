package uk.co.bbc.remoteconfig

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import kotlin.time.Duration

fun FallbackCacheDataSource(
    context: Context,
    directory: String,
    configVersion: String,
    ttl: Duration,
    dataSource: () -> Result<ByteArray>
): FallbackCacheDataSource {
    return FallbackCacheDataSource(
        directory = File(context.cacheDir, directory),
        name = configVersion,
        ttl = ttl,
        dataSource = dataSource
    )
}

class FallbackCacheDataSource(
    private val directory: File,
    private val name: String,
    private val ttl: Duration,
    // TODO: add timeout and suspend
    private val dataSource: () -> Result<ByteArray>,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
): () -> Result<ByteArray> {

    private val file = File(directory, name)

    init {
        if(directory.exists()) {
            directory.listFiles()?.forEach {
                if(it.canonicalPath!=file.canonicalPath) {
                    Log.d(this::class.simpleName, "Deleting ${it.name}")
                    it.delete()
                }
            }
        }
    }

    override fun invoke(): Result<ByteArray> {
        return Result.runCatching {
            // TODO: this currently is cache-first whereas we want just a fast fallback to this long term cache
            val expiry = timeProvider() - ttl.inWholeMilliseconds
            if(file.exists() && file.lastModified() > expiry) {
                readFromCache()
            } else {
                val sourceData = dataSource().getOrThrow()
                writeToCache(sourceData)
                sourceData
            }
        }
    }

    fun clearCache() {
        Log.d("CachingDataSource", "Clearing the cache!")
        directory.listFiles()?.forEach { it.delete() }
    }

    private fun writeToCache(sourceData: ByteArray) {
        try {
            directory.mkdirs()
            file.createNewFile()
            file.outputStream().use { it.write(sourceData) }
        } catch (e : IOException) {
            // potentially add some way of monitoring this case
            // should we fail the read if we can't write to cache?
            e.printStackTrace()
        }
    }

    private fun readFromCache(): ByteArray {
        return file.inputStream().use { it.readBytes() }
    }

}