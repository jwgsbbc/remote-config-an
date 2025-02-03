package uk.co.bbc.remoteconfig.sampleapp

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun MainActivityViewModel(creationExtras: CreationExtras): MainActivityViewModel {
    val application = creationExtras[APPLICATION_KEY]!!
    val remoteConfigRepo = AppRemoteConfigRepo(application)
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

