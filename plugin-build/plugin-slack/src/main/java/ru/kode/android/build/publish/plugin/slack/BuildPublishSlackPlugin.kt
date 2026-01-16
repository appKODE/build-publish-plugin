@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.slack

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.StopExecutionException
import ru.kode.android.build.publish.plugin.core.logger.LoggerServiceExtension
import ru.kode.android.build.publish.plugin.core.util.serviceName
import ru.kode.android.build.publish.plugin.foundation.BuildPublishFoundationPlugin
import ru.kode.android.build.publish.plugin.slack.extension.BuildPublishSlackExtension
import ru.kode.android.build.publish.plugin.slack.messages.mustApplyFoundationPluginMessage
import ru.kode.android.build.publish.plugin.slack.messages.noBotsConfiguredMessage
import ru.kode.android.build.publish.plugin.slack.messages.registeringServicesMessage
import ru.kode.android.build.publish.plugin.slack.messages.servicesCreatedMessages
import ru.kode.android.build.publish.plugin.slack.service.SlackService
import ru.kode.android.build.publish.plugin.slack.service.SlackServiceExtension

internal const val EXTENSION_NAME = "buildPublishSlack"
private const val SERVICE_NAME = "slackService"
private const val SERVICE_EXTENSION_NAME = "slackServiceExtension"

/**
 * A Gradle plugin that provides Slack integration for build publishing.
 *
 * This plugin enables:
 * - Sending build notifications to Slack channels
 * - Uploading build artifacts to Slack
 * - Configuring multiple Slack workspaces and channels
 * - Customizing notification messages and channels
 *
 * It sets up the necessary services for Slack webhook and file upload functionality,
 * and provides extensions for build scripts to configure Slack integration.
 */
abstract class BuildPublishSlackPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create(
                EXTENSION_NAME,
                BuildPublishSlackExtension::class.java,
            )

        val servicesProperty =
            project.objects.mapProperty(
                String::class.java,
                Provider::class.java,
            )
        servicesProperty.set(emptyMap())

        project.extensions.create(
            SERVICE_EXTENSION_NAME,
            SlackServiceExtension::class.java,
            servicesProperty,
        )

        if (!project.plugins.hasPlugin(BuildPublishFoundationPlugin::class.java)) {
            throw StopExecutionException(
                mustApplyFoundationPluginMessage(),
            )
        }

        val androidExtension = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl {
            val loggerProvider =
                project.extensions.getByType(LoggerServiceExtension::class.java)
                    .service

            val logger = loggerProvider.get()

            if (extension.bot.isEmpty()) {
                logger.info(noBotsConfiguredMessage())
                return@finalizeDsl
            }

            logger.info(registeringServicesMessage())

            val serviceMap =
                extension.bot.associate { authConfig ->
                    val name = authConfig.name
                    val registered =
                        project.gradle.sharedServices.registerIfAbsent(
                            project.serviceName(SERVICE_NAME, name),
                            SlackService::class.java,
                        ) {
                            it.maxParallelUsages.set(1)
                            it.parameters.webhookUrl.set(authConfig.webhookUrl)
                            it.parameters.uploadApiTokenFile.set(authConfig.uploadApiTokenFile)
                            it.parameters.loggerService.set(loggerProvider)
                        }
                    name to registered
                }

            logger.info(servicesCreatedMessages(serviceMap.keys))

            servicesProperty.set(serviceMap)
        }
    }
}
