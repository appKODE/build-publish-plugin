package ru.kode.android.build.publish.plugin.jira

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
import ru.kode.android.build.publish.plugin.jira.config.JiraAuthConfig
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraInstanceEntity
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraProjectEntity
import ru.kode.android.build.publish.plugin.jira.controller.mappers.toJson
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

        if (extension.auth.isEmpty()) {
            logger.info(noAuthConfigsMessage())
            return
        }

        logger.info(registeringServicesMessage())

        // One Jira service per `auth` entry (build variant, keyed by its name / "default" common),
        // mirroring the Telegram single-service model: every service bakes all of that variant's
        // instances (base URL + credentials + projects) as JSON, and consumers select an instance by
        // name. Baking happens here (inside finalizeDsl/afterEvaluate) so the plain-string parameters
        // stay configuration-cache safe.
        val serviceMap =
            extension.auth.associate { authConfig ->
                val name = authConfig.name
                val registered =
                    project.gradle.sharedServices.registerIfAbsent(
                        project.serviceName(SERVICE_NAME, name),
                        JiraService::class.java,
                    ) { spec ->
                        spec.maxParallelUsages.set(1)
                        spec.parameters.instances.set(authConfig.bakeInstances())
                        spec.parameters.loggerService.set(loggerProvider)
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

        registerStandaloneTasks(project, serviceMap.resolveStandaloneService(), loggerProvider)
    }

    private fun registerStandaloneTasks(
        project: Project,
        service: Provider<JiraService>,
        loggerProvider: Provider<LoggerService>,
    ) {
        project.registerStandaloneServiceTask<AddJiraFixVersionTask, JiraService>(
            TaskNames.Jira.ADD_FIX_VERSION,
            service,
            loggerProvider,
        )
        project.registerStandaloneServiceTask<AddJiraLabelTask, JiraService>(
            TaskNames.Jira.ADD_LABEL,
            service,
            loggerProvider,
        )
        project.registerStandaloneServiceTask<TransitionJiraIssueTask, JiraService>(
            TaskNames.Jira.TRANSITION_ISSUE,
            service,
            loggerProvider,
        )
    }
}

/** Bakes every instance declared on this auth config into the JSON list stored in the service params. */
private fun JiraAuthConfig.bakeInstances(): List<String> =
    instances.map { instance ->
        JiraInstanceEntity(
            name = instance.name,
            baseUrl = instance.baseUrl.get(),
            username = instance.credentials.username.get(),
            password = instance.credentials.password.get(),
            projects =
                instance.projects.mapNotNull { project ->
                    project.projectKey.orNull?.let { key -> JiraProjectEntity(project.name, key) }
                },
        ).toJson()
    }
