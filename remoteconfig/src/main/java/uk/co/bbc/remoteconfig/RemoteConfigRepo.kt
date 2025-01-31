package uk.co.bbc.remoteconfig

import kotlinx.coroutines.flow.Flow
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
    inline fun <reified AC, reified RC> remoteConfigFlow(): Flow<Result<Status<AC, RC>>> {
        val serializer: KSerializer<RemoteConfig<AC, RC>> = serializersModule.serializer()
        return flowA(serializer)
    }

    fun <AC, RC> flowA(
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

private fun <AC, RC> RemoteConfig<AC, RC>.toStatus(): Status<AC, RC> {
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

@Serializable
data class RemoteConfig<AC, RC>(
    @SerialName("killed") val killed: Boolean,
    @SerialName("retired") val retired: Boolean? = null,
    @SerialName("retired_config") val retiredConfig: RC? = null,
    @SerialName("active_config") val activeConfig: AC? = null,
)

sealed interface Status<out AC, out RC> {
    data object Killed : Status<Nothing, Nothing>
    data class Retired<RC>(val retiredConfig: RC) : Status<Nothing, RC>
    data class Active<AC>(val appConfig: AC) : Status<AC, Nothing>
}
