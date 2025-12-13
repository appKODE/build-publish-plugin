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
import ru.kode.android.build.publish.plugin.jira.service.network.JiraService

private const val EXTENSION_NAME = "buildPublishJira"
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

    private val logger: Logger = Logging.getLogger(this::class.java)

    override fun apply(project: Project) {

        val extension =
            project.extensions.create(EXTENSION_NAME, BuildPublishJiraExtension::class.java)

        val servicesProperty =
            project.objects.mapProperty(
                String::class.java,
                Provider::class.java
            )

        servicesProperty.set(emptyMap())

        project.extensions.create(
            SERVICE_EXTENSION_NAME,
            JiraServiceExtension::class.java,
            servicesProperty
        )

        logger.info("JiraServiceExtension created (empty)")

        if (!project.plugins.hasPlugin(BuildPublishFoundationPlugin::class.java)) {
            throw StopExecutionException(
                "Must only be used with BuildPublishFoundationPlugin. " +
                    "Please apply 'ru.kode.android.build-publish-novo.foundation'."
            )
        }

        val androidExtension =
            project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl {
            if (extension.auth.isEmpty()) {
                logger.info("Jira: no auth configs — leaving service map empty")
                return@finalizeDsl
            }

            logger.info("Jira: registering services...")

            val serviceMap = extension.auth.associate { authConfig ->
                val name = authConfig.name
                val registered = project.gradle.sharedServices.registerIfAbsent(
                    project.serviceName(SERVICE_NAME, name),
                    JiraService::class.java
                ) {
                    it.maxParallelUsages.set(1)
                    it.parameters.credentials.set(authConfig.credentials)
                    it.parameters.baseUrl.set(authConfig.baseUrl)
                }
                name to registered
            }

            logger.info("Jira services created: ${serviceMap.keys}")

            servicesProperty.set(serviceMap)

            logger.info(
                "Jira plugin finalizeDsl: auth=${extension.auth.names}; automation=${extension.automation.names}"
            )
        }
    }
}
