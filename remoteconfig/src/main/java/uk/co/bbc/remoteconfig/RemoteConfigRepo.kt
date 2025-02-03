package uk.co.bbc.remoteconfig

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.Json.Default.serializersModule
import kotlinx.serialization.serializer

class RemoteConfigRepo(
    private val pollingRepo: PollingRepository
) {
    inline fun <reified AC : Any, reified RC : Any> remoteConfigFlow(): Flow<Result<Status<AC, RC>>> {
        val serializer: KSerializer<RemoteConfig<AC, RC>> = serializersModule.serializer()
        return remoteConfigFlow(serializer)
    }

    fun <AC : Any, RC : Any> remoteConfigFlow(
        serializer: KSerializer<RemoteConfig<AC, RC>>
    ): Flow<Result<Status<AC, RC>>> {
        return pollingRepo.dataFlow.map { result ->
            result.mapCatching { bytes ->
                Json.decodeFromString(serializer, bytes.decodeToString()).toStatus()
            }
        }
    }

    fun retry() {
        pollingRepo.retry()
    }
}

private fun <AC : Any, RC : Any> RemoteConfig<AC, RC>.toStatus(): Status<AC, RC> {
    return when {
        killed -> Status.Killed
        retired == null -> throw StatusParsingException("Status is not killed, but retired is not specified")
        retired -> retiredStats()
        else -> activeStats()
    }
}

private fun <AC, RC> RemoteConfig<AC, RC>.retiredStats(): Status.Retired<RC & Any> {
    return retiredConfig?.let {
        Status.Retired(retiredConfig)
    } ?: throw StatusParsingException("Status is retired, but retired config not specified")
}

private fun <AC, RC> RemoteConfig<AC, RC>.activeStats(): Status.Active<AC & Any> {
    return activeConfig?.let {
        Status.Active(activeConfig)
    } ?: throw StatusParsingException("Status is active, but active config not specified")
}

class StatusParsingException(message: String): Exception()
class AppKilledException(): Exception()
class AppRetiredException(retiredConfig: Any): Exception()

@Serializable
data class RemoteConfig<AC, RC>(
    @SerialName("killed") val killed: Boolean,
    @SerialName("retired") val retired: Boolean? = null,
    @SerialName("retired_config") val retiredConfig: RC? = null,
    @SerialName("active_config") val activeConfig: AC? = null,
)

sealed interface Status<out AC : Any, out RC : Any> {
    data object Killed : Status<Nothing, Nothing>
    data class Retired<RC : Any>(val retiredConfig: RC) : Status<Nothing, RC>
    data class Active<AC : Any>(val appConfig: AC) : Status<AC, Nothing>
}

fun <AC : Any, RC : Any> Flow<Result<Status<AC, RC>>>.filterActive(): Flow<Status.Active<AC>> {
    return map { it.getOrNull() as? Status.Active<AC> }.filterNotNull()
}

fun <AC : Any, RC : Any> Flow<Result<Status<AC, RC>>>.failureOnInactive(): Flow<Result<Status.Active<AC>>> {
    return map { result ->
        result.mapCatching {
            when(it) {
                is Status.Active -> it
                Status.Killed -> throw AppKilledException()
                is Status.Retired -> throw AppRetiredException(it.retiredConfig)
            }
        }
    }
}
