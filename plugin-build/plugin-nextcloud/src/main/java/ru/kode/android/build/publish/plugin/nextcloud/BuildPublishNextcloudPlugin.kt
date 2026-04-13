@file:Suppress("UnstableApiUsage")

package ru.kode.android.build.publish.plugin.nextcloud

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.StopExecutionException
import ru.kode.android.build.publish.plugin.core.logger.LoggerServiceExtension
import ru.kode.android.build.publish.plugin.core.util.serviceName
import ru.kode.android.build.publish.plugin.foundation.BuildPublishFoundationPlugin
import ru.kode.android.build.publish.plugin.nextcloud.extension.BuildPublishNextcloudExtension
import ru.kode.android.build.publish.plugin.nextcloud.messages.foundationPluginNotFoundException
import ru.kode.android.build.publish.plugin.nextcloud.messages.noAuthConfigsMessage
import ru.kode.android.build.publish.plugin.nextcloud.messages.registeringServicesMessage
import ru.kode.android.build.publish.plugin.nextcloud.messages.servicesCreatedMessage
import ru.kode.android.build.publish.plugin.nextcloud.service.NextcloudService
import ru.kode.android.build.publish.plugin.nextcloud.service.NextcloudServiceExtension

internal const val EXTENSION_NAME = "buildPublishNextcloud"
private const val SERVICE_NAME = "nextcloudService"
private const val SERVICE_EXTENSION_NAME = "nextcloudServiceExtension"

/**
 * A Gradle plugin that provides Nextcloud integration for build publishing.
 *
 * This plugin enables:
 * - Uploading build artifacts to Nextcloud folders
 * - Creating or reusing public share links for uploaded artifacts
 * - Authentication with multiple Nextcloud instances
 *
 * It sets up the necessary services and extensions for Nextcloud integration,
 * including network services for API communication.
 */
abstract class BuildPublishNextcloudPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create(
                EXTENSION_NAME,
                BuildPublishNextcloudExtension::class.java,
            )

        val servicesProperty =
            project.objects.mapProperty(
                String::class.java,
                Provider::class.java,
            )
        servicesProperty.set(emptyMap())

        project.extensions.create(
            SERVICE_EXTENSION_NAME,
            NextcloudServiceExtension::class.java,
            servicesProperty,
        )

        if (!project.plugins.hasPlugin(BuildPublishFoundationPlugin::class.java)) {
            throw StopExecutionException(foundationPluginNotFoundException())
        }

        val androidExtension =
            project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)

        androidExtension.finalizeDsl {
            val loggerProvider =
                project.extensions.getByType(LoggerServiceExtension::class.java)
                    .service

            val logger = loggerProvider.get()

            if (extension.auth.isEmpty()) {
                logger.info(noAuthConfigsMessage())
                return@finalizeDsl
            }

            logger.info(registeringServicesMessage())

            val serviceMap =
                extension.auth.associate { authConfig ->
                    val name = authConfig.name

                    val service =
                        project.gradle.sharedServices.registerIfAbsent(
                            project.serviceName(SERVICE_NAME, name),
                            NextcloudService::class.java,
                        ) {
                            it.maxParallelUsages.set(1)
                            it.parameters.credentials.set(authConfig.credentials)
                            it.parameters.baseUrl.set(authConfig.baseUrl)
                            it.parameters.loggerService.set(loggerProvider)
                        }

                    name to service
                }

            logger.info(servicesCreatedMessage(serviceMap.keys))
            servicesProperty.set(serviceMap)
        }
    }
}
