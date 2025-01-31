package uk.co.bbc.remoteconfig.sampleapp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.map
import uk.co.bbc.remoteconfig.Status

class MainActivityViewModel(repo: AppRemoteConfigRepo): ViewModel() {
    val uiState = repo.configFlow.map { it.toUiState() }
}

private fun Result<AppStatus>.toUiState(): MainUiState {
    return fold(
        onSuccess = { it.toUiState() },
        onFailure = { MainUiState(message = "Error Loading $it") }
    )
}

private fun AppStatus.toUiState(): MainUiState {
    return when(this) {
        Status.Killed -> MainUiState("KILLED")
        is Status.Retired -> MainUiState("Retired: ${retiredConfig.message}")
        is Status.Active -> MainUiState("Active: ${appConfig.activeMessage}")
    }
}
