package uk.co.bbc.remoteconfig.sampleapp

import android.content.Context
import uk.co.bbc.remoteconfig.RemoteConfigRepo

typealias AppRemoteConfigRepo = RemoteConfigRepo<SampleAppConfig, SampleAppRetiredConfig>

fun AppRemoteConfigRepo(context: Context): AppRemoteConfigRepo {
    return RemoteConfigRepo<SampleAppConfig, SampleAppRetiredConfig>(
        dataSource = RemoteConfigDataSource(),
        context = context,
        configVersionName = "1.0.1",
    )
}
