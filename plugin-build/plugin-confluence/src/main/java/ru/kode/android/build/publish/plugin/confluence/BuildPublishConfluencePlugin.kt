package ru.kode.android.build.publish.plugin.confluence

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.confluence.extension.BuildPublishConfluenceExtension
import ru.kode.android.build.publish.plugin.confluence.messages.noAuthConfigsMessage
import ru.kode.android.build.publish.plugin.confluence.messages.registeringServicesMessage
import ru.kode.android.build.publish.plugin.confluence.messages.servicesCreatedMessage
import ru.kode.android.build.publish.plugin.confluence.service.ConfluenceService
import ru.kode.android.build.publish.plugin.confluence.service.ConfluenceServiceExtension
import ru.kode.android.build.publish.plugin.confluence.task.standalone.AddConfluenceCommentTask
import ru.kode.android.build.publish.plugin.confluence.task.standalone.UploadToConfluenceTask
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.core.task.registerStandaloneServiceTask
import ru.kode.android.build.publish.plugin.core.util.applyWithOptionalAndroid
import ru.kode.android.build.publish.plugin.core.util.getOrRegisterLoggerService
import ru.kode.android.build.publish.plugin.core.util.resolveStandaloneService
import ru.kode.android.build.publish.plugin.core.util.serviceName

internal const val EXTENSION_NAME = "buildPublishConfluence"
private const val SERVICE_NAME = "confluenceService"
private const val SERVICE_EXTENSION_NAME = "confluenceServiceExtension"

/**
 * A Gradle plugin that provides Confluence integration for build publishing.
 *
 * This plugin enables:
 * - Uploading build artifacts to Confluence pages
 * - Managing Confluence content as part of the build process
 * - Authentication with multiple Confluence instances
 *
 * It sets up the necessary services and extensions for Confluence integration,
 * including network services for API communication.
 */
abstract class BuildPublishConfluencePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create(
                EXTENSION_NAME,
                BuildPublishConfluenceExtension::class.java,
            )

        val servicesProperty: MapProperty<String, Provider<*>> =
            project.objects.mapProperty(String::class.java, Provider::class.java)
        servicesProperty.set(emptyMap())

        project.extensions.create(
            SERVICE_EXTENSION_NAME,
            ConfluenceServiceExtension::class.java,
            servicesProperty,
        )

        project.applyWithOptionalAndroid {
            setupServicesAndTasks(project, extension, servicesProperty)
        }
    }

    private fun setupServicesAndTasks(
        project: Project,
        extension: BuildPublishConfluenceExtension,
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
                        ConfluenceService::class.java,
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

        val standaloneService = serviceMap.resolveStandaloneService<ConfluenceService>()
        registerStandaloneTasks(project, standaloneService, loggerProvider)
    }

    private fun registerStandaloneTasks(
        project: Project,
        service: Provider<ConfluenceService>,
        loggerProvider: Provider<LoggerService>,
    ) {
        project.registerStandaloneServiceTask<UploadToConfluenceTask, ConfluenceService>(
            TaskNames.Confluence.UPLOAD,
            service,
            loggerProvider,
        )
        project.registerStandaloneServiceTask<AddConfluenceCommentTask, ConfluenceService>(
            TaskNames.Confluence.ADD_COMMENT,
            service,
            loggerProvider,
        )
    }
}
