package ru.kode.android.build.publish.plugin.play.service

import org.gradle.api.provider.Provider

abstract class PlayNetworkServiceExtension(
    val services: Provider<Map<String, Provider<PlayNetworkService>>>
)
