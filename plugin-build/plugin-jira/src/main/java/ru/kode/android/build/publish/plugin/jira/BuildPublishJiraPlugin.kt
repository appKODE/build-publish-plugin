@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.jira

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.core.util.serviceName
import ru.kode.android.build.publish.plugin.jira.extension.BuildPublishJiraExtension
import ru.kode.android.build.publish.plugin.jira.service.JiraServiceExtension
import ru.kode.android.build.publish.plugin.jira.service.network.JiraNetworkService

private const val EXTENSION_NAME = "buildPublishJira"
private const val NETWORK_SERVICE_NAME = "jiraNetworkService"
private const val NETWORK_SERVICE_EXTENSION_NAME = "jiraNetworkServiceExtension"

abstract class BuildPublishJiraPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions
                .create(EXTENSION_NAME, BuildPublishJiraExtension::class.java)

        val androidExtension =
            project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl {
            val services: Provider<Map<String, Provider<JiraNetworkService>>> =
                project.provider {
                    extension.auth.fold(mapOf()) { acc, authConfig ->
                        val service =
                            project.gradle.sharedServices.registerIfAbsent(
                                project.serviceName(NETWORK_SERVICE_NAME, authConfig.name),
                                JiraNetworkService::class.java,
                                {
                                    it.maxParallelUsages.set(1)
                                    it.parameters.password.set(authConfig.authPassword)
                                    it.parameters.username.set(authConfig.authUsername)
                                    it.parameters.baseUrl.set(authConfig.baseUrl)
                                },
                            )
                        acc.toMutableMap().apply {
                            put(authConfig.name, service)
                        }
                    }
                }
            project.extensions.create(
                NETWORK_SERVICE_EXTENSION_NAME,
                JiraServiceExtension::class.java,
                services,
            )
        }
    }
}
