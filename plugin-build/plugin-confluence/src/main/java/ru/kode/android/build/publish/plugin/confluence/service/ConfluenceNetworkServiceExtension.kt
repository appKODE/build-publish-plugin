package ru.kode.android.build.publish.plugin.jira.service

import org.gradle.api.provider.Provider

abstract class ConfluenceNetworkServiceExtension(
    val services: Provider<Map<String, Provider<ConfluenceNetworkService>>>
)
