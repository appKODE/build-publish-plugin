package ru.kode.android.build.publish.plugin.jira.service.network

import ru.kode.android.build.publish.plugin.core.api.service.BasicAuthBuildService
import ru.kode.android.build.publish.plugin.core.logger.PluginLogger
import ru.kode.android.build.publish.plugin.jira.controller.JiraController
import ru.kode.android.build.publish.plugin.jira.controller.addFixVersionToIssues
import ru.kode.android.build.publish.plugin.jira.controller.addLabelToIssues
import ru.kode.android.build.publish.plugin.jira.controller.factory.JiraControllerFactory
import ru.kode.android.build.publish.plugin.jira.controller.transitionIssues
import javax.inject.Inject

/**
 * A Gradle build service that provides network operations for interacting with Jira's REST API.
 *
 * This service handles authentication, request/response serialization, and error handling
 * for Jira API operations. It's designed to be used within Gradle build scripts to
 * automate Jira-related tasks as part of the build process.

 * @see BasicAuthBuildService
 * @see JiraController
 */
abstract class JiraService
    @Inject
    constructor() : BasicAuthBuildService<JiraController>() {
        override fun buildController(
            baseUrl: String,
            username: String,
            password: String,
            logger: PluginLogger,
        ): JiraController =
            JiraControllerFactory.build(
                baseUrl = baseUrl,
                username = username,
                password = password,
                logger = logger,
            )

        /**
         * Retrieves the ID of the status with the given name in the specified project,
         * using the first issue in the list that has a transition to this status.
         *
         * @param projectKey The key of the Jira project
         * @param statusName The name of the status to search for
         * @param issues A list of issue keys to search for a transition to the given status
         * @return The ID of the status with the given name
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error
         */
        fun getStatusTransitionId(
            projectKey: String,
            statusName: String,
            issues: List<String>,
        ): String? {
            return controller.getStatusTransitionId(projectKey, statusName, issues)
        }

        /**
         * Retrieves the ID of a Jira project by its key.
         *
         * @param projectKey The key of the project (e.g., "PROJECT")
         *
         * @return The ID of the project
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error
         */
        fun getProjectId(projectKey: String): Long {
            return controller.getProjectId(projectKey)
        }

        /**
         * Transitions a Jira issue to a new status.
         *
         * @param issueKey The issue key (e.g., "PROJ-123")
         * @param statusTransitionId The ID of the status transition to execute
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error
         */
        fun setStatus(
            issueKey: String,
            statusTransitionId: String,
        ) {
            controller.setIssueStatus(issueKey, statusTransitionId)
        }

        /**
         * Adds a label to a Jira issue.
         *
         * @param issueKey The issue key (e.g., "PROJ-123")
         * @param label The label to add
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error
         */
        fun addLabel(
            issueKey: String,
            label: String,
        ) {
            controller.addIssueLabel(issueKey, label)
        }

        /**
         * Creates a new version in a Jira project.
         *
         * @param projectId The ID of the Jira project
         * @param version The version name to create
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error or version already exists
         */
        fun createVersion(
            projectId: Long,
            version: String,
        ) {
            controller.createProjectVersion(projectId, version)
        }

        /**
         * Adds a fix version to a Jira issue.
         *
         * @param issueKey The issue key (e.g., "PROJ-123")
         * @param version The version to add as a fix version
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error
         */
        fun addFixVersion(
            issueKey: String,
            version: String,
        ) {
            controller.addIssueFixVersion(issueKey, version)
        }

        /**
         * Adds a label to multiple Jira issues.
         *
         * @param label The label to add
         * @param issues A collection of issue keys to label (e.g., "PROJ-123")
         * @param log Callback invoked with human-readable progress messages
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error
         */
        fun addLabelToIssues(
            label: String,
            issues: Collection<String>,
            log: (String) -> Unit,
        ) = controller.addLabelToIssues(label, issues, log)

        /**
         * Adds a fix version to multiple Jira issues, creating the version if it does not exist.
         *
         * @param projectKey The key of the Jira project (e.g., "PROJECT")
         * @param version The version to add as a fix version
         * @param issues A collection of issue keys to update (e.g., "PROJ-123")
         * @param log Callback invoked with human-readable progress messages
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error
         */
        fun addFixVersionToIssues(
            projectKey: String,
            version: String,
            issues: Collection<String>,
            log: (String) -> Unit,
        ) = controller.addFixVersionToIssues(projectKey, version, issues, log)

        /**
         * Transitions multiple Jira issues to the status reachable via the named transition.
         *
         * @param projectKey The key of the Jira project (e.g., "PROJECT")
         * @param transitionName The name of the transition to execute
         * @param issues A collection of issue keys to transition (e.g., "PROJ-123")
         * @param log Callback invoked with human-readable progress messages
         *
         * @throws IOException If the network request fails
         * @throws JiraApiException If the Jira API returns an error
         */
        fun transitionIssues(
            projectKey: String,
            transitionName: String,
            issues: Collection<String>,
            log: (String) -> Unit,
        ) = controller.transitionIssues(projectKey, transitionName, issues, log)
    }
