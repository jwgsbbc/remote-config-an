package uk.co.bbc.remoteconfig.sampleapp

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun MainActivityViewModel(creationExtras: CreationExtras): MainActivityViewModel {
    val application = creationExtras[APPLICATION_KEY]!!
    val dataSource = RemoteConfigDataSource()
    val pollingRepo = PollingRepository(dataSource)
    val genericRemoteConfigRepo = RemoteConfigRepo(pollingRepo)
    val remoteConfigRepo = AppRemoteConfigRepo(genericRemoteConfigRepo)
    return MainActivityViewModel(remoteConfigRepo)
}

@Serializable
data class SampleAppRetiredConfig(
    @SerialName("retired_message") val message: String
)

@Serializable
data class SampleAppConfig(
    @SerialName("active_message") val activeMessage: String
)

class RemoteConfigDataSource: () -> Result<ByteArray> {

    private var count = 0

    override fun invoke(): Result<ByteArray> {
        val rem = (count / 10).rem(3)
        val json = when(rem) {
            0 -> {
                """
                {
                    "killed": false,
                    "retired": false,
                    "active_config": {
                        "active_message": "Hello, app is ALIVE! $count"
                    }
                }
                """
            }
            1 -> {
                """
                {
                    "killed": false,
                    "retired": true,
                    "retired_config": {
                        "retired_message": "Goodbye, app is RETIRED! $count"
                    }
                }
                """
            }
            else -> {
                """
                {
                    "killed": true
                }
                """
            }
        }

        count += 1

        return Result.success(json.encodeToByteArray())
    }
}

