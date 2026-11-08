package ru.kode.android.build.publish.plugin.vk

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.core.task.registerStandaloneServiceTask
import ru.kode.android.build.publish.plugin.vk.extension.BuildPublishVkExtension
import ru.kode.android.build.publish.plugin.vk.messages.noAuthConfigsMessage
import ru.kode.android.build.publish.plugin.vk.messages.registeringServicesMessage
import ru.kode.android.build.publish.plugin.vk.messages.servicesCreatedMessage
import ru.kode.android.build.publish.plugin.vk.service.VkService
import ru.kode.android.build.publish.plugin.vk.service.VkServiceExtension
import ru.kode.android.build.publish.plugin.vk.task.standalone.UploadToVkTask
import ru.kode.android.gradle.commons.logger.LoggerService
import ru.kode.android.gradle.commons.util.applyWithOptionalAndroid
import ru.kode.android.gradle.commons.util.getOrRegisterLoggerService
import ru.kode.android.gradle.commons.util.resolveStandaloneService
import ru.kode.android.gradle.commons.util.serviceName

internal const val EXTENSION_NAME = "buildPublishVk"
private const val SERVICE_NAME = "vkService"
private const val SERVICE_EXTENSION_NAME = "vkServiceExtension"

/**
 * A Gradle plugin that sends build artifacts as documents to a VK conversation.
 */
abstract class BuildPublishVkPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create(
                EXTENSION_NAME,
                BuildPublishVkExtension::class.java,
            )

        val servicesProperty: MapProperty<String, Provider<*>> =
            project.objects.mapProperty(String::class.java, Provider::class.java)
        servicesProperty.set(emptyMap())

        project.extensions.create(
            SERVICE_EXTENSION_NAME,
            VkServiceExtension::class.java,
            servicesProperty,
        )

        project.applyWithOptionalAndroid {
            setupServicesAndTasks(project, extension, servicesProperty)
        }
    }

    private fun setupServicesAndTasks(
        project: Project,
        extension: BuildPublishVkExtension,
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
                        VkService::class.java,
                    ) {
                        it.maxParallelUsages.set(1)
                        it.parameters.accessTokenFile.set(authConfig.accessTokenFile)
                        it.parameters.loggerService.set(loggerProvider)
                    }

                name to (service as Provider<*>)
            }

        logger.info(servicesCreatedMessage(serviceMap.keys))
        servicesProperty.set(serviceMap)

        val standaloneService = serviceMap.resolveStandaloneService<VkService>()
        registerStandaloneTasks(project, standaloneService, loggerProvider)
    }

    private fun registerStandaloneTasks(
        project: Project,
        service: Provider<VkService>,
        loggerProvider: Provider<LoggerService>,
    ) {
        project.registerStandaloneServiceTask<UploadToVkTask, VkService>(
            TaskNames.Vk.UPLOAD,
            service,
            loggerProvider,
        )
    }
}
