package ru.kode.android.build.publish.plugin.telegram.service

import org.gradle.api.provider.Provider

abstract class TelegramNetworkServiceExtension(
    val services: Provider<Map<String, Provider<TelegramNetworkService>>>,
)
