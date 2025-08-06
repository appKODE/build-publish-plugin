package ru.kode.android.build.publish.plugin.appcenter.service

import org.gradle.api.provider.Provider

abstract class AppCenterNetworkServiceExtension(
    val services: Provider<Map<String, Provider<AppCenterNetworkService>>>
)
