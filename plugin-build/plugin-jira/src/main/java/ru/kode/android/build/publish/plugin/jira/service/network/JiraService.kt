package ru.kode.android.build.publish.plugin.jira.service.network

import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.logger.LoggerService
import ru.kode.android.build.publish.plugin.jira.controller.JiraController
import ru.kode.android.build.publish.plugin.jira.controller.JiraControllerImpl
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraEndpoint
import ru.kode.android.build.publish.plugin.jira.controller.mappers.jiraEndpointFromJson
import ru.kode.android.build.publish.plugin.jira.messages.noMatchingJiraEndpointForProjectKeyMessage
import ru.kode.android.build.publish.plugin.jira.messages.noMatchingJiraEndpointMessage
import ru.kode.android.build.publish.plugin.jira.network.factory.JiraApiFactory
import ru.kode.android.build.publish.plugin.jira.network.factory.JiraClientFactory
import javax.inject.Inject

/**
 * A Gradle build service that provides network operations for interacting with Jira's REST API.
 *
 * This service handles credentials, request/response serialization, and error handling
 * for Jira API operations. It's designed to be used within Gradle build scripts to
 * automate Jira-related tasks as part of the build process.

 * @see BuildService
 * @see JiraApi
 */
abstract class JiraService
    @Inject
    constructor() : BuildService<JiraService.Params> {
        /**
         * Configuration parameters for the Jira network service.
         *
         * @see BasicAuthCredentials
         */
        interface Params : BuildServiceParameters {
            /**
             * A list of configured Jira endpoints that can be used for making requests.
             * Each endpoint is serialized as JSON.
             */
            val endpoints: ListProperty<String>

            /**
             * The logger service for logging messages during network operations.
             */
            val loggerService: Property<LoggerService>
        }

        private val endpoints: List<JiraEndpoint>
            get() =
                parameters.endpoints
                    .map { it.map { json -> jiraEndpointFromJson(json) } }
                    .get()

        private fun resolveEndpointByProjectKey(projectKey: String): JiraEndpoint {
            val normalizedProjectKey = projectKey.uppercase()
            return endpoints.firstOrNull { endpoint ->
                endpoint.projectKeys.map { it.uppercase() }.contains(normalizedProjectKey)
            } ?: throw GradleException(noMatchingJiraEndpointForProjectKeyMessage(projectKey))
        }

        private fun resolveEndpointByName(endpointName: String): JiraEndpoint {
            return endpoints.firstOrNull { it.name == endpointName }
                ?: throw GradleException(noMatchingJiraEndpointMessage(endpointName))
        }

        private fun resolveEndpointByIssueKey(issueKey: String): JiraEndpoint {
            val projectKey = issueKey.substringBefore("-").uppercase()
            return resolveEndpointByProjectKey(projectKey)
        }

        private fun buildController(endpoint: JiraEndpoint): JiraController {
            val logger = parameters.loggerService.get().logger
            val basicAuth =
                endpoint.basicAuth
                    ?: throw GradleException("Jira endpoint '${endpoint.name}' has no credentials")
            val client = JiraClientFactory.build(basicAuth.username, basicAuth.password, logger)
            val api = JiraApiFactory.build(client, endpoint.baseUrl)
            return JiraControllerImpl(api, logger)
        }

        private fun controllerForProjectKey(projectKey: String): JiraController {
            return buildController(resolveEndpointByProjectKey(projectKey))
        }

        private fun controllerForEndpointName(endpointName: String): JiraController {
            return buildController(resolveEndpointByName(endpointName))
        }

        private fun controllerForIssueKey(issueKey: String): JiraController {
            return buildController(resolveEndpointByIssueKey(issueKey))
        }

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
        ): String {
            return controllerForProjectKey(projectKey).getStatusTransitionId(projectKey, statusName, issues)
        }

        fun getStatusTransitionId(
            endpointName: String,
            projectKey: String,
            statusName: String,
            issues: List<String>,
        ): String {
            return controllerForEndpointName(endpointName).getStatusTransitionId(projectKey, statusName, issues)
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
            return controllerForProjectKey(projectKey).getProjectId(projectKey)
        }

        fun getProjectId(
            endpointName: String,
            projectKey: String,
        ): Long {
            return controllerForEndpointName(endpointName).getProjectId(projectKey)
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
            controllerForIssueKey(issueKey).setIssueStatus(issueKey, statusTransitionId)
        }

        fun setStatus(
            endpointName: String,
            issueKey: String,
            statusTransitionId: String,
        ) {
            controllerForEndpointName(endpointName).setIssueStatus(issueKey, statusTransitionId)
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
            controllerForIssueKey(issueKey).addIssueLabel(issueKey, label)
        }

        fun addLabel(
            endpointName: String,
            issueKey: String,
            label: String,
        ) {
            controllerForEndpointName(endpointName).addIssueLabel(issueKey, label)
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
            projectKey: String,
            version: String,
        ) {
            val projectId = getProjectId(projectKey)
            controllerForProjectKey(projectKey).createProjectVersion(projectId, version)
        }

        fun createVersion(
            endpointName: String,
            projectKey: String,
            version: String,
        ) {
            val projectId = getProjectId(endpointName, projectKey)
            controllerForEndpointName(endpointName).createProjectVersion(projectId, version)
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
            controllerForIssueKey(issueKey).addIssueFixVersion(issueKey, version)
        }

        fun addFixVersion(
            endpointName: String,
            issueKey: String,
            version: String,
        ) {
            controllerForEndpointName(endpointName).addIssueFixVersion(issueKey, version)
        }
    }
