package ru.kode.android.build.publish.plugin.play.service

import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.play.service.network.PlayNetworkService

abstract class PlayServiceExtension(
    val networkServices: Provider<Map<String, Provider<PlayNetworkService>>>,
)
