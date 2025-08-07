package ru.kode.android.build.publish.plugin.appcenter.service

import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.appcenter.service.network.AppCenterNetworkService

abstract class AppCenterServiceExtension(
    val networkServices: Provider<Map<String, Provider<AppCenterNetworkService>>>,
)
