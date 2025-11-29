@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.jira

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.StopExecutionException
import ru.kode.android.build.publish.plugin.core.util.serviceName
import ru.kode.android.build.publish.plugin.foundation.BuildPublishFoundationPlugin
import ru.kode.android.build.publish.plugin.jira.extension.BuildPublishJiraExtension
import ru.kode.android.build.publish.plugin.jira.service.JiraServiceExtension
import ru.kode.android.build.publish.plugin.jira.service.network.JiraNetworkService

private const val EXTENSION_NAME = "buildPublishJira"
private const val NETWORK_SERVICE_NAME = "jiraNetworkService"
private const val NETWORK_SERVICE_EXTENSION_NAME = "jiraNetworkServiceExtension"

/**
 * Gradle plugin for integrating Jira issue tracking with Android build and publish workflows.
 *
 * This plugin provides functionality to interact with Jira during the build process,
 * enabling features like:
 * - Updating Jira issues with build information
 * - Validating Jira issue statuses before release
 * - Automating release notes generation from Jira issues
 *
 * @see JiraNetworkService For the underlying network communication
 * @see BuildPublishJiraExtension For configuration options
 */
abstract class BuildPublishJiraPlugin : Plugin<Project> {
    private val logger: Logger = Logging.getLogger(this::class.java)

    override fun apply(project: Project) {
        val extension =
            project.extensions
                .create(EXTENSION_NAME, BuildPublishJiraExtension::class.java)

        val androidExtension =
            project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)

        if (!project.plugins.hasPlugin(BuildPublishFoundationPlugin::class.java)) {
            throw StopExecutionException(
                "Must only be used with BuildPublishFoundationPlugin." +
                    " Please apply the 'ru.kode.android.build-publish-novo.foundation' plugin.",
            )
        }

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
                                    it.parameters.credentials.set(authConfig.credentials)
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
            logger.info("Jira plugin executed: auth=${extension.auth.asMap}; automation=${extension.automation.asMap}")
        }
    }
}
