@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.jira

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.StopExecutionException
import ru.kode.android.build.publish.plugin.core.logger.LoggerServiceExtension
import ru.kode.android.build.publish.plugin.core.util.serviceName
import ru.kode.android.build.publish.plugin.foundation.BuildPublishFoundationPlugin
import ru.kode.android.build.publish.plugin.jira.controller.mappers.mapToEntity
import ru.kode.android.build.publish.plugin.jira.controller.mappers.toJson
import ru.kode.android.build.publish.plugin.jira.extension.BuildPublishJiraExtension
import ru.kode.android.build.publish.plugin.jira.messages.jiraServicesCreatedMessage
import ru.kode.android.build.publish.plugin.jira.messages.mustApplyFoundationPluginMessage
import ru.kode.android.build.publish.plugin.jira.messages.noEndpointsConfigsMessage
import ru.kode.android.build.publish.plugin.jira.messages.pluginInitializedMessage
import ru.kode.android.build.publish.plugin.jira.messages.registeringServicesMessage
import ru.kode.android.build.publish.plugin.jira.service.JiraServiceExtension
import ru.kode.android.build.publish.plugin.jira.service.network.JiraService

internal const val EXTENSION_NAME = "buildPublishJira"
private const val SERVICE_NAME = "jiraService"
private const val SERVICE_EXTENSION_NAME = "jiraServiceExtension"

/**
 * Gradle plugin for integrating Jira issue tracking with Android build and publish workflows.
 *
 * This plugin provides functionality to interact with Jira during the build process,
 * enabling features like:
 * - Updating Jira issues with build information
 * - Validating Jira issue statuses before release
 * - Automating release notes generation from Jira issues
 *
 * @see JiraService For the underlying network communication
 * @see BuildPublishJiraExtension For configuration options
 */
abstract class BuildPublishJiraPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create(EXTENSION_NAME, BuildPublishJiraExtension::class.java)

        val servicesProperty =
            project.objects.mapProperty(
                String::class.java,
                Provider::class.java,
            )

        servicesProperty.set(emptyMap())

        project.extensions.create(
            SERVICE_EXTENSION_NAME,
            JiraServiceExtension::class.java,
            servicesProperty,
        )

        if (!project.plugins.hasPlugin(BuildPublishFoundationPlugin::class.java)) {
            throw StopExecutionException(mustApplyFoundationPluginMessage())
        }

        val androidExtension =
            project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl {
            val loggerProvider =
                project.extensions.getByType(LoggerServiceExtension::class.java)
                    .service

            val logger = loggerProvider.get()

            if (extension.endpoints.isEmpty()) {
                logger.info(noEndpointsConfigsMessage())
                return@finalizeDsl
            }

            logger.info(registeringServicesMessage())

            val serviceMap =
                extension.endpoints.associate { endpointsConfig ->
                    val name = endpointsConfig.name
                    val registered =
                        project.gradle.sharedServices.registerIfAbsent(
                            project.serviceName(SERVICE_NAME, name),
                            JiraService::class.java,
                        ) {
                            it.maxParallelUsages.set(1)
                            it.parameters.endpoints.set(endpointsConfig.endpoints.map { it.mapToEntity().toJson() })
                            it.parameters.loggerService.set(loggerProvider)
                        }
                    name to registered
                }

            logger.info(jiraServicesCreatedMessage(serviceMap.keys))

            servicesProperty.set(serviceMap)

            logger.info(
                pluginInitializedMessage(
                    extension.endpoints.names,
                    extension.automation.names,
                ),
            )
        }
    }
}
