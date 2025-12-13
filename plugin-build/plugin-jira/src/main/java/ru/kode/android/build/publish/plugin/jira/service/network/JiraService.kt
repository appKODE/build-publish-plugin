package ru.kode.android.build.publish.plugin.jira.service.network

import okhttp3.OkHttpClient
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import ru.kode.android.build.publish.plugin.core.api.config.BasicAuthCredentials
import ru.kode.android.build.publish.plugin.jira.controller.JiraController
import ru.kode.android.build.publish.plugin.jira.controller.JiraControllerImpl
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraIssueStatus
import ru.kode.android.build.publish.plugin.jira.controller.entity.JiraIssueTransition
import ru.kode.android.build.publish.plugin.jira.network.factory.JiraApiFactory
import ru.kode.android.build.publish.plugin.jira.network.factory.JiraClientFactory
import ru.kode.android.build.publish.plugin.jira.network.api.JiraApi
import javax.inject.Inject

/**
 * A Gradle build service that provides network operations for interacting with Jira's REST API.
 *
 * This service handles authentication, request/response serialization, and error handling
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
             * The base URL of the Jira instance (e.g., "https://your-domain.atlassian.net")
             */
            val baseUrl: Property<String>

            /**
             * The authentication credentials for the Jira API, containing username and password/token
             */
            val credentials: Property<BasicAuthCredentials>
        }

        private val logger: Logger = Logging.getLogger("Jira")

        internal abstract val okHttpClientProperty: Property<OkHttpClient>

        internal abstract val apiProperty: Property<JiraApi>

        internal abstract val controllerProperty: Property<JiraController>

        init {
            val username = parameters.credentials.flatMap { it.username }
            val password = parameters.credentials.flatMap { it.password }
            okHttpClientProperty.set(
                username
                    .zip(password) { username, password ->
                        JiraClientFactory.build(username, password, logger)
                    },
            )
            apiProperty.set(
                okHttpClientProperty
                    .zip(parameters.baseUrl) { client, baseUrl ->
                        JiraApiFactory.build(client, baseUrl)
                    },
            )
            controllerProperty.set(
                apiProperty.map { api -> JiraControllerImpl(api, logger) }
            )
        }

        private val controller: JiraController get() = controllerProperty.get()


        /**
         * Retrieves all statuses available in a Jira project across all workflows.
         *
         * Calls:
         * `GET /rest/api/2/project/{projectKey}/statuses`
         *
         * Example returned values:
         * - ID: "1", Name: "Open"
         * - ID: "3", Name: "In Progress"
         * - ID: "5", Name: "Resolved"
         */
        fun getProjectAvailableStatuses(projectKey: String): List<JiraIssueStatus> {
           return controller.getProjectAvailableStatuses(projectKey)
        }

        /**
         * Retrieves all available transitions for a Jira issue.
         *
         * This method calls:
         * `GET /rest/api/2/issue/{issue}/transitions`
         *
         * Jira returns a list of transitions, where each transition contains:
         * - transition ID
         * - transition name
         *
         * Example returned transitions:
         * - ID: "1", Name: "Start Progress", TargetStatus: "In Progress"
         * - ID: "2", Name: "Resolve", TargetStatus: "Resolved"
         *
         * If no transitions are available, returns an empty list.
         *
         * @param issue The Jira issue key (e.g., "PROJ-123").
         *
         * @return A list of `JiraIssueTransition` domain objects.
         *
         * @throws IOException If HTTP request fails.
         * @throws JiraApiException If Jira responds with error code or unexpected result.
         */
        fun getIssueTransitions(issueKey: String): List<JiraIssueTransition> {
            return controller.getAvailableIssueTransitions(issueKey)
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
    }
