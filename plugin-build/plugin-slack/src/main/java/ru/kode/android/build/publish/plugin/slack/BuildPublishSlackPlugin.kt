package ru.kode.android.build.publish.plugin.slack

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
import ru.kode.android.build.publish.plugin.slack.extension.BuildPublishSlackExtension
import ru.kode.android.build.publish.plugin.slack.messages.noBotsConfiguredMessage
import ru.kode.android.build.publish.plugin.slack.messages.registeringServicesMessage
import ru.kode.android.build.publish.plugin.slack.messages.servicesCreatedMessages
import ru.kode.android.build.publish.plugin.slack.service.SlackService
import ru.kode.android.build.publish.plugin.slack.service.SlackServiceExtension
import ru.kode.android.build.publish.plugin.slack.task.standalone.SendSlackFileTask
import ru.kode.android.build.publish.plugin.slack.task.standalone.SendSlackMessageTask

internal const val EXTENSION_NAME = "buildPublishSlack"
private const val SERVICE_NAME = "slackService"
private const val SERVICE_EXTENSION_NAME = "slackServiceExtension"

abstract class BuildPublishSlackPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(EXTENSION_NAME, BuildPublishSlackExtension::class.java)

        @Suppress("UNCHECKED_CAST")
        val servicesProperty =
            project.objects.mapProperty(String::class.java, Provider::class.java)
                as MapProperty<String, Provider<*>>
        servicesProperty.set(emptyMap())

        project.extensions.create(SERVICE_EXTENSION_NAME, SlackServiceExtension::class.java, servicesProperty)

        project.applyWithOptionalAndroid {
            setupServicesAndTasks(project, extension, servicesProperty)
        }
    }

    private fun setupServicesAndTasks(
        project: Project,
        extension: BuildPublishSlackExtension,
        servicesProperty: MapProperty<String, Provider<*>>,
    ) {
        val loggerProvider = project.getOrRegisterLoggerService()
        val logger = loggerProvider.get()

        if (extension.bot.isEmpty()) {
            logger.info(noBotsConfiguredMessage())
            return
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
                name to (registered as Provider<*>)
            }

        logger.info(servicesCreatedMessages(serviceMap.keys))
        servicesProperty.set(serviceMap)

        val standaloneService = serviceMap.resolveStandaloneService<SlackService>()
        registerStandaloneTasks(project, standaloneService, loggerProvider)
    }

    private fun registerStandaloneTasks(
        project: Project,
        service: Provider<SlackService>,
        loggerProvider: Provider<LoggerService>,
    ) {
        project.registerStandaloneServiceTask<SendSlackMessageTask, SlackService>(
            TaskNames.Slack.SEND_MESSAGE,
            service,
            loggerProvider,
        )
        project.registerStandaloneServiceTask<SendSlackFileTask, SlackService>(
            TaskNames.Slack.SEND_FILE,
            service,
            loggerProvider,
        )
    }
}
