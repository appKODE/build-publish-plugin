package ru.kode.android.build.publish.plugin.telegram

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
import ru.kode.android.build.publish.plugin.telegram.controller.mappers.mapToEntity
import ru.kode.android.build.publish.plugin.telegram.controller.mappers.toJson
import ru.kode.android.build.publish.plugin.telegram.extension.BuildPublishTelegramExtension
import ru.kode.android.build.publish.plugin.telegram.messages.noBotsConfiguredMessage
import ru.kode.android.build.publish.plugin.telegram.messages.registeringServiceMessage
import ru.kode.android.build.publish.plugin.telegram.messages.telegramServicesCreated
import ru.kode.android.build.publish.plugin.telegram.service.TelegramService
import ru.kode.android.build.publish.plugin.telegram.service.TelegramServiceExtension
import ru.kode.android.build.publish.plugin.telegram.task.standalone.SendTelegramFileTask
import ru.kode.android.build.publish.plugin.telegram.task.standalone.SendTelegramMessageTask

internal const val EXTENSION_NAME = "buildPublishTelegram"
internal const val SERVICE_EXTENSION_NAME = "telegramServiceExtension"

private const val SERVICE_NAME = "telegramService"

abstract class BuildPublishTelegramPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, BuildPublishTelegramExtension::class.java)

        @Suppress("UNCHECKED_CAST")
        val servicesProperty =
            project.objects.mapProperty(String::class.java, Provider::class.java)
                as MapProperty<String, Provider<*>>
        servicesProperty.set(emptyMap())

        project.extensions.create(SERVICE_EXTENSION_NAME, TelegramServiceExtension::class.java, servicesProperty)

        project.applyWithOptionalAndroid {
            setupServicesAndTasks(project, extension, servicesProperty)
        }
    }

    private fun setupServicesAndTasks(
        project: Project,
        extension: BuildPublishTelegramExtension,
        servicesProperty: MapProperty<String, Provider<*>>,
    ) {
        val loggerProvider = project.getOrRegisterLoggerService()
        val logger = loggerProvider.get()

        if (extension.bots.isEmpty()) {
            logger.info(noBotsConfiguredMessage())
            return
        }

        logger.info(registeringServiceMessage())

        val serviceMap: Map<String, Provider<TelegramService>> =
            extension.bots.associate { botConfig ->
                val name = botConfig.name
                val service =
                    project.gradle.sharedServices.registerIfAbsent(
                        project.serviceName(SERVICE_NAME, name),
                        TelegramService::class.java,
                    ) { spec ->
                        spec.maxParallelUsages.set(1)
                        spec.parameters.bots.set(botConfig.bots.map { it.mapToEntity().toJson() })
                        spec.parameters.loggerService.set(loggerProvider)
                    }
                name to service
            }

        logger.info(telegramServicesCreated(serviceMap.keys))
        @Suppress("UNCHECKED_CAST")
        servicesProperty.set(serviceMap as Map<String, Provider<*>>)

        val standaloneService = serviceMap.resolveStandaloneService<TelegramService>()
        registerStandaloneTasks(project, standaloneService, loggerProvider)
    }

    private fun registerStandaloneTasks(
        project: Project,
        service: Provider<TelegramService>,
        loggerProvider: Provider<LoggerService>,
    ) {
        project.registerStandaloneServiceTask<SendTelegramMessageTask, TelegramService>(
            TaskNames.Telegram.SEND_MESSAGE,
            service,
            loggerProvider,
        )
        project.registerStandaloneServiceTask<SendTelegramFileTask, TelegramService>(
            TaskNames.Telegram.SEND_FILE,
            service,
            loggerProvider,
        )
    }
}
