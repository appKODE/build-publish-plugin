package ru.kode.android.build.publish.plugin.clickup

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.clickup.extension.BuildPublishClickUpExtension
import ru.kode.android.build.publish.plugin.clickup.messages.noAuthConfigMessage
import ru.kode.android.build.publish.plugin.clickup.messages.registeringServicesMessage
import ru.kode.android.build.publish.plugin.clickup.messages.servicesCreatedMessage
import ru.kode.android.build.publish.plugin.clickup.service.ClickUpServiceExtension
import ru.kode.android.build.publish.plugin.clickup.service.network.ClickUpService
import ru.kode.android.build.publish.plugin.clickup.task.standalone.AddClickUpFixVersionTask
import ru.kode.android.build.publish.plugin.clickup.task.standalone.AddClickUpTagTask
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.core.task.registerStandaloneServiceTask
import ru.kode.android.build.publish.plugin.core.util.applyWithOptionalAndroid
import ru.kode.android.build.publish.plugin.core.util.getOrRegisterLoggerService
import ru.kode.android.build.publish.plugin.core.util.serviceName

internal const val EXTENSION_NAME = "buildPublishClickUp"
private const val SERVICE_NAME = "clickUpService"
private const val SERVICE_EXTENSION_NAME = "clickUpServiceExtension"

/**
 * A Gradle plugin that integrates ClickUp task management with the Android build process.
 *
 * This plugin provides functionality to automate ClickUp tasks during the build process,
 * such as updating task statuses, adding tags, and setting custom fields.
 *
 * @see BuildPublishClickUpExtension For configuration options
 * @see ClickUpService For the underlying network operations
 */
abstract class BuildPublishClickUpPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create(
                EXTENSION_NAME,
                BuildPublishClickUpExtension::class.java,
            )

        val servicesProperty: MapProperty<String, Provider<*>> =
            project.objects.mapProperty(String::class.java, Provider::class.java)
        servicesProperty.set(emptyMap())

        project.extensions.create(
            SERVICE_EXTENSION_NAME,
            ClickUpServiceExtension::class.java,
            servicesProperty,
        )

        project.applyWithOptionalAndroid {
            setupServicesAndTasks(project, extension, servicesProperty)
        }
    }

    private fun setupServicesAndTasks(
        project: Project,
        extension: BuildPublishClickUpExtension,
        servicesProperty: MapProperty<String, Provider<*>>,
    ) {
        val loggerProvider = project.getOrRegisterLoggerService()
        val logger = loggerProvider.get()

        if (extension.auth.isEmpty()) {
            logger.info(noAuthConfigMessage())
            return
        }

        logger.info(registeringServicesMessage())

        val serviceMap =
            extension.auth.associate { authConfig ->
                val name = authConfig.name

                val service =
                    project.gradle.sharedServices.registerIfAbsent(
                        project.serviceName(SERVICE_NAME, name),
                        ClickUpService::class.java,
                    ) {
                        it.maxParallelUsages.set(1)
                        it.parameters.token.set(authConfig.apiTokenFile)
                        it.parameters.loggerService.set(loggerProvider)
                    }

                name to (service as Provider<*>)
            }

        logger.info(servicesCreatedMessage(serviceMap.keys))

        servicesProperty.set(serviceMap)

        @Suppress("UNCHECKED_CAST")
        val standaloneService =
            (serviceMap["common"] ?: serviceMap.values.first()) as Provider<ClickUpService>
        registerStandaloneTasks(project, standaloneService, loggerProvider)
    }

    private fun registerStandaloneTasks(
        project: Project,
        service: Provider<ClickUpService>,
        loggerProvider: Provider<LoggerService>,
    ) {
        project.registerStandaloneServiceTask<AddClickUpTagTask, ClickUpService>(
            TaskNames.ClickUp.ADD_TAG,
            service,
            loggerProvider,
        )
        project.registerStandaloneServiceTask<AddClickUpFixVersionTask, ClickUpService>(
            TaskNames.ClickUp.ADD_FIX_VERSION,
            service,
            loggerProvider,
        )
    }
}
