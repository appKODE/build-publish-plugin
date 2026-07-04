package ru.kode.android.build.publish.plugin.nextcloud

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.core.task.registerStandaloneServiceTask
import ru.kode.android.build.publish.plugin.core.util.applyWithOptionalAndroid
import ru.kode.android.build.publish.plugin.core.util.getOrRegisterLoggerService
import ru.kode.android.build.publish.plugin.core.util.resolveStandaloneService
import ru.kode.android.build.publish.plugin.core.util.serviceName
import ru.kode.android.build.publish.plugin.nextcloud.extension.BuildPublishNextcloudExtension
import ru.kode.android.build.publish.plugin.nextcloud.messages.noAuthConfigsMessage
import ru.kode.android.build.publish.plugin.nextcloud.messages.registeringServicesMessage
import ru.kode.android.build.publish.plugin.nextcloud.messages.servicesCreatedMessage
import ru.kode.android.build.publish.plugin.nextcloud.service.NextcloudService
import ru.kode.android.build.publish.plugin.nextcloud.service.NextcloudServiceExtension
import ru.kode.android.build.publish.plugin.nextcloud.task.standalone.UploadToNextcloudTask

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

        val servicesProperty: MapProperty<String, Provider<*>> =
            project.objects.mapProperty(String::class.java, Provider::class.java)
        servicesProperty.set(emptyMap())

        project.extensions.create(
            SERVICE_EXTENSION_NAME,
            NextcloudServiceExtension::class.java,
            servicesProperty,
        )

        project.applyWithOptionalAndroid {
            setupServicesAndTasks(project, extension, servicesProperty)
        }
    }

    private fun setupServicesAndTasks(
        project: Project,
        extension: BuildPublishNextcloudExtension,
        servicesProperty: MapProperty<String, Provider<*>>,
    ) {
        val loggerProvider = project.getOrRegisterLoggerService()
        val logger = loggerProvider.get()

        if (extension.auth.isEmpty()) {
            logger.info(noAuthConfigsMessage())
            return
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

                name to (service as Provider<*>)
            }

        logger.info(servicesCreatedMessage(serviceMap.keys))
        servicesProperty.set(serviceMap)

        val standaloneService = serviceMap.resolveStandaloneService<NextcloudService>()
        registerStandaloneTasks(project, standaloneService, loggerProvider)
    }

    private fun registerStandaloneTasks(
        project: Project,
        service: Provider<NextcloudService>,
        loggerProvider: Provider<LoggerService>,
    ) {
        project.registerStandaloneServiceTask<UploadToNextcloudTask, NextcloudService>(
            TaskNames.Nextcloud.UPLOAD,
            service,
            loggerProvider,
        )
    }
}
