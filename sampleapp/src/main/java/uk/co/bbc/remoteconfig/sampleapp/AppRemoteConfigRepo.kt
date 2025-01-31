package uk.co.bbc.remoteconfig.sampleapp

typealias AppStatus = Status<SampleAppConfig, SampleAppRetiredConfig>

class AppRemoteConfigRepo(remoteConfigRepo: RemoteConfigRepo) {
    val configFlow = remoteConfigRepo.remoteConfigFlow<SampleAppConfig, SampleAppRetiredConfig>()
}