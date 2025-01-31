package uk.co.bbc.remoteconfig.sampleapp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class PollingRepository(
    dataSource: () -> Result<ByteArray>,
    pollingConfigFlow: Flow<PollingConfig> = flowOf(defaultPollingConfig)
) {

    private val retryFlow = MutableStateFlow(Unit)

    val dataFlow: Flow<Result<ByteArray>> = pollingConfigFlow.distinctUntilChanged()
        .combine(retryFlow) { pollingConfig, _ -> pollingConfig }
        .flatMapLatest { pollingConfig ->
            flow {
                while(true) {
                    emit(dataSource())
                    delay(pollingConfig.pollingRate)
                }
            }
        }

    fun retry() {
        retryFlow.tryEmit(Unit)
    }
}

private val defaultPollingConfig = PollingConfig(
    pollingRate = 1.seconds
)

data class PollingConfig(
    val pollingRate: Duration
)
