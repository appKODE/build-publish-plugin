package ru.kode.android.build.publish.plugin.telegram.service

import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.telegram.service.network.TelegramNetworkService

abstract class TelegramServiceExtension(
    val networkServices: Provider<Map<String, Provider<TelegramNetworkService>>>,
)
