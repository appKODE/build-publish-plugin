package ru.kode.android.build.publish.plugin.jira.service

import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.jira.service.network.JiraNetworkService

abstract class JiraServiceExtension(
    val networkServices: Provider<Map<String, Provider<JiraNetworkService>>>,
)
