package ru.kode.android.build.publish.plugin.jira

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.core.task.TaskNames
import ru.kode.android.build.publish.plugin.core.util.COMMON_CONTAINER_NAME
import ru.kode.android.build.publish.plugin.core.util.applyWithOptionalAndroid
import ru.kode.android.build.publish.plugin.core.util.getOrRegisterLoggerService
import ru.kode.android.build.publish.plugin.core.util.serviceName
import ru.kode.android.build.publish.plugin.jira.extension.BuildPublishJiraExtension
import ru.kode.android.build.publish.plugin.jira.messages.jiraServicesCreatedMessage
import ru.kode.android.build.publish.plugin.jira.messages.noAuthConfigsMessage
import ru.kode.android.build.publish.plugin.jira.messages.pluginInitializedMessage
import ru.kode.android.build.publish.plugin.jira.messages.registeringServicesMessage
import ru.kode.android.build.publish.plugin.jira.service.JiraServiceExtension
import ru.kode.android.build.publish.plugin.jira.service.network.JiraService
import ru.kode.android.build.publish.plugin.jira.task.standalone.AddJiraFixVersionTask
import ru.kode.android.build.publish.plugin.jira.task.standalone.AddJiraLabelTask
import ru.kode.android.build.publish.plugin.jira.task.standalone.TransitionJiraIssueTask

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

        val servicesProperty: MapProperty<String, Provider<*>> =
            project.objects.mapProperty(String::class.java, Provider::class.java)

        servicesProperty.set(emptyMap())

        project.extensions.create(
            SERVICE_EXTENSION_NAME,
            JiraServiceExtension::class.java,
            servicesProperty,
        )

        project.applyWithOptionalAndroid {
            setupServicesAndTasks(project, extension, servicesProperty)
        }
    }

    private fun setupServicesAndTasks(
        project: Project,
        extension: BuildPublishJiraExtension,
        servicesProperty: MapProperty<String, Provider<*>>,
    ) {
        val loggerProvider = project.getOrRegisterLoggerService()
        val logger = loggerProvider.get()

        // Flatten the instances declared across every auth config (`common` / `buildVariant(...)`)
        // into a single service per named instance. Each instance carries its own base URL and
        // credentials; projects and standalone tasks select one by its name via `instanceName`.
        val instances = extension.auth.flatMap { it.instances }
        if (instances.isEmpty()) {
            logger.info(noAuthConfigsMessage())
            return
        }

        logger.info(registeringServicesMessage())

        val serviceMap =
            instances.associate { instance ->
                val name = instance.name
                val registered =
                    project.gradle.sharedServices.registerIfAbsent(
                        project.serviceName(SERVICE_NAME, name),
                        JiraService::class.java,
                    ) {
                        it.maxParallelUsages.set(1)
                        it.parameters.credentials.set(instance.credentials)
                        it.parameters.baseUrl.set(instance.baseUrl)
                        it.parameters.loggerService.set(loggerProvider)
                    }
                name to (registered as Provider<*>)
            }

        logger.info(jiraServicesCreatedMessage(serviceMap.keys))

        servicesProperty.set(serviceMap)

        logger.info(
            pluginInitializedMessage(
                serviceMap.keys,
                extension.automation.names,
            ),
        )

        @Suppress("UNCHECKED_CAST")
        val typedServiceMap =
            serviceMap.mapValues { (_, provider) -> provider as Provider<JiraService> }
        val defaultService =
            typedServiceMap[COMMON_CONTAINER_NAME] ?: typedServiceMap.values.first()
        registerStandaloneTasks(project, typedServiceMap, defaultService, loggerProvider)
    }

    private fun registerStandaloneTasks(
        project: Project,
        services: Map<String, Provider<JiraService>>,
        defaultService: Provider<JiraService>,
        loggerProvider: Provider<LoggerService>,
    ) {
        project.tasks.register(TaskNames.Jira.ADD_FIX_VERSION, AddJiraFixVersionTask::class.java) { task ->
            task.service.set(defaultService)
            task.loggerService.set(loggerProvider)
            services.forEach { (name, provider) ->
                task.services.put(name, provider)
                task.usesService(provider)
            }
            task.usesService(defaultService)
            task.usesService(loggerProvider)
        }
        project.tasks.register(TaskNames.Jira.ADD_LABEL, AddJiraLabelTask::class.java) { task ->
            task.service.set(defaultService)
            task.loggerService.set(loggerProvider)
            services.forEach { (name, provider) ->
                task.services.put(name, provider)
                task.usesService(provider)
            }
            task.usesService(defaultService)
            task.usesService(loggerProvider)
        }
        project.tasks.register(TaskNames.Jira.TRANSITION_ISSUE, TransitionJiraIssueTask::class.java) { task ->
            task.service.set(defaultService)
            task.loggerService.set(loggerProvider)
            services.forEach { (name, provider) ->
                task.services.put(name, provider)
                task.usesService(provider)
            }
            task.usesService(defaultService)
            task.usesService(loggerProvider)
        }
    }
}
