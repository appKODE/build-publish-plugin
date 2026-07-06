package ru.kode.android.build.publish.plugin.jira.service.network

import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.core.util.COMMON_CONTAINER_NAME
import ru.kode.android.build.publish.plugin.jira.controller.JiraController
import ru.kode.android.build.publish.plugin.jira.controller.addFixVersionToIssues
import ru.kode.android.build.publish.plugin.jira.controller.addLabelToIssues
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraInstanceEntity
import ru.kode.android.build.publish.plugin.jira.controller.factory.JiraControllerFactory
import ru.kode.android.build.publish.plugin.jira.controller.mappers.jiraInstanceFromJson
import ru.kode.android.build.publish.plugin.jira.controller.transitionIssues
import ru.kode.android.build.publish.plugin.jira.messages.unknownInstanceNameMessage
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * A Gradle build service providing network operations for every configured Jira instance.
 *
 * Mirrors the Telegram single-service model: one service per build variant bakes **all** of that
 * variant's instances (base URL + credentials + projects) as JSON into its parameters, and each
 * operation selects the instance by name. A [JiraController] is built lazily per instance on first use
 * (Jira authentication is per base URL/credentials, so instances cannot share one controller).
 *
 * @see JiraController
 */
abstract class JiraService
    @Inject
    constructor() : BuildService<JiraService.Params> {
        interface Params : BuildServiceParameters {
            /** Each configured Jira instance, baked as [JiraInstanceEntity] JSON. */
            val instances: ListProperty<String>
            val loggerService: Property<LoggerService>
        }

        private val instances: List<JiraInstanceEntity> by lazy {
            parameters.instances.get().map { jiraInstanceFromJson(it) }
        }

        private val controllers = ConcurrentHashMap<String, JiraController>()

        /**
         * Resolves the controller for [requestedInstanceName]. A blank name (a standalone task invoked
         * without `--instanceName`) selects the instance named `default`, or the sole/first declared
         * instance otherwise.
         */
        private fun controller(requestedInstanceName: String): JiraController {
            val instanceName = requestedInstanceName.ifBlank { defaultInstanceName() }
            return controllers.getOrPut(instanceName) {
                val instance =
                    instances.firstOrNull { it.name == instanceName }
                        ?: throw GradleException(
                            unknownInstanceNameMessage(instanceName, instances.map { it.name }),
                        )
                JiraControllerFactory.build(
                    baseUrl = instance.baseUrl,
                    username = instance.username,
                    password = instance.password,
                    logger = parameters.loggerService.get().logger,
                )
            }
        }

        private fun defaultInstanceName(): String =
            (instances.firstOrNull { it.name == COMMON_CONTAINER_NAME } ?: instances.firstOrNull())?.name
                ?: throw GradleException(unknownInstanceNameMessage(COMMON_CONTAINER_NAME, emptyList()))

        /**
         * The project key of the registry project [projectName] on [instanceName], or `null` when it is
         * not declared.
         */
        fun projectKey(
            instanceName: String,
            projectName: String,
        ): String? {
            val instance = instances.firstOrNull { it.name == instanceName } ?: return null
            return instance.projects.firstOrNull { it.name == projectName }?.key
        }

        /**
         * Retrieves the ID of the status transition with the given name in the specified project, using
         * the first issue in the list that has a transition to it.
         */
        fun getStatusTransitionId(
            instanceName: String,
            projectKey: String,
            statusName: String,
            issues: List<String>,
        ): String? = controller(instanceName).getStatusTransitionId(projectKey, statusName, issues)

        /** Retrieves the ID of a Jira project by its key. */
        fun getProjectId(
            instanceName: String,
            projectKey: String,
        ): Long = controller(instanceName).getProjectId(projectKey)

        /** Retrieves the summary (title) of a Jira issue, or `null` when it cannot be retrieved. */
        fun getIssueSummary(
            instanceName: String,
            issueKey: String,
        ): String? = controller(instanceName).getIssueSummary(issueKey)

        /** Transitions a Jira issue to a new status. */
        fun setStatus(
            instanceName: String,
            issueKey: String,
            statusTransitionId: String,
        ) = controller(instanceName).setIssueStatus(issueKey, statusTransitionId)

        /** Adds a label to a Jira issue. */
        fun addLabel(
            instanceName: String,
            issueKey: String,
            label: String,
        ) = controller(instanceName).addIssueLabel(issueKey, label)

        /** Creates a new version in a Jira project. */
        fun createVersion(
            instanceName: String,
            projectId: Long,
            version: String,
        ) = controller(instanceName).createProjectVersion(projectId, version)

        /** Adds a fix version to a Jira issue. */
        fun addFixVersion(
            instanceName: String,
            issueKey: String,
            version: String,
        ) = controller(instanceName).addIssueFixVersion(issueKey, version)

        /** Adds a label to multiple Jira issues. */
        fun addLabelToIssues(
            instanceName: String,
            label: String,
            issues: Collection<String>,
            log: (String) -> Unit,
        ) = controller(instanceName).addLabelToIssues(label, issues, log)

        /** Adds a fix version to multiple Jira issues, creating the version if it does not exist. */
        fun addFixVersionToIssues(
            instanceName: String,
            projectKey: String,
            version: String,
            issues: Collection<String>,
            log: (String) -> Unit,
        ) = controller(instanceName).addFixVersionToIssues(projectKey, version, issues, log)

        /** Transitions multiple Jira issues to the status reachable via the named transition. */
        fun transitionIssues(
            instanceName: String,
            projectKey: String,
            transitionName: String,
            issues: Collection<String>,
            log: (String) -> Unit,
        ) = controller(instanceName).transitionIssues(projectKey, transitionName, issues, log)
    }
