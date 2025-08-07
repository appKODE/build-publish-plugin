package ru.kode.android.build.publish.plugin.confluence.service

import org.gradle.api.provider.Provider

abstract class ConfluenceNetworkServiceExtension(
    val services: Provider<Map<String, Provider<ConfluenceNetworkService>>>,
)
